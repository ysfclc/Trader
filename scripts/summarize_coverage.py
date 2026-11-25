#!/usr/bin/env python3
"""Summarize AGP JaCoCo coverage and enforce the checked-in source-coverage floor."""
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


def read_coverage(report: Path) -> dict[str, dict[str, dict[str, int]]]:
    if not report.is_file():
        raise ValueError(f"Coverage XML report is missing: {report}")

    root = ET.parse(report).getroot()
    all_bytecode = _read_counters(root)
    application_source = {kind: {"covered": 0, "missed": 0} for kind in METRICS}

    for package in root.findall("package"):
        for class_node in package.findall("class"):
            # Room's KSP-generated DAO/database implementations are not app-authored source.
            if "_Impl" in class_node.attrib.get("name", ""):
                continue
            for kind, values in _read_counters(class_node).items():
                application_source[kind]["covered"] += values["covered"]
                application_source[kind]["missed"] += values["missed"]

    line = application_source["LINE"]
    if line["covered"] + line["missed"] <= 0:
        raise ValueError("Coverage XML has zero application-source lines after generated-code exclusion")
    branch = application_source["BRANCH"]
    if branch["covered"] + branch["missed"] <= 0:
        raise ValueError("Coverage XML has zero application-source branches")
    return {"all_bytecode": all_bytecode, "application_source": application_source}


def is_below_baseline(current: dict[str, int], baseline: dict[str, int]) -> bool:
    current_total = current["covered"] + current["missed"]
    baseline_total = baseline["covered"] + baseline["missed"]
    if current_total <= 0 or baseline_total <= 0:
        raise ValueError("Coverage baseline and current counters must both be non-empty")
    # Compare exact ratios using integer arithmetic; avoid rounded-percentage loopholes.
    return current["covered"] * baseline_total < baseline["covered"] * current_total


def enforce_baseline(
    current: dict[str, dict[str, int]], baseline: dict[str, Any]
) -> list[str]:
    failures = []
    for kind, label in (("LINE", "line"), ("BRANCH", "branch")):
        expected = baseline["metrics"][label]
        actual = current[kind]
        if is_below_baseline(actual, expected):
            current_total = actual["covered"] + actual["missed"]
            baseline_total = expected["covered"] + expected["missed"]
            current_pct = actual["covered"] / current_total * 100
            baseline_pct = expected["covered"] / baseline_total * 100
            failures.append(
                f"{label} coverage regressed: {current_pct:.4f}% < baseline {baseline_pct:.4f}%"
            )
    return failures


def _format(values: dict[str, int]) -> str:
    total = values["covered"] + values["missed"]
    return f"{values['covered'] / total * 100:.2f}% ({values['covered']}/{total})" if total else "n/a"


def render_summary(coverage: dict[str, dict[str, dict[str, int]]], baseline_status: str) -> str:
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
    rows.extend(["", f"Baseline check: **{baseline_status}**"])
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
        failures = enforce_baseline(coverage["application_source"], baseline)
    except (OSError, ValueError, KeyError, TypeError, ET.ParseError) as error:
        print(f"Coverage verification failed: {error}", file=sys.stderr)
        return 1

    status = "FAIL" if failures else "PASS"
    message = render_summary(coverage, status)
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
