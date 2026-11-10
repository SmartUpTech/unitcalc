"""Regression checks for aliases and full-app offline translation coverage."""
import unittest
import xml.etree.ElementTree as ET
from audit_localization import audit, placeholders, resolve, text


class LocalizationAuditTest(unittest.TestCase):
    def test_full_app_has_no_gaps_or_invalid_formats(self):
        report = audit()
        self.assertEqual([], report["errors"])
        for locale, details in report["locales"].items():
            self.assertEqual([], details["missing"], locale)

    def test_alias_keeps_arguments_and_percent_escape(self):
        alias = ET.fromstring('<string name="alias">@string/message</string>')
        target = ET.fromstring('<string name="message">%1$s · %2$s%% · %3$d mois</string>')
        actual = resolve(alias, {("string", "message"): target})
        self.assertEqual(["%1$s", "%2$s", "%3$d"], placeholders(text(actual)))

    def test_invalid_aliases_fail_instead_of_silently_using_english(self):
        alias = ET.fromstring('<string name="alias">@string/missing</string>')
        with self.assertRaisesRegex(ValueError, "Missing"):
            resolve(alias, {})
        cycle = ET.fromstring('<string name="loop">@string/loop</string>')
        with self.assertRaisesRegex(ValueError, "Cyclic"):
            resolve(cycle, {("string", "loop"): cycle})


if __name__ == "__main__":
    unittest.main()
