# Usage analytics and review — 2026-10-27

## Firebase setup

Register Android package `net.smartlogic.unitconverter` in the owner's Analytics-enabled Firebase project and put its `google-services.json` in `app/` (gitignored). The Google Services plugin is applied only when this registration exists. Without it, the app works and analytics is inactive. No fabricated Firebase identity is included.

Collection defaults off in the manifest. Settings > Share usage analytics is an explicit opt-in with explanatory text; switching off disables collection, denies Analytics storage and resets local analytics data. Ad storage, ad user data and personalization stay denied for this Analytics integration. Analytics advertising-ID collection and automatic screen reporting are disabled. This consent is separate from AdMob UMP and does not grant advertising consent.

Before release, update the published privacy policy and Play Data safety for the SDK's actual collection, including app-instance identifiers, diagnostics and automatically collected events. Inspect Firebase retention, region, access controls and consent requirements. Switching off cannot retract historical server data; apply the owner's documented deletion/retention process.

## Implemented events

| Event | Dimension | Intended use |
| --- | --- | --- |
| screen_view | screen_name | Native tab navigation frequency |
| tool_opened | tool_id | Catalog tools and unit/currency mode popularity |
| calculation_saved | tool_id | Accepted history writes per tool; excludes history previews |
| theme_selected | theme_id | Explicit successful theme selections |
| theme_active | theme_id | Current theme on MainActivity resume and analytics opt-in |
| game_started | game_id | Accepted visible game starts |
| game_completed | game_id | New durable completions, excludes duplicate completion writes |
| game_exited | game_id | Accepted visible game exits |
| games_load_failed | reason | Bridge-reported load errors |
| currency_refresh_requested | source | Manual refresh demand |
| currency_refresh_result | outcome | Available/failed refresh outcomes (available can be cache) |
| history_restore | source | Restore requests |
| graphy_opened | tool_id | Calculator transition into Graphy |
| graphy_export_requested | format | PDF/PNG preparation requests |
| graphy_export_result | outcome | Saved, cancelled or failed export |
| share_opened | content_type | Share chooser requests, not confirmed recipient delivery |
| setting_changed | setting | Decimal and separator setting changes |

Register event-scoped custom dimensions for these fields. Compare event count and active users by tool/theme/game; derive game completion/start ratios with repeat starts in mind. These are activity counts, not exact unique puzzle attempts. Theme selection frequency is not time spent in a theme. No expression, numeric amount, score, history text, currency credentials, raw web payload, ad ID or custom user ID is passed to events.

## Useful next measures

Theme exposure duration; favourite additions/removals; timer starts/completions; rate cache age buckets and provider failure categories; picker empty-result counts; optional reward offer/load/earned/save outcomes; accessibility/font-scale buckets. Add bounded dimensions at authoritative completion points, not on recomposition or every keystroke. Do not infer ad reward or review success from dialog closure. Theme-active events measure resume distribution rather than time spent.

## Validation

Use a configured Firebase development project and DebugView on a debug APK. Verify disabled startup produces no uploads; opt in, exercise each event, check bounded dimensions and absence of inputs; disable/restart and verify no further uploads. Check the merged manifest and Console Data safety after dependency resolution. Live delivery is unverified without project registration.

## In-app review

Play Review 2.0.2 requests a review on leaving History after at least ten persisted calculations/game completions and seven days since first use. Attempts have a 90-day cooldown. Ad/consent activity, destroyed Activity and loss of window focus suppress the request. Errors do not interrupt normal navigation. Settings' explicit rating link still opens Google Play, since the review API is quota-limited and provides no guarantee a card appeared or a review was submitted. Verify real card behavior through Play internal testing/internal app sharing; sideloading alone is insufficient.

Sources checked 2026-10-27: https://firebase.google.com/docs/analytics/android/configure-data-collection ; https://firebase.google.com/support/release-notes/android ; https://developer.android.com/guide/playcore/in-app-review/kotlin-java ; https://developer.android.com/guide/playcore/in-app-review
