"""When the launchers start a session versus wait on CI (scripts/loop-lib.sh).

Run: python3 -m unittest scripts.test_loop_lib
"""
from pathlib import Path
import subprocess
import tempfile
import unittest

LIB = Path(__file__).with_name("loop-lib.sh")

# A gh stub: `pr list` prints the open PR numbers from $OPEN, `pr checks <n>` prints the bucket from
# $CHECKS_<n> (empty = not registered yet). Every --watch is recorded, so a test can tell a wait.
GH = r'''#!/usr/bin/env bash
if [ "$1 $2" = "pr list" ]; then
  for n in $OPEN; do echo "$n"; done; exit 0
fi
if [ "$1 $2" = "pr checks" ]; then
  [[ " $* " == *" --watch "* ]] && { echo "$3" >> "$ROOT/watched"; exit 0; }
  var="CHECKS_$3"; [ -n "${!var}" ] && echo "${!var}"; exit 0
fi
'''


class AwaitTurnTest(unittest.TestCase):
    def run_turn(self, open_prs, checks):
        with tempfile.TemporaryDirectory() as root:
            bin_dir = Path(root) / "bin"
            bin_dir.mkdir()
            (bin_dir / "gh").write_text(GH)
            (bin_dir / "gh").chmod(0o755)
            (bin_dir / "sleep").write_text("#!/usr/bin/env bash\n")
            (bin_dir / "sleep").chmod(0o755)
            env = {"PATH": f"{bin_dir}:/usr/bin:/bin", "ROOT": root, "OPEN": " ".join(open_prs)}
            env.update({f"CHECKS_{n}": v for n, v in checks.items()})
            script = (f'log() {{ echo "LOG $*"; }}; source "{LIB}"; '
                      f'loop_await_turn loop-ecl-; echo "PRS=$LOOP_PRS"')
            out = subprocess.run(["bash", "-c", script], env=env, capture_output=True, text=True,
                                 timeout=10, check=True).stdout
            watched = Path(root, "watched").read_text().split() if Path(root, "watched").exists() else []
            return out, watched

    def test_no_open_pr_starts_at_once(self):
        out, watched = self.run_turn([], {})
        self.assertIn("PRS=\n", out)
        self.assertEqual(watched, [])

    def test_pending_pr_waits_instead_of_building_the_next_unit(self):
        _, watched = self.run_turn(["7"], {"7": "pending"})
        self.assertEqual(watched, ["7"])

    def test_waits_on_the_oldest(self):
        out, watched = self.run_turn(["9", "7"], {"7": "pending", "9": "pending"})
        self.assertIn("PRS=7 9", out)
        self.assertEqual(watched, ["7"])

    def test_a_finished_pr_needs_a_session(self):
        _, watched = self.run_turn(["7", "9"], {"7": "pass", "9": "pending"})
        self.assertEqual(watched, [])

    def test_unregistered_checks_count_as_pending(self):
        _, watched = self.run_turn(["7"], {})
        self.assertEqual(watched, ["7"])


# A session that writes $EVENTS to stdout, leaves a background task running (its pid in $ROOT/kid),
# and won't exit until that task does ($KID_SECONDS later) — how `claude -p` behaves with a watcher that never finishes.
SESSION = r'''#!/usr/bin/env bash
printf '%s\n' "$EVENTS"
sleep "$KID_SECONDS" &
echo $! > "$ROOT/kid"
wait
'''

ANSWERED = '{"type":"assistant"}\n{"type":"result","result":"Summary: merged #7.\\n\\nSTEP 6 DONE"}'


class RunReapedTest(unittest.TestCase):
    def run_session(self, events, kid_seconds=30):
        with tempfile.TemporaryDirectory() as root:
            session = Path(root) / "session"
            session.write_text(SESSION)
            session.chmod(0o755)
            transcript = Path(root) / "iter1.jsonl"
            env = {"PATH": "/usr/bin:/bin:/opt/homebrew/bin:/usr/local/bin", "ROOT": root, "EVENTS": events,
                   "KID_SECONDS": str(kid_seconds),
                   "LOOP_LINGER_GRACE": "2", "LOOP_REAP_TICK": "1"}
            script = (f'log() {{ echo "LOG $*"; }}; source "{LIB}"; '
                      f'loop_run_reaped "{transcript}" SET_COMPLETE "{session}" | tee "{transcript}" >/dev/null; '
                      f'loop_note_reaped "{transcript}"')
            out = subprocess.run(["bash", "-c", script], env=env, capture_output=True, text=True,
                                 timeout=25, check=True).stdout
            kid = Path(root, "kid").read_text().strip()
            kid_alive = subprocess.run(["kill", "-0", kid], capture_output=True).returncode == 0
            if kid_alive:
                subprocess.run(["kill", kid])
            return out, kid_alive, transcript.with_name("iter1.jsonl.reaped").exists()

    def test_an_answered_session_left_lingering_is_stopped_with_its_background_tasks(self):
        out, kid_alive, marker = self.run_session(ANSWERED)
        self.assertIn("LOG session sat idle 2s after answering", out)
        self.assertFalse(kid_alive)
        self.assertFalse(marker)

    # Sessions that sit silent past the grace but must be left to finish on their own.
    def assert_left_running(self, events):
        out, _, marker = self.run_session(events, kid_seconds=5)
        self.assertNotIn("sat idle", out)
        self.assertFalse(marker)

    def test_a_session_still_mid_turn_is_never_taken_for_idle(self):
        # A long silent build: the last event is a tool call, not a result.
        self.assert_left_running(ANSWERED + '\n{"type":"assistant","message":{"content":[{"type":"tool_use"}]}}')

    def test_a_result_without_a_protocol_line_is_not_an_answer(self):
        self.assert_left_running('{"type":"result","result":"Waiting for the gate to finish"}')


if __name__ == "__main__":
    unittest.main()
