"""Offline Android resource coverage/placeholder audit; never manufactures translations.

Run: python scripts/audit_localization.py --output .artifacts/localization/resources.json
Regional resources inherit their language base. English variants inherit US fallback.
Identical translations are reported for human review, not automatically rejected.
"""
import argparse
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET

RES = Path(__file__).resolve().parents[1] / "app/src/main/res"
TEXT = {"string", "plurals", "string-array"}


def resources(directory):
    result = {}
    problems = []
    for file in directory.glob("*.xml"):
        for element in ET.parse(file).getroot():
            if element.tag not in TEXT:
                continue
            key = (element.tag, element.get("name"))
            if key in result:
                problems.append(f"Duplicate {key} in {file}")
            result[key] = element
            if directory.name != "values" and file.name != "strings.xml":
                problems.append(f"Translated text outside strings.xml: {file}:{key}")
    return result, problems


def placeholders(text):
    return sorted(re.findall(r"%(?!%)(?:\d+\$)?[-+0 #]*\d*(?:\.\d+)?[sdfoxegbctSDFOXEGBCThH]", text))


def text(element):
    return "".join(element.itertext())


def audit():
    base, errors = resources(RES / "values")
    required = {key: value for key, value in base.items() if value.get("translatable") != "false"}
    tags = [e.get("{http://schemas.android.com/apk/res/android}name")
            for e in ET.parse(RES / "xml/locales_config.xml").getroot()]
    report = {"errors": errors, "base_translatable_resources": len(required), "locales": {}}
    for tag in tags:
        language, _, region = tag.partition("-")
        qualifier = "in" if language == "id" else language
        translated = dict(base) if language == "en" else {}
        for directory in [RES / ("values-" + qualifier), RES / ("values-" + qualifier + "-r" + region)]:
            entries, problems = resources(directory)
            errors.extend(problems)
            translated.update(entries)
        missing = [key[1] for key in required if key not in translated]
        identical = []
        for key, target in translated.items():
            if key not in base:
                errors.append(f"{tag}: unknown {key}")
                continue
            source = base[key]
            if source.get("translatable") == "false" and target is not source:
                errors.append(f"{tag}: translated machine resource {key}")
            if key[0] == "string" and placeholders(text(source)) != placeholders(text(target)):
                errors.append(f"{tag}: placeholder mismatch {key}")
            if key[0] == "string-array" and len(source) != len(target):
                errors.append(f"{tag}: array length mismatch {key}")
            if key[0] == "plurals":
                quantities = {item.get("quantity") for item in target}
                if "other" not in quantities:
                    errors.append(f"{tag}: missing other plural {key}")
                signature = placeholders(text(source.find("item[@quantity='other']")))
                if any(placeholders(text(item)) != signature for item in target):
                    errors.append(f"{tag}: plural placeholder mismatch {key}")
            if language != "en" and key in required and text(source) == text(target):
                identical.append(key[1])
        report["locales"][tag] = {"missing": sorted(missing), "identical_for_review": sorted(identical)}
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    report = audit()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Locales: {len(report['locales'])}; errors: {len(report['errors'])}")
    for tag, detail in report["locales"].items():
        print(f"{tag}: {len(detail['missing'])} missing; {len(detail['identical_for_review'])} identical for review")
    raise SystemExit(1 if report["errors"] or any(v["missing"] for v in report["locales"].values()) else 0)
