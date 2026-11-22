#!/usr/bin/env bash
#
# kill-stale-daemons.sh — reap Kotlin compile daemons and Gradle daemons that are idle, orphaned,
# or so starved of heap that every build routed through them crawls.
#
# Why: `org.gradle.daemon.idletimeout` only governs the *Gradle* daemon. The Kotlin compile
# daemon is a separate JVM with its own (6g) heap and its own lifetime, shared by every worktree
# whose JVM args match. Three ways it goes wrong on a box running many worktree agents:
#
#   idle     hours-old, 0%-CPU JVMs each holding gigabytes push the machine into swap and make
#            the *next* compile OOM. Detected by sampling cumulative CPU time twice.
#   sick     a long-lived shared daemon whose old generation has filled up. It never quite OOMs:
#            it grinds through a full GC every few seconds, both build slots sit behind it, and
#            every queued build looks "stuck". Detected with `jstat`.
#   orphan   a Gradle daemon whose worktree directory has been deleted; nothing can reuse it.
#
# SAFETY:
#   * idle reaping refuses to run while any gradle-locked slot is held (other than --ignore-lock) —
#     a build is in flight and its daemon may sit at 0% CPU while tests run in a forked JVM;
#   * sick and orphan reaping ignore the slots: a sick daemon *is* what the in-flight build is
#     stuck on, so failing that build fast beats letting it grind for hours;
#   * never touches `kotlin-lsp` / language-server JVMs (those are the user's editor, not ours);
#   * DRY RUN unless --apply is passed.
#
# Usage:
#   scripts/kill-stale-daemons.sh                        # dry run, 60-minute idle threshold
#   scripts/kill-stale-daemons.sh --min-age 30 --apply
#   scripts/kill-stale-daemons.sh --sick-only --apply    # what gradle-locked runs while queued
#   scripts/kill-stale-daemons.sh --ignore-lock <slot>   # treat the caller's own slot as free
#   --quiet prints only what was killed.

set -euo pipefail

MIN_AGE_MIN=60
SAMPLE_SECONDS=3
GC_WINDOW_SECONDS=10
APPLY=0
QUIET=0
SICK_ONLY=0
IGNORE_LOCK=""
LOCK_FILE="${GRADLE_LOCK_FILE:-$HOME/.cache/argentum/gradle.lock}"

while [ $# -gt 0 ]; do
    case "$1" in
        --min-age) MIN_AGE_MIN="$2"; shift 2 ;;
        --apply) APPLY=1; shift ;;
        --dry-run) APPLY=0; shift ;;
        --quiet) QUIET=1; shift ;;
        --sick-only) SICK_ONLY=1; shift ;;
        --ignore-lock) IGNORE_LOCK="$2"; shift 2 ;;
        -h|--help) sed -n '2,30p' "$0"; exit 0 ;;
        *) echo "kill-stale-daemons: unknown argument '$1'" >&2; exit 2 ;;
    esac
done

if ! [[ "$MIN_AGE_MIN" =~ ^[0-9]+$ ]]; then
    echo "kill-stale-daemons: --min-age must be an integer (got '$MIN_AGE_MIN')." >&2
    exit 2
fi
MIN_AGE_SEC=$((MIN_AGE_MIN * 60))

say() { [ "$QUIET" -eq 1 ] || echo "$@"; }

reclaimed_kb=0
reap() { # pid rss label age reason
    reclaimed_kb=$((reclaimed_kb + $2))
    local rss_h
    rss_h=$(awk -v k="$2" 'BEGIN{printf "%.1f GB", k/1024/1024}')
    if [ "$APPLY" -eq 1 ]; then
        echo "kill  $3 pid=$1 age=$4 rss=$rss_h  ($5)"
        kill "$1" 2>/dev/null || true
    else
        echo "would $3 pid=$1 age=$4 rss=$rss_h  ($5)"
    fi
}

# pid<TAB>elapsed-seconds<TAB>rss-kb<TAB>cpu-seconds<TAB>label<TAB>java-binary
#
# macOS `ps` has no `etimes`, only the formatted `etime` ([[dd-]hh:]mm:ss), and `time` is
# mm:ss.ss or hh:mm:ss — so both are parsed by hand rather than read as integers.
snapshot() {
    ps -Ao pid=,etime=,rss=,time=,command= | awk -v OFS='\t' '
        function secs(s,   days, parts, n, i, out) {
            days = 0
            if (index(s, "-") > 0) { days = substr(s, 1, index(s, "-") - 1) + 0
                                     s = substr(s, index(s, "-") + 1) }
            n = split(s, parts, ":")
            out = 0
            for (i = 1; i <= n; i++) out = out * 60 + (parts[i] + 0)
            return int(out + days * 86400)
        }
        /kotlin-lsp|language-server/ { next }
        /KotlinCompileDaemon/        { label = "kotlin-daemon" }
        /GradleDaemon/               { label = "gradle-daemon" }
        label != "" {
            print $1, secs($2), $3, secs($4), label, $5
            label = ""
        }'
}

age_of() { awk -v s="$1" 'BEGIN{printf "%dh%02dm", s/3600, (s%3600)/60}'; }

before="$(snapshot)"
if [ -z "$before" ]; then
    say "kill-stale-daemons: no Gradle or Kotlin daemons running."
    exit 0
fi

reaped=" "

