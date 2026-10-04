---
name: google-play-compliance
description: Review Android changes for Google Play policy requirements covering permissions, user data, SDKs, disclosures, foreground services, exact alarms, ads, and release declarations. Use for release preparation or changes to manifest, data handling, SDKs, timer/background work, privacy, or store claims.
---

# Google Play Compliance

1. Scope the change using AGENTS.md and the affected manifest, Gradle configuration, feature, and SDK. Review the merged manifest when available, including library-added permissions.
2. Open the applicable official sources in [policy-review.md](references/policy-review.md). Record review date, source URLs, current deadlines, and applicability. Do not hardcode target API levels, SDK versions, or deadlines into policy rules. If sources are unavailable, mark policy currency unverified.
3. Review least-privilege access: request permissions in context, explain sensitive access when required, support denial/revocation, and prefer less privileged APIs. Never add permissions solely to silence a failure.
4. Trace actual collection/sharing by the app and third-party SDKs. Check purposes, retention, security, prominent disclosure/consent when required, privacy policy, and Data safety consistency. Do not claim "no data collected" merely because app code stores little data.
5. For timer changes, verify USE_EXACT_ALARM eligibility, SCHEDULE_EXACT_ALARM behavior, foreground-service type/subtype and Play declaration requirements, notification permission denial, and user-visible stop behavior. Do not assume a timer embedded in a calculator qualifies for restricted permissions.
6. Check applicable target API and native-library requirements, SDK policy status, exported components, secure transport, ads declaration, target audience/Families rules, and accurate store claims. If accounts exist, review required account-deletion paths; mark this check not applicable otherwise.
7. Inspect ad changes using the admob-compliance skill. Preserve accessible, localized privacy/disclosure controls.
8. Return findings with severity, file/symbol, policy source/date, concrete remediation, and verification evidence. Separate code findings from Play Console/account settings that cannot be verified in the repository. Do not certify approval or submit/change Console declarations without an explicit task.
