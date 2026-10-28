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

/** Restorable unit picker and explicit adult-range reference popup. */
public final class BmiDialogFragment extends DialogFragment {
    public static final String TAG = "bmi_dialog", RESULT = "bmi_unit_choice";
    public static final String HEIGHT = "height", WEIGHT = "weight", RANGES = "ranges";
    private View content;
    private final ThemeManager.Listener listener = theme -> applyTheme();

    public static BmiDialogFragment create(String mode, int selected) {
        BmiDialogFragment fragment = new BmiDialogFragment();
        Bundle args = new Bundle(); args.putString("mode", mode); args.putInt("selected", selected);
        fragment.setArguments(args); return fragment;
    }

    @NonNull @Override public Dialog onCreateDialog(Bundle state) {
        var theme = ThemeManager.get();
        var builder = new MaterialAlertDialogBuilder(requireContext(), R.style.GraphyAlertDialogTheme)
                .setBackground(ThemeApplier.roundedFill(theme.background(), getResources().getDimension(R.dimen.converter_picker_corner)));
        String mode = requireArguments().getString("mode");
        int selected = requireArguments().getInt("selected", -1);
        if (RANGES.equals(mode)) {
            LinearLayout table = new LinearLayout(requireContext());
            table.setOrientation(LinearLayout.VERTICAL);
            int pad = getResources().getDimensionPixelSize(R.dimen.converter_card_padding);
            table.setPadding(pad, 0, pad, 0);
            for (int i = 0; i < BmiFormatter.CATEGORIES.length; i++) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setPadding(0, pad / 2, 0, pad / 2);
                TextView label = new TextView(requireContext(), null, 0, R.style.BmiText);
                label.setText(i == selected ? getString(R.string.bmi_your_range, getString(BmiFormatter.CATEGORIES[i]))
                        : getString(BmiFormatter.CATEGORIES[i]));
                TextView range = new TextView(requireContext(), null, 0, R.style.BmiText);
                range.setText(BmiFormatter.RANGES[i]);
                range.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
                label.setTag(i == selected ? "bmi_selected" : "content_primary");
                range.setTag(label.getTag());
                row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
                row.addView(range, new LinearLayout.LayoutParams(-2, -2));
                table.addView(row);
            }
            content = table;
            builder.setTitle(R.string.bmi_ranges_title).setMessage(R.string.bmi_guidance)
                    .setView(table).setPositiveButton(R.string.bmi_close, null);
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
    private void applyTheme() {
        if (!(getDialog() instanceof AlertDialog dialog)) return;
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(ThemeApplier.roundedFill(
                ThemeManager.get().background(), getResources().getDimension(R.dimen.converter_picker_corner)));
        ThemeApplier.applyDialog(dialog);
        ThemeApplier.apply(content);
        if (content instanceof LinearLayout table) {
            int selected = requireArguments().getInt("selected", -1);
            for (int i = 0; i < table.getChildCount(); i++) {
                LinearLayout row = (LinearLayout) table.getChildAt(i);
                if (i == selected) for (int j = 0; j < row.getChildCount(); j++)
                    ((TextView) row.getChildAt(j)).setTextColor(BmiGaugeView.color(BmiCalculator.Category.values()[i]));
            }
        }
        if (dialog.getListView() != null) {
            dialog.getListView().setBackgroundColor(ThemeManager.get().background());
            applyListColors(dialog.getListView());
        }
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
    @Override public void onDestroyView() { content = null; super.onDestroyView(); }
}
