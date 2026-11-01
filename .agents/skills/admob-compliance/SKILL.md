---
name: admob-compliance
description: Review and implement Android AdMob privacy, consent, test ads, placement, invalid-traffic prevention, and lifecycle safety. Use for ad manager changes, banners, interstitials, app-open/rewarded/native ads, UMP, mediation, or monetization release checks.
---

# AdMob Compliance

1. Read AGENTS.md and the affected ad managers, initialization, manifest, and Gradle dependency. Detect legacy versus GMA Next-Gen APIs; use matching official documentation. Reuse existing managers rather than creating competing initialization/show paths.
2. Verify current applicable sources and account prerequisites in [ad-review.md](references/ad-review.md). Record source/date; mark AdMob account settings or mediation configuration unverified when unavailable.
3. Follow UMP's current launch update/form flow. Request ads only when canRequestAds() permits it; prevent duplicate initialization/requests across callbacks. On consent errors, consult UMP eligibility instead of bypassing consent or using a homegrown persisted flag. Expose the required privacy-options entry point and recheck eligibility after choices change.
4. For Next-Gen, verify application ID configuration wherever required by its initialization and UMP integration; do not copy legacy classes/imports into this SDK. Check child-directed/under-age settings and mediation consent signals against the actual audience and current requirements; never invent age classification.
5. Keep banners visually separate from keypad, conversion, navigation and timer controls; avoid overlap, moving targets, and disguised ads. Keep native attribution/assets visible. Do not encourage or simulate clicks.
6. Show interstitials only at genuine content breaks. Do not show them on app entry/exit, every action, during typing/conversion/countdown, or unexpectedly after delayed loading. If not ready at the break, continue content without a late surprise.
7. For app-open ads, respect foreground lifecycle, loading-screen timing, documented expiry, and full-screen exclusion; prevent redisplay when returning from an ad. Never show ads from background timer services, widgets, or alarm notifications.
8. For rewarded formats, follow the format's current opt-in/disclosure rules and grant rewards exactly once from the earned-reward callback. Never reward ad clicks.
9. Use test ad units/devices during development, including mediation-specific test setup. Never click production ads. Keep forced consent geography/reset and other test overrides debug-only.
10. Validate no-fill/error/offline handling, destroyed Activity, consent transitions, overlapping callbacks, and foreground return. Preserve core app use without ads. Report evidence and remaining account/device checks; do not promise compliance certification.
