"""Pure helpers of scripts/loop-tui. Run: python3 -m unittest scripts.test_loop_tui"""
import importlib.machinery
import importlib.util
from pathlib import Path
import unittest
from datetime import datetime
from types import SimpleNamespace
import tempfile

_loader = importlib.machinery.SourceFileLoader("loop_tui", str(Path(__file__).with_name("loop-tui")))
_spec = importlib.util.spec_from_loader("loop_tui", _loader)
loop_tui = importlib.util.module_from_spec(_spec)
_loader.exec_module(loop_tui)


class LoopTuiTest(unittest.TestCase):
    def test_stamped_stream_lines_preserve_session_state_and_old_milestones(self):
        with tempfile.TemporaryDirectory() as root:
            log = Path(root) / "pip.log"
            log.write_text(
                "[2026-10-12 23:58:00] set-loop pip started (harness codex, model test)\n"
                "[2026-10-12 23:59:00] iteration 1 (PR: none)\n"
                "   🔨 Working on: legacy message\n"
                "[2026-10-13 00:00:01]    🔎 Finding: stamped message\n"
                "[2026-10-13 00:00:02]    → command\n"
                "[2026-10-13 00:00:03]    💬 other message\n"
                "[2026-10-13 00:00:04]    ! error\n"
                "[2026-10-13 00:01:00]    📋 Summary: done\n"
                "[2026-10-13 00:02:00] iteration 1 finished: STEP 1 DONE (3 min)\n"
            )
            events, milestones = loop_tui.status.parse(log)
        self.assertEqual(len(events), 3)
        self.assertEqual(milestones, [(1, "🔨", "Working on: legacy message"),
                                      (1, "🔎", "Finding: stamped message"),
                                      (1, "📋", "Summary: done")])

    def test_history_shows_completion_date_seconds_and_duration_across_midnight(self):
        ui = SimpleNamespace(GREEN=1, YELLOW=2, RED=3)
        events = [(datetime(2026, 10, 12, 23, 59), "iteration 1 (PR: none)"),
                  (datetime(2026, 10, 13, 0, 2, 5), "iteration 1 finished: STEP 1 DONE (3 min)")]
        row = loop_tui.Screen.history(ui, events)[0]
        self.assertTrue(row[0].startswith("2026-10-13 00:02:05  STEP 1 DONE"))
        self.assertEqual(row[2], loop_tui.human(185))

    def test_ci_state(self):
        self.assertEqual(loop_tui.ci_state([]), "waiting for checks")
        self.assertEqual(loop_tui.ci_state([{"conclusion": "SUCCESS"}, {"conclusion": "SKIPPED"}]), "green")
        self.assertEqual(loop_tui.ci_state([{"conclusion": "SUCCESS"}, {"conclusion": ""}]), "running 1/2")
        self.assertEqual(loop_tui.ci_state([{"conclusion": "FAILURE"}, {"conclusion": ""}]), "failed")

    def test_wrap_and_clip_respect_display_width(self):
        self.assertEqual(loop_tui.wrap_text("one two three four", 9), ["one two", "three", "four"])
        self.assertEqual(loop_tui.clip("abcdef", 4), "abc…")
        self.assertEqual(loop_tui.width("📋a"), 3)

    def test_window_labels(self):
        self.assertEqual(loop_tui.window_label(300), "5h")
        self.assertEqual(loop_tui.window_label(10080), "7d")
        self.assertEqual(loop_tui.window_label(45), "45m")

    def test_last_json_reads_the_newest_matching_line(self):
        import tempfile
        with tempfile.NamedTemporaryFile("w", suffix=".jsonl", delete=False) as f:
            f.write('{"type":"rate_limit_event","n":1}\n' + '{"type":"x"}\n' * 20000
                    + '{"type":"rate_limit_event","n":2}\n{"type":"y"}\n')
        self.assertEqual(loop_tui.last_json(Path(f.name), '"rate_limit_event"')["n"], 2)
        self.assertIsNone(loop_tui.last_json(Path(f.name), '"nope"'))
        Path(f.name).unlink()

    def test_activity_follows_claude_subagents_and_their_own_actions(self):
        import json
        events = [
            {"type": "assistant", "timestamp": "2026-10-14T06:30:00Z", "parent_tool_use_id": None,
             "message": {"content": [{"type": "text", "text": "Working on: step 6\nmore"},
                                     {"type": "tool_use", "id": "t1", "name": "Agent",
                                      "input": {"description": "Implement G38", "prompt": "Build it"}}]}},
            {"type": "system", "subtype": "task_started", "tool_use_id": "t1", "task_type": "local_agent",
             "description": "Implement G38"},
            {"type": "assistant", "timestamp": "2026-10-14T06:31:00Z", "parent_tool_use_id": "t1",
             "message": {"content": [{"type": "tool_use", "id": "t2", "name": "Bash",
                                      "input": {"command": "cd /repo/.claude/worktrees/x && just loop-gate"}}]}},
            {"type": "system", "subtype": "task_progress", "tool_use_id": "t1",
             "usage": {"total_tokens": 5000, "tool_uses": 7}},
        ]
        with tempfile.TemporaryDirectory() as root:
            path = Path(root) / "20261014-083000-iter1.jsonl"
            path.write_text("".join(json.dumps(e) + "\n" for e in events) + '{"type": "assistant", "timest')
            activity = loop_tui.Activity()
            activity.follow(path)
            agent = activity.agents["t1"]
            self.assertTrue(agent.running)
            self.assertEqual((agent.label, agent.prompt, agent.tools, agent.tokens), ("Implement G38", "Build it", 7, 5000))
            self.assertEqual(agent.feed[-1][1], "→ Bash: just loop-gate")
            self.assertEqual(activity.main.feed[0][1], "💬 Working on: step 6")
            # The half-written last line is finished by a later append, not dropped.
            with open(path, "a") as f:
                f.write('amp": "2026-10-14T06:40:00Z", "parent_tool_use_id": null, "message": {"content": []}}\n')
                f.write(json.dumps({"type": "user", "parent_tool_use_id": None, "message": {"content": [
                    {"type": "tool_result", "tool_use_id": "t1", "is_error": False}]}}) + "\n")
            activity.follow(path)
        self.assertEqual(agent.status, "done")
        self.assertFalse(agent.running)
        self.assertEqual(activity.main.feed[-1][1], "🤖 Implement G38 done after 10 min, 7 tool calls")

    def test_codex_calls_read_as_their_command_or_patched_file(self):
        exec_call = 'text(await tools.exec_command({cmd:"cd /w && git status --short\\nls",workdir:"/w"}));'
        self.assertEqual(loop_tui.codex_call_line("exec", exec_call), "$ git status --short")
        patch = 'tools.apply_patch("*** Begin Patch\\n*** Add File: /w/review/final.md\\n+x\\n*** Update File: /w/a.kt")'
        self.assertEqual(loop_tui.codex_call_line("exec", patch), "✎ add final.md (+1 more)")
        self.assertEqual(loop_tui.codex_call_line("shell", '{"command": ["bash", "-lc", "ls"]}'), "$ bash -lc ls")
        self.assertEqual(loop_tui.shell_line("/bin/zsh -lc 'gh pr view 3'"), "$ gh pr view 3")

    def test_session_start_comes_from_the_transcript_name(self):
        start = loop_tui.session_start(Path("20261014-071535-iter6.jsonl"))
        self.assertEqual(datetime.fromtimestamp(start), datetime(2026, 10, 14, 7, 15, 35))

    def test_watching_a_loop_without_a_log_fails_cleanly(self):
        self.assertEqual(loop_tui.main(["--name", "no-such-loop-zz"]), 1)


if __name__ == "__main__":
    unittest.main()
