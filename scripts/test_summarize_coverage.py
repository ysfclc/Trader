import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

from summarize_coverage import enforce_baseline, is_below_baseline, read_coverage


class CoverageSummaryTests(unittest.TestCase):
    def _report(self, xml: str) -> Path:
        temporary = tempfile.NamedTemporaryFile(mode="w", suffix=".xml", encoding="utf-8", delete=False)
        with temporary:
            temporary.write(xml)
        self.addCleanup(Path(temporary.name).unlink, missing_ok=True)
        return Path(temporary.name)

    def test_aggregates_source_classes_and_excludes_room_generated_impl(self):
        report = self._report(
            '<report name="debug">'
            '<counter type="LINE" covered="14" missed="86"/>'
            '<counter type="BRANCH" covered="7" missed="93"/>'
            '<package name="example">'
            '<class name="example/Repository"><counter type="LINE" covered="8" missed="2"/>'
            '<counter type="BRANCH" covered="4" missed="1"/></class>'
            '<class name="example/Repository_Impl"><counter type="LINE" covered="0" missed="80"/>'
            '<counter type="BRANCH" covered="0" missed="90"/></class>'
            '<class name="example/Rules"><counter type="LINE" covered="3" missed="7"/>'
            '<counter type="BRANCH" covered="2" missed="3"/></class>'
            '</package></report>'
        )
        result = read_coverage(report)
        self.assertEqual(result["all_bytecode"]["LINE"], {"covered": 14, "missed": 86})
        self.assertEqual(result["application_source"]["LINE"], {"covered": 11, "missed": 9})
        self.assertEqual(result["application_source"]["BRANCH"], {"covered": 6, "missed": 4})

    def test_rejects_empty_application_source_line_counter(self):
        report = self._report(
            '<report><counter type="LINE" covered="0" missed="0"/>'
            '<package name="example"><class name="example/Dao_Impl">'
            '<counter type="LINE" covered="0" missed="50"/></class></package></report>'
        )
        with self.assertRaisesRegex(ValueError, "zero application-source lines"):
            read_coverage(report)

    def test_rejects_empty_application_source_branch_counter(self):
        report = self._report(
            '<report><counter type="LINE" covered="2" missed="1"/>'
            '<package name="example"><class name="example/Rules">'
            '<counter type="LINE" covered="2" missed="1"/>'
            '<counter type="BRANCH" covered="0" missed="0"/></class></package></report>'
        )
        with self.assertRaisesRegex(ValueError, "zero application-source branches"):
            read_coverage(report)

    def test_exact_baseline_passes(self):
        current = {"LINE": {"covered": 14, "missed": 86}, "BRANCH": {"covered": 7, "missed": 93}}
        baseline = {"metrics": {"line": current["LINE"], "branch": current["BRANCH"]}}
        self.assertEqual(enforce_baseline(current, baseline), [])

    def test_line_and_branch_regressions_fail_independently(self):
        baseline = {
            "metrics": {
                "line": {"covered": 14, "missed": 86},
                "branch": {"covered": 7, "missed": 93},
            }
        }
        line_drop = {"LINE": {"covered": 13, "missed": 87}, "BRANCH": {"covered": 7, "missed": 93}}
        self.assertEqual(len(enforce_baseline(line_drop, baseline)), 1)
        branch_drop = {"LINE": {"covered": 14, "missed": 86}, "BRANCH": {"covered": 6, "missed": 94}}
        self.assertEqual(len(enforce_baseline(branch_drop, baseline)), 1)

    def test_integer_ratio_comparison_does_not_use_rounded_percentages(self):
        self.assertTrue(is_below_baseline({"covered": 1, "missed": 10000}, {"covered": 1, "missed": 9999}))
        self.assertFalse(is_below_baseline({"covered": 14, "missed": 86}, {"covered": 14, "missed": 86}))


if __name__ == "__main__":
    unittest.main()
