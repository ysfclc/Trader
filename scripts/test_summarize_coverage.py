import tempfile
import unittest
from pathlib import Path

from summarize_coverage import enforce_baseline, read_coverage


class CoverageSummaryTests(unittest.TestCase):
    def _report(self, xml: str) -> Path:
        temporary = tempfile.NamedTemporaryFile(mode="w", suffix=".xml", encoding="utf-8", delete=False)
        with temporary:
            temporary.write(xml)
        self.addCleanup(Path(temporary.name).unlink, missing_ok=True)
        return Path(temporary.name)

    def _baseline(self):
        return {
            "comparison_policy": "per-class-covered-count-non-regression",
            "metrics": {
                "line": {"covered": 11, "missed": 9},
                "branch": {"covered": 6, "missed": 4},
            },
            "classes": {
                "example/Repository": {"line": 8, "branch": 4},
                "example/Rules": {"line": 3, "branch": 2},
            },
        }

    def _coverage(self, classes, line_missed=9, branch_missed=4):
        return {
            "application_source": {
                "LINE": {"covered": sum(row.get("line", 0) for row in classes.values()), "missed": line_missed},
                "BRANCH": {"covered": sum(row.get("branch", 0) for row in classes.values()), "missed": branch_missed},
            },
            "classes": classes,
        }

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
        self.assertEqual(result["classes"]["example/Repository"], {"line": 8, "branch": 4})
        self.assertNotIn("example/Repository_Impl", result["classes"])

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
        coverage = self._coverage({
            "example/Repository": {"line": 8, "branch": 4},
            "example/Rules": {"line": 3, "branch": 2},
        })
        self.assertEqual(enforce_baseline(coverage, self._baseline()), [])

    def test_new_classes_and_expanded_denominators_do_not_regress_existing_floor(self):
        coverage = self._coverage({
            "example/Repository": {"line": 8, "branch": 4},
            "example/Rules": {"line": 3, "branch": 2},
            "example/NewComposeScreen": {"line": 0, "branch": 0},
        }, line_missed=200, branch_missed=100)
        self.assertEqual(enforce_baseline(coverage, self._baseline()), [])

    def test_line_and_branch_covered_count_regressions_fail_independently(self):
        line_drop = self._coverage({
            "example/Repository": {"line": 7, "branch": 4},
            "example/Rules": {"line": 3, "branch": 2},
        })
        self.assertEqual(len(enforce_baseline(line_drop, self._baseline())), 1)

        branch_drop = self._coverage({
            "example/Repository": {"line": 8, "branch": 4},
            "example/Rules": {"line": 3, "branch": 1},
        })
        self.assertEqual(len(enforce_baseline(branch_drop, self._baseline())), 1)

    def test_missing_previously_covered_class_fails_closed(self):
        coverage = self._coverage({"example/Repository": {"line": 8, "branch": 4}})
        failures = enforce_baseline(coverage, self._baseline())
        self.assertTrue(any("previously covered class disappeared" in failure for failure in failures))

    def test_baseline_class_map_must_match_aggregate_covered_counts(self):
        baseline = self._baseline()
        baseline["classes"]["example/Rules"]["line"] = 2
        with self.assertRaisesRegex(ValueError, "class counters do not match"):
            enforce_baseline(self._coverage({
                "example/Repository": {"line": 8, "branch": 4},
                "example/Rules": {"line": 3, "branch": 2},
            }), baseline)


if __name__ == "__main__":
    unittest.main()
