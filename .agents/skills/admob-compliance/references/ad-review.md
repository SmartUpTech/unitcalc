# Official sources

Open documentation for the SDK family installed in app/build.gradle; verify current methods and dependencies before changing code.

- Next-Gen privacy/UMP: https://developers.google.com/admob/android/next-gen/privacy
- Next-Gen integration: https://developers.google.com/admob/android/next-gen/quick-start
- Next-Gen app-open lifecycle: https://developers.google.com/admob/android/next-gen/app-open
- Next-Gen test ads: https://developers.google.com/admob/android/next-gen/test-ads
- Legacy privacy/UMP: https://developers.google.com/admob/android/privacy
- Legacy test ads: https://developers.google.com/admob/android/test-ads
- AdMob policies: https://support.google.com/admob/answer/6128543
- Disallowed interstitials: https://support.google.com/admob/answer/6201362
- Recommended interstitials: https://support.google.com/admob/answer/6201350
- Google publisher policies: https://support.google.com/publisherpolicies/answer/10502938

## Verification scenarios

- First launch, returning consent, privacy options, rejected choices, expired consent, form/update errors and offline startup.
- Test UMP regions only on configured test devices; keep production builds free of forced geography/reset.
- Rapid tab taps, repeated resume, rotation/recreation where supported, ad dismissal/failure, no fill, delayed load, and destroyed Activity.
- Assert one eligible request path and at most one full-screen ad; do not block navigation on ad failure.
- Inspect placements with keyboard open, system insets, large text, supported languages, and small screens.
- Exercise timer expiry/background/widget actions; verify none can initiate ads outside foreground app content.
- For mediated units, confirm each adapter's test and privacy requirements; Google test setup alone does not prove all networks serve test ads.

## Account checks

Confirm messages are configured for the matching AdMob application ID, geographic/audience settings are accurate, test configuration is correct, and applicable mediation/privacy signals are supported. Mark unseen account configuration unverified. Cross-check SDK data behavior with the google-play-compliance skill.
