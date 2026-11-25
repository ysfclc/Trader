#!/usr/bin/env python3
"""Validate and summarize the AGP JaCoCo XML report without third-party packages."""
from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def percentage(covered: int, missed: int) -> str:
    total = covered + missed
    return f"{covered / total * 100:.2f}%" if total else "n/a"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", required=True, type=Path)
    parser.add_argument("--summary-file", type=Path)
    args = parser.parse_args()

    if not args.report.is_file():
        raise SystemExit(f"Coverage XML report is missing: {args.report}")

    root = ET.parse(args.report).getroot()
    counters = {
        node.attrib.get("type"): node.attrib
        for node in root.findall("counter")
    }
    line = counters.get("LINE")
    if line is None:
        raise SystemExit("Coverage XML has no root LINE counter")
    line_covered = int(line["covered"])
    line_missed = int(line["missed"])
    if line_covered + line_missed <= 0:
        raise SystemExit("Coverage XML contains zero executable lines; refusing an empty baseline")

    labels = {
        "LINE": "Line",
        "BRANCH": "Branch",
        "INSTRUCTION": "Instruction",
        "METHOD": "Method",
        "CLASS": "Class",
    }
    rows = []
    for kind, label in labels.items():
        item = counters.get(kind)
        if item is None:
            rows.append(f"| {label} | unavailable |")
        else:
            covered, missed = int(item["covered"]), int(item["missed"])
            rows.append(f"| {label} | {percentage(covered, missed)} ({covered}/{covered + missed}) |")

    output = [
        "## Trader debug unit-test coverage",
        "",
        "| Metric | Coverage |",
        "|---|---:|",
        *rows,
        "",
        f"Report: `{args.report.as_posix()}`",
    ]
    message = "\n".join(output)
    print(message)
    if args.summary_file:
        args.summary_file.parent.mkdir(parents=True, exist_ok=True)
        with args.summary_file.open("a", encoding="utf-8") as stream:
            stream.write(message + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
