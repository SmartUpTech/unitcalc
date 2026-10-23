# Google Play listing

The English listing copy is maintained in `app/src/main/res/values/strings.xml`:

| Play Console field | Resource |
| --- | --- |
| App name | `store_listing_title` |
| Short description | `store_listing_short_description` |
| Full description | `store_listing_full_description` |

Copy the resolved text into Play Console > Main store listing. Android string resources do not update the public listing automatically. Convert resource `\n` escapes into line breaks when pasting. Keep the launcher brand as Calculator+.

The title targets calculator and unit conversion searches; the short description includes scientific calculation and currency conversion. The full description explains existing features without rankings, unsupported promises or repeated keyword lists. The legacy `keywords` resource is not a Google Play keywords field.

Google Play limits titles to 30 characters, short descriptions to 80 and full descriptions to 4,000. Source: [Google Play listing setup](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en), checked 2026-10-23.

For the next store asset update, capture the current calculator, unit converter and currency converter with real results and readable text. Measure listing conversion and acquisition search terms in Play Console before and after the change; use a store listing experiment for eligible description or graphic variants. No ranking or download improvement is guaranteed by copy changes alone.
