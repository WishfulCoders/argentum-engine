"""Plan checks for scripts/loop-gate. Run: python3 -m unittest scripts.test_loop_gate"""
import importlib.machinery
import importlib.util
from pathlib import Path
import unittest

_loader = importlib.machinery.SourceFileLoader("loop_gate", str(Path(__file__).with_name("loop-gate")))
_spec = importlib.util.spec_from_loader("loop_gate", _loader)
loop_gate = importlib.util.module_from_spec(_spec)
_loader.exec_module(loop_gate)

CARD = "mtg-sets/2017-2022/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/dmu/cards/Foo.kt"
CARD_TEST = "mtg-sets/2017-2022/tests/src/test/kotlin/com/wingedsheep/engine/scenarios/FooScenarioTest.kt"
GOLDEN = "mtg-sets/src/test/resources/snapshots/cards/DMU.json"
ENGINE = "rules-engine/src/main/kotlin/com/wingedsheep/engine/X.kt"


class LoopGatePlanTest(unittest.TestCase):
    def test_card_batch_runs_content_suite_and_only_its_own_scenarios(self):
        args, web = loop_gate.plan([CARD, CARD_TEST, GOLDEN, "backlog/sets/dmu/cards.md"])
        self.assertIn(":mtg-sets:test", args)
        i = args.index(":mtg-sets:2017-2022:tests:test")
        self.assertEqual(args[i + 1:i + 3], ["--tests", "FooScenarioTest"])
        self.assertNotIn("testClasses", args)
        self.assertNotIn(":rules-engine:test", args)
        self.assertNotIn(":game-server:test", args)
        self.assertFalse(web)

    def test_engine_change_runs_engine_suite_touched_eras_and_compiles_everything(self):
        args, _ = loop_gate.plan([ENGINE, CARD])
        self.assertIn("testClasses", args)
        self.assertIn(":rules-engine:test", args)
        self.assertIn(":mtg-sets:test", args)
        i = args.index(":mtg-sets:2017-2022:tests:test")
        self.assertNotEqual(args[i + 1:i + 2], ["--tests"])  # the whole era, not one class
        self.assertNotIn(":mtg-sets:2024:tests:test", args)

    def test_rebless_rides_on_the_content_suite_only(self):
        args, _ = loop_gate.plan([CARD, GOLDEN])
        self.assertEqual(loop_gate.with_rebless(args)[-1], "-DupdateSnapshots=true")
        self.assertEqual(loop_gate.with_rebless([]), [])

    def test_docs_only_change_runs_nothing(self):
        self.assertEqual(loop_gate.plan(["docs/card-sdk-language-reference.md"]), ([], False))

    def test_web_client_change_typechecks(self):
        self.assertEqual(loop_gate.plan(["web-client/src/App.tsx"]), ([], True))

    def test_server_change_runs_server_suite(self):
        args, _ = loop_gate.plan(["game-server/src/main/kotlin/X.kt"])
        self.assertIn(":game-server:test", args)
        self.assertNotIn(":mtg-sets:test", args)


if __name__ == "__main__":
    unittest.main()
