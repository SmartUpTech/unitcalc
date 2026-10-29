package net.smartlogic.unitconverter.bmi;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;

/** Restorable unit/month pickers and age-group reference sections. */
public final class BmiDialogFragment extends DialogFragment {
    public static final String TAG = "bmi_dialog", RESULT = "bmi_unit_choice";
    public static final String HEIGHT = "height", WEIGHT = "weight", RANGES = "ranges", MONTHS = "months";
    private View content;
    private LinearLayout referenceBody;
    private int section = 2;
    private final ThemeManager.Listener listener = theme -> applyTheme();

    public static BmiDialogFragment create(String mode, int selected) {
        BmiDialogFragment fragment = new BmiDialogFragment();
        Bundle args = new Bundle(); args.putString("mode", mode); args.putInt("selected", selected);
        fragment.setArguments(args); return fragment;
    }

    public static BmiDialogFragment ranges(int ageMonths, boolean female, int selected) {
        BmiDialogFragment dialog = create(RANGES, selected);
        dialog.requireArguments().putInt("ageMonths", ageMonths);
        dialog.requireArguments().putBoolean("female", female);
        return dialog;
    }

    @NonNull @Override public Dialog onCreateDialog(Bundle state) {
        var theme = ThemeManager.get();
        var builder = new MaterialAlertDialogBuilder(requireContext(), R.style.GraphyAlertDialogTheme)
                .setBackground(ThemeApplier.roundedFill(theme.background(), getResources().getDimension(R.dimen.converter_picker_corner)));
        String mode = requireArguments().getString("mode");
        int selected = requireArguments().getInt("selected", -1);
        if (RANGES.equals(mode)) {
            int months = requireArguments().getInt("ageMonths", 300);
            section = state == null ? months >= 0 && months < 24 ? 0 : months >= 24 && months < 240 ? 1 : 2 : state.getInt("section", 2);
            LinearLayout panel = new LinearLayout(requireContext());
            panel.setOrientation(LinearLayout.VERTICAL);
            int pad = getResources().getDimensionPixelSize(R.dimen.converter_card_padding);
            panel.setPadding(pad, 0, pad, 0);
            com.google.android.material.tabs.TabLayout tabs = new com.google.android.material.tabs.TabLayout(requireContext());
            tabs.setId(R.id.bmi_reference_tabs);
            int[] groups = {R.string.bmi_group_infants, R.string.bmi_group_children, R.string.bmi_group_adults};
            for (int title : groups) tabs.addTab(tabs.newTab().setText(title));
            tabs.setTabTextColors(theme.secondaryContentColor(), theme.accentContentColor());
            tabs.setSelectedTabIndicatorColor(theme.accentContentColor());
            tabs.selectTab(tabs.getTabAt(section));
            panel.addView(tabs);
            android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext()) {
                @Override protected void onMeasure(int ws, int hs) {
                    // Secondary reference content may scroll; the calculator itself never scrolls.
                    int cap = getResources().getDisplayMetrics().heightPixels / 2;
                    if (View.MeasureSpec.getMode(hs) != View.MeasureSpec.UNSPECIFIED)
                        cap = Math.min(cap, View.MeasureSpec.getSize(hs));
                    super.onMeasure(ws, View.MeasureSpec.makeMeasureSpec(cap, View.MeasureSpec.AT_MOST));
                }
            };
            referenceBody = new LinearLayout(requireContext());
            scroll.setId(R.id.bmi_reference_scroll);
            scroll.setScrollbarFadingEnabled(false);
            referenceBody.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(referenceBody);
            panel.addView(scroll);
            tabs.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                    section = tab.getPosition(); populateReference(); scroll.scrollTo(0, 0);
                }
                public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) { }
                public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) { }
            });
            content = panel;
            populateReference();
            builder.setTitle(R.string.bmi_reference_title).setView(panel).setPositiveButton(R.string.bmi_close, null);
        } else if (MONTHS.equals(mode)) {
            String[] months = new String[12];
            for (int i = 0; i < months.length; i++) months[i] = getString(R.string.bmi_months_choice, i);
            builder.setTitle(R.string.bmi_additional_months).setSingleChoiceItems(months, selected, (dialog, which) -> {
                Bundle choice = new Bundle(); choice.putString("mode", MONTHS); choice.putInt("selected", which);
                getParentFragmentManager().setFragmentResult(RESULT, choice); dismiss();
            }).setNegativeButton(android.R.string.cancel, null);
        } else {
            boolean height = HEIGHT.equals(mode);
            String[] units = {getString(height ? R.string.bmi_metric_height : R.string.bmi_metric_weight),
                    getString(height ? R.string.bmi_imperial_height : R.string.bmi_imperial_weight)};
            builder.setTitle(height ? R.string.bmi_height_unit : R.string.bmi_weight_unit)
                    .setSingleChoiceItems(units, selected, (dialog, which) -> {
                        Bundle choice = new Bundle(); choice.putString("mode", mode); choice.putInt("selected", which);
                        getParentFragmentManager().setFragmentResult(RESULT, choice); dismiss();
                    }).setNegativeButton(android.R.string.cancel, null);
        }
        return builder.create();
    }
    private void populateReference() {
        if (referenceBody == null) return;
        referenceBody.removeAllViews();
        String guidance = getString(section == 0 ? R.string.bmi_infant_guidance : section == 1 ? R.string.bmi_child_guidance : R.string.bmi_adult_guidance);
        int lineBreak = guidance.indexOf('\n');
        addText(lineBreak < 0 ? guidance : guidance.substring(0, lineBreak), false);
        String childReferenceDetails = null;
        int months = requireArguments().getInt("ageMonths", 300);
        if (section == 1 && months >= 24 && months < 240) {
            boolean female = requireArguments().getBoolean("female");
            addText(getString(R.string.bmi_child_current,
                    getString(R.string.bmi_age_full, Integer.toString(months / 12), months % 12),
                    getString(female ? R.string.bmi_gender_female : R.string.bmi_gender_male)), false);
            var reference = BmiAgeAssessment.reference(months, female);
            BmiFormatter formatter = new BmiFormatter(requireContext());
            childReferenceDetails = getString(R.string.bmi_child_reference_values, formatter.number(reference.p5()),
                    formatter.number(reference.p85()), formatter.number(reference.p95()));
        }
        if (section > 0) {
            int[] labels = section == 2 ? BmiFormatter.CATEGORIES : new int[]{R.string.bmi_underweight, R.string.bmi_healthy,
                    R.string.bmi_overweight, R.string.bmi_child_obesity, R.string.bmi_child_severe};
            int[] limits = section == 2 ? BmiFormatter.RANGES : new int[]{R.string.bmi_child_low_band, R.string.bmi_child_healthy_band,
                    R.string.bmi_child_over_band, R.string.bmi_child_obesity_band, R.string.bmi_child_severe_rule};
            int ageMonths = requireArguments().getInt("ageMonths", 300);
            boolean current = section == (ageMonths < 24 ? 0 : ageMonths < 240 ? 1 : 2);
            int selected = requireArguments().getInt("selected", -1);
            if (section == 1 && selected == BmiCalculator.Category.OBESITY_III.ordinal()) selected = 4;
            for (int i = 0; i < labels.length; i++) {
                boolean active = current && i == selected;
                String label = getString(labels[i]);
                addText((active ? getString(R.string.bmi_your_range, label) : label) + "\n" + getString(limits[i]), active);
            }
        }
        if (childReferenceDetails != null) addText(childReferenceDetails, false);
        if (lineBreak >= 0) addText(guidance.substring(lineBreak + 1), false);
        addText(getString(R.string.bmi_sources), false);
        TextView source = addText(getString(R.string.bmi_read_source), false);
        android.text.SpannableString link = new android.text.SpannableString(source.getText());
        String url = getString(section == 0 ? R.string.bmi_infant_source_url : section == 1 ? R.string.bmi_child_source_url : R.string.bmi_adult_source_url);
        link.setSpan(new android.text.style.URLSpan(url), 0, link.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        source.setText(link); source.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        source.setLinkTextColor(ThemeManager.get().accentContentColor());
        source.setMinHeight(getResources().getDimensionPixelSize(R.dimen.converter_touch_target));
        ThemeApplier.apply(content);
        applySelectedColors(referenceBody);
    }
    private TextView addText(String value, boolean selected) {
        TextView text = new TextView(requireContext(), null, 0, R.style.BmiText);
        text.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.app_text));
        text.setText(value); text.setTag(selected ? "bmi_selected" : "content_primary");
        int gap = getResources().getDimensionPixelSize(R.dimen.converter_spacing);
        text.setPadding(0, gap, 0, gap);
        referenceBody.addView(text);
        return text;
    }
    @Override public void onSaveInstanceState(@NonNull Bundle state) { super.onSaveInstanceState(state); state.putInt("section", section); }

    private void applyTheme() {
        if (!(getDialog() instanceof AlertDialog dialog)) return;
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(ThemeApplier.roundedFill(
                ThemeManager.get().background(), getResources().getDimension(R.dimen.converter_picker_corner)));
        ThemeApplier.applyDialog(dialog);
        ThemeApplier.apply(content);
        applySelectedColors(referenceBody);
        if (content instanceof LinearLayout panel && panel.getChildAt(0) instanceof com.google.android.material.tabs.TabLayout tabs) {
            tabs.setTabTextColors(ThemeManager.get().secondaryContentColor(), ThemeManager.get().accentContentColor());
            tabs.setSelectedTabIndicatorColor(ThemeManager.get().accentContentColor());
        }
        if (dialog.getListView() != null) {
            dialog.getListView().setBackgroundColor(ThemeManager.get().background());
            applyListColors(dialog.getListView());
        }
    }
    private void applySelectedColors(View view) {
        if (view == null) return;
        if (view instanceof TextView text && "bmi_selected".equals(text.getTag())) {
            int selected = requireArguments().getInt("selected", -1);
            if (selected >= 0 && selected < BmiCalculator.Category.values().length)
                text.setTextColor(BmiGaugeView.color(BmiCalculator.Category.values()[selected]));
        }
        if (view instanceof android.view.ViewGroup group)
            for (int i = 0; i < group.getChildCount(); i++) applySelectedColors(group.getChildAt(i));
    }
    private void applyListColors(android.widget.ListView list) {
        for (int i = 0; i < list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            if (child instanceof TextView text) text.setTextColor(ThemeManager.get().primaryContentColor());
            if (child instanceof android.widget.CheckedTextView checked)
                checked.setCheckMarkTintList(android.content.res.ColorStateList.valueOf(ThemeManager.get().accentContentColor()));
        }
    }
    @Override public void onStart() {
        super.onStart(); ThemeManager.addListener(listener); applyTheme();
        if (requireDialog() instanceof AlertDialog dialog && dialog.getListView() != null)
            dialog.getListView().addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> applyListColors(dialog.getListView()));
    }
    @Override public void onStop() { ThemeManager.removeListener(listener); super.onStop(); }
    @Override public void onDestroyView() { content = null; referenceBody = null; super.onDestroyView(); }
}
