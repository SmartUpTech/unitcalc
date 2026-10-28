package net.smartlogic.unitconverter.bmi;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.math.BigDecimal;
import org.json.JSONArray;
import org.json.JSONObject;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.IdleHistoryRecorder;
import net.smartlogic.unitconverter.helper.InteractionFeedbackManager;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;

/** Single-screen adult BMI tool, using the canonical converter keypad and history recorder. */
public final class BmiCalculatorFragment extends Fragment {
    private static final int MAX_INPUT_CHARACTERS = 12;
    private static final int MAX_INPUT_FRACTION_DIGITS = 3;
    private EditText height, inches, weight, active;
    private boolean imperialHeight, pounds, binding;
    private BigDecimal canonicalHeight, canonicalWeight;
    private BmiCalculator.Result result;
    private IdleHistoryRecorder history;
    private String restored = "";
    private Bundle recorderState;
    private int activeIndex;
    private final ThemeManager.Listener themeListener = theme -> render();

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        restored = state == null ? Preferences.getInstance(requireContext()).getBmiState() : state.getString("bmi", "");
        recorderState = state;
    }
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_bmi_calculator, container, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        height = view.findViewById(R.id.bmi_height_input); inches = view.findViewById(R.id.bmi_inches_input); weight = view.findViewById(R.id.bmi_weight_input);
        binding = true;
        Bundle savedRecorder = recorderState == null ? new Bundle() : recorderState;
        try {
            JSONObject saved = new JSONObject(restored);
            imperialHeight = saved.optBoolean("imperial"); pounds = saved.optBoolean("pounds");
            height.setText(saved.optString("height")); inches.setText(saved.optString("inches")); weight.setText(saved.optString("weight"));
            activeIndex = saved.optInt("focus", 0);
            canonicalHeight = decimalOrNull(saved.optString("cm")); canonicalWeight = decimalOrNull(saved.optString("kg"));
            JSONArray last = saved.optJSONArray("history");
            if (last != null && last.length() == 3 && state == null)
                savedRecorder.putStringArray("idle_history_last_saved", new String[]{last.getString(0), last.getString(1), last.getString(2)});
        } catch (Exception ignored) { /* Empty/corrupt saved state starts with blank inputs. */ }
        history = new IdleHistoryRecorder(this, savedRecorder);
        binding = false;
        for (EditText field : new EditText[]{height, inches, weight}) {
            field.setShowSoftInputOnFocus(false);
            field.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
                if (binding) return null; // Preserve exact magnitude when converting stored measurements.
                String next = dest.subSequence(0, dstart).toString() + source.subSequence(start, end) + dest.subSequence(dend, dest.length());
                if (end == start && next.matches("[0-9]*(\\.[0-9]*)?")) return null;
                int decimal = next.indexOf('.');
                boolean wholeFeet = imperialHeight && field == height;
                boolean valid = next.length() <= MAX_INPUT_CHARACTERS && next.matches("[0-9]*(\\.[0-9]*)?")
                        && (!wholeFeet || decimal < 0)
                        && (decimal < 0 || next.length() - decimal - 1 <= MAX_INPUT_FRACTION_DIGITS);
                return valid ? null : dest.subSequence(dstart, dend);
            }});
            field.setOnFocusChangeListener((v, focus) -> {
                if (focus) { active = field; activeIndex = field == height ? 0 : field == inches ? 1 : 2; }
                render();
            });
            field.addTextChangedListener(new TextWatcher() {
                private String lastText = field.getText().toString();
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                public void onTextChanged(CharSequence s, int start, int before, int count) { }
                public void afterTextChanged(Editable value) {
                    String next = value.toString();
                    if (next.equals(lastText)) return;
                    lastText = next;
                    if (binding) return;
                    if (field == weight) canonicalWeight = null; else canonicalHeight = null;
                    render(); persist();
                }
            });
        }
        removeKey(view.findViewById(R.id.copy));
        removeKey(view.findViewById(R.id.minus));
        int[] digits = {R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine};
        for (int i = 0; i < digits.length; i++) { String value = Integer.toString(i); action(digits[i], () -> insert(value)); }
        action(R.id.double_zero, () -> insert("00")); action(R.id.dot, () -> insert("."));
        action(R.id.ac, () -> active.setText(""));
        action(R.id.backspace, () -> {
            int a = Math.max(0, active.getSelectionStart()), b = Math.max(0, active.getSelectionEnd());
            if (a != b) active.getText().delete(Math.min(a, b), Math.max(a, b));
            else if (a > 0) active.getText().delete(a - 1, a);
        });
        action(R.id.bmi_ranges, () -> openDialog(BmiDialogFragment.RANGES, result == null ? -1 : result.category().ordinal()));
        action(R.id.bmi_height_unit, () -> openDialog(BmiDialogFragment.HEIGHT, imperialHeight ? 1 : 0));
        action(R.id.bmi_weight_unit, () -> openDialog(BmiDialogFragment.WEIGHT, pounds ? 1 : 0));
        getChildFragmentManager().setFragmentResultListener(BmiDialogFragment.RESULT, getViewLifecycleOwner(), (key, choice) ->
                changeUnit(choice.getString("mode"), choice.getInt("selected") == 1));
        updateUnits();
        active = activeIndex == 2 ? weight : activeIndex == 1 && imperialHeight ? inches : height;
        active.requestFocus(); active.setSelection(active.length());
        ThemeManager.addListener(themeListener);
        render();
    }
    private static BigDecimal decimalOrNull(String value) {
        try { return value.isEmpty() ? null : new BigDecimal(value); } catch (NumberFormatException e) { return null; }
    }
    private void removeKey(View key) {
        ViewGroup row = (ViewGroup) key.getParent();
        int index = row.indexOfChild(key);
        ViewGroup.LayoutParams params = key.getLayoutParams();
        row.removeView(key);
        // Retain canonical number-key column positions, without an invisible interactive key.
        row.addView(new android.widget.Space(requireContext()), index, params);
    }
    private void action(int id, Runnable run) {
        View control = requireView().findViewById(id); InteractionFeedbackManager.configure(control);
        control.setOnClickListener(v -> { InteractionFeedbackManager.perform(v, InteractionFeedbackManager.Type.INPUT); run.run(); });
    }
    private void insert(String value) {
        int a = Math.max(0, active.getSelectionStart()), b = Math.max(0, active.getSelectionEnd());
        active.getText().replace(Math.min(a, b), Math.max(a, b), value);
    }
    private void openDialog(String mode, int selected) {
        if (getChildFragmentManager().isStateSaved() || getChildFragmentManager().findFragmentByTag(BmiDialogFragment.TAG) != null) return;
        BmiDialogFragment.create(mode, selected).showNow(getChildFragmentManager(), BmiDialogFragment.TAG);
    }
    private BigDecimal heightCm() {
        return canonicalHeight == null ? BmiCalculator.heightCm(height.getText().toString(), inches.getText().toString(), imperialHeight) : canonicalHeight;
    }
    private BigDecimal weightKg() {
        return canonicalWeight == null ? BmiCalculator.weightKg(weight.getText().toString(), pounds) : canonicalWeight;
    }
    private void changeUnit(String mode, boolean imperial) {
        binding = true;
        if (BmiDialogFragment.HEIGHT.equals(mode) && imperialHeight != imperial) {
            BigDecimal cm = null; try { cm = heightCm(); } catch (IllegalArgumentException ignored) { }
            imperialHeight = imperial;
            if (cm != null) {
                canonicalHeight = cm;
                if (imperial) {
                    BigDecimal total = cm.divide(BmiCalculator.CM_PER_INCH, BmiCalculator.PRECISION);
                    BigDecimal feet = total.divideToIntegralValue(BmiCalculator.INCHES_PER_FOOT);
                    BigDecimal remaining = total.subtract(feet.multiply(BmiCalculator.INCHES_PER_FOOT)).setScale(3, java.math.RoundingMode.HALF_EVEN);
                    if (remaining.compareTo(BmiCalculator.INCHES_PER_FOOT) >= 0) { feet = feet.add(BigDecimal.ONE); remaining = BigDecimal.ZERO; }
                    height.setText(feet.toPlainString()); inches.setText(BmiFormatter.input(remaining));
                } else { height.setText(BmiFormatter.input(cm)); inches.setText(""); }
            } else { height.setText(""); inches.setText(""); canonicalHeight = null; }
        } else if (BmiDialogFragment.WEIGHT.equals(mode) && pounds != imperial) {
            BigDecimal kg = null; try { kg = weightKg(); } catch (IllegalArgumentException ignored) { }
            pounds = imperial;
            canonicalWeight = kg;
            weight.setText(kg == null ? "" : BmiFormatter.input(BmiFormatter.mass(kg, pounds)));
        }
        binding = false;
        updateUnits();
        if (active == inches && !imperialHeight) height.requestFocus();
        render(); persist();
    }
    private void updateUnits() {
        View heightPanel = (View) height.getParent().getParent();
        View weightPanel = (View) weight.getParent().getParent();
        ((android.widget.LinearLayout.LayoutParams) heightPanel.getLayoutParams()).weight = imperialHeight ? 1.2f : 1f;
        ((android.widget.LinearLayout.LayoutParams) weightPanel.getLayoutParams()).weight = imperialHeight ? 0.8f : 1f;
        int unitWidth = getResources().getDimensionPixelSize(R.dimen.converter_category_min_width);
        requireView().findViewById(R.id.bmi_height_unit).getLayoutParams().width = unitWidth;
        requireView().findViewById(R.id.bmi_weight_unit).getLayoutParams().width = unitWidth;
        inches.setVisibility(imperialHeight ? View.VISIBLE : View.GONE);
        requireView().findViewById(R.id.bmi_feet_label).setVisibility(imperialHeight ? View.VISIBLE : View.GONE);
        height.setContentDescription(getString(imperialHeight ? R.string.bmi_height_ft : R.string.bmi_height_cm));
        weight.setContentDescription(getString(pounds ? R.string.bmi_weight_lb : R.string.bmi_weight_kg));
        text(R.id.bmi_height_unit, getString(R.string.bmi_unit_button, getString(imperialHeight ? R.string.bmi_in : R.string.bmi_cm)));
        text(R.id.bmi_weight_unit, getString(R.string.bmi_unit_button, getString(pounds ? R.string.bmi_lb : R.string.bmi_kg)));
        requireView().findViewById(R.id.bmi_height_unit).setContentDescription(getString(R.string.bmi_measurement,
                getString(R.string.bmi_height_unit), getString(imperialHeight ? R.string.bmi_imperial_height : R.string.bmi_metric_height)));
        requireView().findViewById(R.id.bmi_weight_unit).setContentDescription(getString(R.string.bmi_measurement,
                getString(R.string.bmi_weight_unit), getString(pounds ? R.string.bmi_imperial_weight : R.string.bmi_metric_weight)));
    }
    private void render() {
        if (getView() == null || height == null) return;
        result = null;
        try { result = BmiCalculator.calculate(heightCm(), weightKg()); } catch (IllegalArgumentException | ArithmeticException ignored) { }
        var theme = ThemeManager.get();
        requireView().setBackgroundColor(theme.background()); ThemeApplier.apply(requireView());
        for (EditText field : new EditText[]{height, inches, weight}) {
            field.setTextColor(theme.primaryContentColor()); field.setHintTextColor(theme.secondaryContentColor());
            // The shared converter_amount tag owns the outline and its semantic color.
            field.setBackgroundTintList(null);
        }
        BmiFormatter f = new BmiFormatter(requireContext());
        ((BmiSummaryLayout) requireView().findViewById(R.id.bmi_summary)).setHasResult(result != null);
        ((BmiGaugeView) requireView().findViewById(R.id.bmi_gauge)).setResult(result);
        text(R.id.bmi_value, result == null ? getString(R.string.bmi_no_result) : f.number(result.bmi()));
        boolean empty = height.length() == 0 || weight.length() == 0;
        boolean invalidImperialHeight = false;
        if (imperialHeight && height.length() > 0) {
            try { heightCm(); } catch (IllegalArgumentException ignored) { invalidImperialHeight = true; }
        }
        int prompt = empty ? R.string.bmi_empty : invalidImperialHeight ? R.string.bmi_invalid_imperial : R.string.bmi_invalid;
        text(R.id.bmi_category_value, result == null ? getString(prompt) : f.category(result));
        text(R.id.bmi_range_value, result == null ? getString(R.string.bmi_no_result) : getString(R.string.bmi_range_result, f.range(result, pounds)));
        text(R.id.bmi_difference_value, result == null ? getString(R.string.bmi_no_result) : f.difference(result, pounds));
        int color = result == null ? theme.secondaryContentColor() : BmiGaugeView.color(result.category());
        for (int id : new int[]{R.id.bmi_value, R.id.bmi_category_value, R.id.bmi_difference_value}) requireView().<TextView>findViewById(id).setTextColor(color);
        if (result == null) history.invalidate();
        else {
            String h = imperialHeight ? getString(R.string.bmi_feet_inches, height.getText(), inches.length() == 0 ? "0" : inches.getText())
                    : getString(R.string.bmi_measurement, height.getText(), getString(R.string.bmi_cm));
            String w = getString(R.string.bmi_measurement, weight.getText(), f.unit(pounds));
            history.update(getString(R.string.bmi_history_input, h, w), f.summary(result, pounds), CalculatorCatalog.ID_BMI);
        }
    }
    private void text(int id, String value) { requireView().<TextView>findViewById(id).setText(value); }
    public void reset() {
        if (height == null) return;
        binding = true; height.setText(""); inches.setText(""); weight.setText(""); canonicalHeight = canonicalWeight = null;
        binding = false; height.requestFocus(); render(); persist();
    }
    private String snapshot() {
        if (height == null) return restored;
        try {
            JSONObject saved = new JSONObject().put("version", 1).put("height", height.getText().toString())
                    .put("inches", inches.getText().toString()).put("weight", weight.getText().toString())
                    .put("imperial", imperialHeight).put("pounds", pounds).put("focus", activeIndex)
                    .put("cm", canonicalHeight == null ? "" : canonicalHeight.toPlainString())
                    .put("kg", canonicalWeight == null ? "" : canonicalWeight.toPlainString());
            Bundle marker = new Bundle(); history.saveState(marker);
            String[] last = marker.getStringArray("idle_history_last_saved");
            if (last != null) saved.put("history", new JSONArray(java.util.Arrays.asList(last)));
            return saved.toString();
        } catch (Exception ignored) { return ""; }
    }
    private void persist() { if (height != null) { restored = snapshot(); Preferences.getInstance(requireContext()).setBmiState(restored); } }
    @Override public void onResume() { super.onResume(); render(); if (history != null) history.visibilityChanged(); }
    @Override public void onPause() { persist(); super.onPause(); }
    @Override public void onHiddenChanged(boolean hidden) { super.onHiddenChanged(hidden); if (history != null) { persist(); history.visibilityChanged(); } }
    @Override public void onSaveInstanceState(@NonNull Bundle out) { super.onSaveInstanceState(out); out.putString("bmi", snapshot()); if (history != null) history.saveState(out); }
    @Override public void onDestroyView() {
        persist(); ThemeManager.removeListener(themeListener);
        if (history != null) { history.invalidate(); getLifecycle().removeObserver(history); }
        history = null; height = inches = weight = active = null; super.onDestroyView();
    }
}
