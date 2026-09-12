# DESIGN SYSTEM
- Map: Keypad (pane_calculator_calculate), Result (@style/ResultDisplay), Card (@drawable/bg_converter_card), List (HorizontalListView), Tabs (@style/WorkspaceTabLayout).
- Layout: Use @dimen. Standard margins/paddings. 3/4 column keypad.
- Comps: themed=MaterialButton. Keys=@style/CalculatorKey. Containers=bg_converter_card.
- Visual: 4dp corners (@style/RoundedCorners). Flat (0dp elevation). Ripples (@color/pad_button_ripple_color). ImageButtons need contentDescription.
- Compose: Graphy layer only. Spacing/Shape same as XML. Use GraphyViewTheme.
- Rules: Reuse pane_*. Use strings.xml/dimens.xml. No hardcoding.