# --- sick: Kotlin daemons grinding on a full old generation --------------------------------------
# One `jstat` window per Kotlin daemon. Sick when, over the window, the JVM spent at least half its
# wall time in GC, or ran two-plus full GCs with the old generation still ≥90% full. Also sick when
# chronically starved: 100+ full GCs over its life, ≥5% of its uptime spent in GC, old gen ≥85%
# (a healthy compile daemon runs a handful of full GCs an hour and spends well under 1% in GC).
while IFS=$'\t' read -r pid etimes rss _cpu label java; do
    [ "$label" = "kotlin-daemon" ] || continue
    jstat="$(dirname "$java")/jstat"
    [ -x "$jstat" ] || jstat="$(command -v jstat || true)"
    [ -n "$jstat" ] || { say "skip  kotlin-daemon pid=$pid  (no jstat to measure GC)"; continue; }
    # Columns: S0 S1 E O M CCS YGC YGCT FGC FGCT CGC CGCT GCT — the decimal separator follows the
    # locale, so normalise commas.
    lines="$("$jstat" -gcutil "$pid" "$((GC_WINDOW_SECONDS * 1000))" 2 2>/dev/null | tail -n 2 | tr ',' '.')" || continue
    [ "$(printf '%s\n' "$lines" | wc -l | tr -d ' ')" -eq 2 ] || continue
    verdict="$(printf '%s\n' "$lines" | awk -v w="$GC_WINDOW_SECONDS" -v up="$etimes" '
        NR == 1 { fgc0 = $9; gct0 = $NF }
        NR == 2 {
            old = $4; dfgc = $9 - fgc0; dgct = $NF - gct0
            if (dgct / w >= 0.5)
                printf "sick: %.0f%% of the last %ss in GC, old gen %s%%", 100 * dgct / w, w, old
            else if (old >= 90 && dfgc >= 2)
                printf "sick: %d full GCs in %ss, old gen still %s%%", dfgc, w, old
            else if ($9 >= 100 && up > 0 && $NF / up >= 0.05 && old >= 85)
                printf "sick: %d full GCs, %.0f%% of uptime in GC, old gen %s%%", $9, 100 * $NF / up, old
        }')"
    if [ -n "$verdict" ]; then
        reap "$pid" "$rss" "$label" "$(age_of "$etimes")" "$verdict"
        reaped="$reaped$pid "
    else
        say "keep  kotlin-daemon pid=$pid age=$(age_of "$etimes")  (GC healthy)"
    fi
done <<< "$before"

if [ "$SICK_ONLY" -eq 1 ]; then
    [ "$reclaimed_kb" -eq 0 ] || [ "$QUIET" -eq 1 ] || echo
    exit 0
fi

# --- orphan: Gradle daemons whose worktree is gone ----------------------------------------------
while IFS=$'\t' read -r pid etimes rss _cpu label _java; do
    [ "$label" = "gradle-daemon" ] || continue
    case "$reaped" in *" $pid "*) continue ;; esac
    cwd="$(lsof -a -p "$pid" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p' | tail -1 || true)"
    if [ -n "$cwd" ] && [ ! -d "$cwd" ]; then
        reap "$pid" "$rss" "$label" "$(age_of "$etimes")" "orphan: $cwd no longer exists"
        reaped="$reaped$pid "
    fi
done <<< "$before"

# --- idle: everything else, only while no build is in flight ------------------------------------
for slot in "$LOCK_FILE" "$LOCK_FILE".[0-9]; do
    [ -e "$slot" ] || continue
    if [ "$slot" = "$IGNORE_LOCK" ]; then continue; fi
    say "kill-stale-daemons: '$slot' is held — a Gradle build is running. Not reaping idle daemons."
    exit 0
done

# Only sample when something is old enough to be a candidate; the sleep is otherwise wasted.
if ! printf '%s\n' "$before" | awk -F'\t' -v m="$MIN_AGE_SEC" '$2 >= m {found=1} END{exit !found}'; then
    say "kill-stale-daemons: nothing older than ${MIN_AGE_MIN}m."
    exit 0
fi

sleep "$SAMPLE_SECONDS"
after="$(snapshot)"

while IFS=$'\t' read -r pid etimes rss cpu label _java; do
    [ -n "${pid:-}" ] || continue
    case "$reaped" in *" $pid "*) continue ;; esac
    cpu_before=$(printf '%s\n' "$before" | awk -F'\t' -v p="$pid" '$1 == p {print $4}')
    [ -n "$cpu_before" ] || continue

    if [ "$etimes" -lt "$MIN_AGE_SEC" ]; then
        say "keep  $label pid=$pid age=$(age_of "$etimes")  (younger than ${MIN_AGE_MIN}m)"
        continue
    fi
    if [ "$cpu" -ne "$cpu_before" ]; then
        say "keep  $label pid=$pid age=$(age_of "$etimes")  (burned CPU during the ${SAMPLE_SECONDS}s sample)"
        continue
    fi
    reap "$pid" "$rss" "$label" "$(age_of "$etimes")" "idle"
done <<< "$after"

if [ "$QUIET" -eq 0 ]; then
    echo
    awk -v k="$reclaimed_kb" -v a="$APPLY" 'BEGIN{
        printf "%s: %.1f GB resident\n", (a ? "reclaimed" : "reclaimable (re-run with --apply)"), k/1024/1024
    }'
fi
