#!/usr/bin/env python3
"""Summarize AGP JaCoCo coverage and enforce the historical per-class coverage floor."""
from __future__ import annotations

import argparse
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any

METRICS = ("LINE", "BRANCH", "INSTRUCTION", "METHOD", "CLASS")


def _read_counters(node: ET.Element) -> dict[str, dict[str, int]]:
    result: dict[str, dict[str, int]] = {}
    for counter in node.findall("counter"):
        kind = counter.attrib.get("type")
        if kind in METRICS:
            result[kind] = {
                "covered": int(counter.attrib["covered"]),
                "missed": int(counter.attrib["missed"]),
            }
    return result


def read_coverage(report: Path) -> dict[str, Any]:
    if not report.is_file():
        raise ValueError(f"Coverage XML report is missing: {report}")

    root = ET.parse(report).getroot()
    all_bytecode = _read_counters(root)
    application_source = {kind: {"covered": 0, "missed": 0} for kind in METRICS}
    classes: dict[str, dict[str, int]] = {}

    for package in root.findall("package"):
        for class_node in package.findall("class"):
            name = class_node.attrib.get("name", "")
            # Room's KSP-generated DAO/database implementations are not app-authored source.
            if "_Impl" in name:
                continue
            counters = _read_counters(class_node)
            classes[name] = {
                "line": counters.get("LINE", {}).get("covered", 0),
                "branch": counters.get("BRANCH", {}).get("covered", 0),
            }
            for kind, values in counters.items():
                application_source[kind]["covered"] += values["covered"]
                application_source[kind]["missed"] += values["missed"]

    line = application_source["LINE"]
    if line["covered"] + line["missed"] <= 0:
        raise ValueError("Coverage XML has zero application-source lines after generated-code exclusion")
    branch = application_source["BRANCH"]
    if branch["covered"] + branch["missed"] <= 0:
        raise ValueError("Coverage XML has zero application-source branches")
    return {
        "all_bytecode": all_bytecode,
        "application_source": application_source,
        "classes": classes,
    }


def enforce_baseline(coverage: dict[str, Any], baseline: dict[str, Any]) -> list[str]:
    if baseline.get("comparison_policy") != "per-class-covered-count-non-regression":
        raise ValueError("Unsupported coverage baseline comparison policy")
    baseline_classes = baseline.get("classes")
    if not isinstance(baseline_classes, dict) or not baseline_classes:
        raise ValueError("Coverage baseline has no per-class counters")

    # Guard the baseline file itself: the class map must account for its aggregate
    # historical covered counts, otherwise a truncated baseline could weaken the gate.
    for metric in ("line", "branch"):
        expected_total = baseline.get("metrics", {}).get(metric, {}).get("covered")
        mapped_total = sum(values.get(metric, 0) for values in baseline_classes.values())
        if expected_total is None or mapped_total != expected_total:
            raise ValueError(f"Coverage baseline {metric} class counters do not match its aggregate")

    current_classes = coverage.get("classes", {})
    failures: list[str] = []
    for class_name, expected in baseline_classes.items():
        actual = current_classes.get(class_name)
        if actual is None:
            failures.append(f"previously covered class disappeared from report: {class_name}")
            continue
        for metric in ("line", "branch"):
            if metric not in expected:
                continue
            expected_count = int(expected[metric])
            actual_count = int(actual.get(metric, 0))
            if actual_count < expected_count:
                failures.append(
                    f"{class_name} {metric} coverage regressed: "
                    f"{actual_count} covered < historical {expected_count}"
                )
    return failures


def _format(values: dict[str, int]) -> str:
    total = values["covered"] + values["missed"]
    return f"{values['covered'] / total * 100:.2f}% ({values['covered']}/{total})" if total else "n/a"


def render_summary(
    coverage: dict[str, Any], baseline_status: str, baseline_class_count: int
) -> str:
    rows = [
        "## Trader debug unit-test coverage",
        "",
        "| Scope | Metric | Coverage |",
        "|---|---|---:|",
    ]
    labels = {
        "LINE": "Line",
        "BRANCH": "Branch",
        "INSTRUCTION": "Instruction",
        "METHOD": "Method",
        "CLASS": "Class",
    }
    for scope, title in (
        ("all_bytecode", "All bytecode (includes generated code)"),
        ("application_source", "Application source (excludes Room *_Impl)"),
    ):
        for kind, label in labels.items():
            values = coverage[scope].get(kind)
            if values is not None:
                rows.append(f"| {title} | {label} | {_format(values)} |")
    rows.extend([
        "",
        f"Historical per-class covered-count floor ({baseline_class_count} classes): **{baseline_status}**",
        "New classes do not enlarge the historical floor; their behavior still requires focused feature tests.",
    ])
    return "\n".join(rows)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", required=True, type=Path)
    parser.add_argument("--baseline", required=True, type=Path)
    parser.add_argument("--summary-file", type=Path)
    args = parser.parse_args()

    try:
        coverage = read_coverage(args.report)
        baseline = json.loads(args.baseline.read_text(encoding="utf-8"))
        failures = enforce_baseline(coverage, baseline)
    except (OSError, ValueError, KeyError, TypeError, ET.ParseError) as error:
        print(f"Coverage verification failed: {error}", file=sys.stderr)
        return 1

    status = "FAIL" if failures else "PASS"
    message = render_summary(coverage, status, len(baseline["classes"]))
    print(message)
    if args.summary_file:
        args.summary_file.parent.mkdir(parents=True, exist_ok=True)
        with args.summary_file.open("a", encoding="utf-8") as stream:
            stream.write(message + "\n")
    for failure in failures:
        print(f"::error::{failure}")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
