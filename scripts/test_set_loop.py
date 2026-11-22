"""Launcher integration checks without model sessions, GitHub calls, or real sleeps.

Run: python3 -m unittest scripts.test_set_loop
"""
import os
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class SetLoopTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        (self.root / ".git").mkdir()
        shutil.copy(Path(__file__).with_name("set-loop"), self.bin / "set-loop")
        shutil.copy(Path(__file__).with_name("loop-lib.sh"), self.bin / "loop-lib.sh")
        self.stub("resolve-set", 'echo ecl')
        self.stub("git", 'echo "$TEST_ROOT/.git"')
        self.stub("gh", "exit 0")
        # The session reaper ticks every LOOP_REAP_TICK seconds; those aren't the loop's waits.
        self.stub("sleep", '[ "$1" = "$LOOP_REAP_TICK" ] && exit 0; '
                           'echo "$1" >> "$TEST_ROOT/sleeps"; touch "$TEST_ROOT/.claude/loop-runs/ecl.stop"')
        self.stub("codex", """
printf '%s\\n' "$@" > "$TEST_ROOT/args"
while [ "$#" -gt 0 ]; do
  if [ "$1" = --output-last-message ]; then shift; output=$1; fi
  shift
done
if [ "$MODE" = limit ]; then
  echo '{"type":"error","message":"usage limit reached"}'
  exit 1
fi
if [ "$MODE" = failure ]; then echo 'Invalid model'; exit 1; fi
printf '%s\\n' "$VERDICT" > "$output"
echo '{"type":"item.completed","item":{"type":"agent_message","text":"Summary: checked"}}'
echo '{"type":"turn.completed","usage":{}}'
""")
        self.stub("claude", """
printf '%s\\n' "$@" > "$TEST_ROOT/args"
echo '{"type":"result","result":"SET_COMPLETE","num_turns":1,"duration_ms":1000}'
""")

    def stub(self, name, body):
        path = self.bin / name
        path.write_text("#!/usr/bin/env bash\n" + body + "\n")
        path.chmod(0o755)

    def run_loop(self, *args, model="", mode="", verdict="SET_COMPLETE", set_code="ecl"):
        env = dict(os.environ, PATH=f"{self.bin}:{os.environ['PATH']}",
                   TEST_ROOT=str(self.root), HOME=str(self.root), LOOP_REAP_TICK="1", MODEL=model, MODE=mode, VERDICT=verdict)
        return subprocess.run(["bash", str(self.bin / "set-loop"), set_code, *args],
                              cwd=self.root, env=env, capture_output=True, text=True, timeout=10)

    def test_help_and_models_do_not_resolve_sets_or_launch_sessions(self):
        self.stub("resolve-set", "exit 99")
        for option in ("", "--help", "-h", "--models"):
            with self.subTest(option=option):
                result = self.run_loop(set_code=option)
                self.assertEqual(result.returncode, 0, result.stderr)
                for model in ("sonnet", "opus", "astra"):
                    self.assertIn(model, result.stdout)
                self.assertFalse((self.root / "args").exists())
                self.assertFalse((self.root / ".claude").exists())

    def test_claude_aliases_and_explicit_id(self):
        for model in ("sonnet", "opus", "claude-opus-5-5"):
            with self.subTest(model=model):
                result = self.run_loop(model)
                self.assertEqual(result.returncode, 0, result.stderr)
                args = (self.root / "args").read_text().splitlines()
                self.assertEqual(args[0], "-p")
                self.assertEqual(args[args.index("--model") + 1], model)
                if model in ("sonnet", "opus"):
                    self.assertIn("resolve the actual driving", "\n".join(args))

    def test_prompt_reviews_before_the_pr_and_gates_narrowly(self):
        result = self.run_loop("claude-opus-5-5")
        self.assertEqual(result.returncode, 0, result.stderr)
        prompt = "\n".join((self.root / "args").read_text().splitlines())
        self.assertIn("PR LIFECYCLE", prompt)
        self.assertIn("just loop-gate", prompt)
        self.assertIn("loop-reviewed <full sha", prompt)
        self.assertNotIn("$LOOP_PR_PROTOCOL", prompt)
        self.assertIn("NO E2E", prompt)
        self.assertIn("just ui-dev", prompt)
        self.assertNotIn("e2e-loop", prompt)

    def test_claude_session_runs_under_a_goal_without_mcp_servers(self):
        result = self.run_loop("claude-opus-5-5")
        self.assertEqual(result.returncode, 0, result.stderr)
        args = (self.root / "args").read_text().splitlines()
        self.assertTrue(args[1].startswith("/goal "), args[1])
        self.assertIn("STEP <n> DONE", args[1])
        self.assertLessEqual(len(args[1]), 4000)
        self.assertIn("--strict-mcp-config", args)
        self.assertIn("PR LIFECYCLE", "\n".join(args[args.index("--append-system-prompt") + 1:]))

    def test_goal_closing_empty_result_does_not_hide_the_answer(self):
        self.stub("claude", """
echo '{"type":"result","result":"Launched the implementers; waiting."}'
echo '{"type":"result","result":"SET_COMPLETE"}'
echo '{"type":"result","result":""}'
""")
        result = self.run_loop("claude-opus-5-5")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("set ecl complete", result.stdout)
        self.assertFalse((self.root / "sleeps").exists())

    def test_picker_without_terminal_does_not_launch(self):
        result = self.run_loop("pick")
        self.assertEqual(result.returncode, 2)
        self.assertIn("pick needs a terminal", result.stderr)
        self.assertFalse((self.root / "args").exists())

    def test_astra_alias_and_explicit_id(self):
        for model in ("astra", "gpt-6-astra"):
            with self.subTest(model=model):
                result = self.run_loop(model)
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                args = (self.root / "args").read_text().splitlines()
                self.assertEqual(args[:3], ["exec", "--model", "gpt-6-astra"])
                self.assertIn("--approve-for-me", args)
                self.assertIn("[agent-loop: gpt-6-astra]", "\n".join(args))
                self.assertIn("Summary: checked", result.stdout)
                self.assertRegex(result.stdout, r"\[\d{4}-\d\d-\d\d \d\d:\d\d:\d\d\]    📋 Summary: checked")
                self.assertIn("set ecl complete", result.stdout)

    def test_environment_model_and_positional_override(self):
        result = self.run_loop("", model="astra")
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("harness codex", result.stdout)
        result = self.run_loop("claude-opus-5-5", model="astra")
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("harness claude", result.stdout)

    def test_arbitrary_codex_model_ids(self):
        for model in ("gpt-6-sol", "o3", "custom-model", "Custom-Model"):
            with self.subTest(model=model):
                result = self.run_loop(f"codex:{model}")
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                args = (self.root / "args").read_text().splitlines()
                self.assertEqual(args[:3], ["exec", "--model", model])
                self.assertIn(f"[agent-loop: {model}]", "\n".join(args))

    def test_gpt_display_name_uses_lowercase_codex_id(self):
        for model in ("codex:GPT-6.1-Sol", "GPT-6.1-Sol", "codex:gPt-6.1-sOl"):
            with self.subTest(model=model):
                result = self.run_loop(model, set_code="lea")
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                args = (self.root / "args").read_text().splitlines()
                self.assertEqual(args[:3], ["exec", "--model", "gpt-6.1-sol"])
                self.assertIn("[agent-loop: gpt-6.1-sol]", "\n".join(args))
                self.assertIn("normalized to", result.stdout)

        result = self.run_loop(model="codex:GPT-6.1-Sol")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("harness codex, model gpt-6.1-sol", result.stdout)

    def test_codex_model_from_environment(self):
        result = self.run_loop(model="codex:custom-model")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("harness codex, model custom-model", result.stdout)

    def test_empty_codex_model_does_not_launch(self):
        result = self.run_loop("codex:")
        self.assertEqual(result.returncode, 2)
        self.assertIn("requires a model ID", result.stderr)
        self.assertFalse((self.root / "args").exists())

    def test_default_still_uses_claude(self):
        result = self.run_loop()
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("harness claude, model claude-opus-5-5", result.stdout)

    def test_claude_milestones_are_timestamped(self):
        self.stub("claude", """
echo '{"type":"assistant","message":{"content":[{"type":"text","text":"Working on: proof\\nSummary: checked"}]}}'
echo '{"type":"result","result":"SET_COMPLETE"}'
""")
        result = self.run_loop("sonnet")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        for icon, text in (("🔨", "Working on: proof"), ("📋", "Summary: checked")):
            self.assertRegex(result.stdout, rf"\[\d{{4}}-\d\d-\d\d \d\d:\d\d:\d\d\]    {icon} {text}")

    def test_priority_deck_is_carried_into_fresh_sessions(self):
        priority = self.root / "deck list.txt"
        priority.write_text("Caesar, Legion's Emperor\nWear // Tear\n")
        result = self.run_loop("codex:gpt-6.1-sol", str(priority))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        prompt = (self.root / "args").read_text()
        self.assertIn(priority.read_text(), prompt)
        self.assertIn("overrides the global phase ordering", prompt)
        self.assertIn("Filter the sweep's authored cards AND row-only/basic rows", prompt)
        self.assertIn("continue the ordinary whole-set phases", prompt)

    def test_invalid_priority_file_does_not_launch(self):
        empty = self.root / "empty.txt"
        empty.touch()
        for path in (self.root / "missing.txt", empty, self.root):
            result = self.run_loop("astra", str(path))
            self.assertEqual(result.returncode, 2, result.stderr)
            self.assertIn("readable, nonempty file", result.stderr)
            self.assertFalse((self.root / "args").exists())

    def test_stuck_and_launch_failure_stop(self):
        for options in ({"verdict": "STUCK missing capability"}, {"mode": "failure"}):
            with self.subTest(options=options):
                result = self.run_loop("astra", **options)
                self.assertEqual(result.returncode, 1, result.stdout)
                self.assertFalse((self.root / "sleeps").exists())

    def test_stale_stop_file_from_a_dead_run_is_cleared(self):
        runs = self.root / ".claude" / "loop-runs"
        runs.mkdir(parents=True)
        (runs / "ecl.stop").touch()
        result = self.run_loop("claude-opus-5-5")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("removed a stale ecl.stop", result.stdout)
        self.assertIn("set ecl complete", result.stdout)
        self.assertNotIn("stop file found", result.stdout)

    def test_usage_limit_waits_then_honors_stop_file(self):
        result = self.run_loop("astra", mode="limit")
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertEqual((self.root / "sleeps").read_text(), "900\n")
        self.assertIn("stop file found", result.stdout)

    def test_claude_session_limit_waits_then_honors_stop_file(self):
        message = "You've hit your session limit · resets 1:20am (Europe/Amsterdam)"
        event = json.dumps({"type": "result", "result": message,
                            "num_turns": 1, "duration_ms": 3000})
        self.stub("claude", "cat <<'EOF'\n" + event + "\nEOF")
        result = self.run_loop("sonnet")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual((self.root / "sleeps").read_text(), "900\n")
        self.assertIn("limit reached — waiting 15 min", result.stdout)
        self.assertNotIn("iteration 2", result.stdout)
        self.assertIn("stop file found", result.stdout)


if __name__ == "__main__":
    unittest.main()
