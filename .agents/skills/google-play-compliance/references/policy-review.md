# Review checklist and official sources

Recheck the live policy before each release or affected change; this checklist is a routing aid, not a substitute for the policy.

- Overall policy and accurate functionality/listings: https://play.google.com/about/developer-content-policy/
- User data, privacy policy, disclosures and SDK responsibility: https://support.google.com/googleplay/android-developer/answer/10144311
- Data safety definitions and declaration scope: https://support.google.com/googleplay/android-developer/answer/10787469
- Sensitive permissions, including exact alarms: https://support.google.com/googleplay/android-developer/answer/16558241
- Target API requirements and applicable dates/exceptions: https://support.google.com/googleplay/android-developer/answer/11926878
- Foreground services and declaration requirements: https://support.google.com/googleplay/android-developer/answer/13392821
- Technical exact alarm behavior: https://developer.android.com/develop/background-work/services/alarms/schedule
- Technical foreground service types: https://developer.android.com/develop/background-work/services/fgs/service-types
- Native library/page-size requirements if dependencies contain native code: https://developer.android.com/guide/practices/page-sizes

## Repository evidence

Inspect app/build.gradle, app/src/main/AndroidManifest.xml, merged manifests, SDK dependencies, network configuration, backup rules, privacy entry points, data stores, and the affected timer/ad implementation. Load only what the change requires.

For each permission or service, explain the actual user-facing need and current eligibility; include denial/revocation and background restrictions in verification.

## Outside repository evidence

Mark privacy policy publication, Play Console Data safety, ads/target-audience declarations, foreground-service declarations, restricted-permission approvals, and SDK Console notices as unverified unless inspected. Describe the required owner action rather than guessing or changing these settings.

Report statuses as verified / issue found / not applicable / unverified. Source freshness and technical tests do not imply Google Play approval.
