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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
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
    private static final int MAX_AGE_CHARACTERS = 3;
    private enum Gender { MALE, FEMALE }
    private EditText age, height, inches, weight, active;
    private MaterialButton genderMale, genderFemale;
    private Gender gender = Gender.MALE;
    private boolean imperialHeight, pounds, binding;
    private BigDecimal canonicalHeight, canonicalWeight;
    private BmiCalculator.Result result;
    private BmiAgeAssessment.Assessment assessment;
    private IdleHistoryRecorder history;
    private String restored = "";
    private Bundle recorderState;
    private int activeIndex;
    private int ageMonths;
    private boolean keypadExpanded;
    private androidx.activity.OnBackPressedCallback keypadBack;
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
        age = view.findViewById(R.id.bmi_age_input);
        height = view.findViewById(R.id.bmi_height_input); inches = view.findViewById(R.id.bmi_inches_input); weight = view.findViewById(R.id.bmi_weight_input);
        genderMale = view.findViewById(R.id.bmi_gender_male);
        genderFemale = view.findViewById(R.id.bmi_gender_female);
        binding = true;
        Bundle savedRecorder = recorderState == null ? new Bundle() : recorderState;
        try {
            JSONObject saved = new JSONObject(restored);
            imperialHeight = saved.optBoolean("imperial"); pounds = saved.optBoolean("pounds");
            if (saved.optInt("version", 1) < 2) age.setText(saved.optString("age", "25"));
            else age.setText(saved.optString("age"));
            gender = Gender.valueOf(saved.optString("gender", Gender.MALE.name()));
            height.setText(saved.optString("height")); inches.setText(saved.optString("inches")); weight.setText(saved.optString("weight"));
            activeIndex = saved.optInt("focus", 0);
            ageMonths = Math.max(0, Math.min(11, saved.optInt("ageMonths", 0)));
            keypadExpanded = state != null && state.getBoolean("bmi_keypad_expanded");
            canonicalHeight = decimalOrNull(saved.optString("cm")); canonicalWeight = decimalOrNull(saved.optString("kg"));
            JSONArray last = saved.optJSONArray("history");
            if (last != null && last.length() == 3 && state == null)
                savedRecorder.putStringArray("idle_history_last_saved", new String[]{last.getString(0), last.getString(1), last.getString(2)});
        } catch (Exception ignored) {
            age.setText("25");
            gender = Gender.MALE;
        }
        history = new IdleHistoryRecorder(this, savedRecorder);
        binding = false;
        MaterialButtonToggleGroup genderGroup = view.findViewById(R.id.bmi_gender_group);
        genderGroup.check(gender == Gender.FEMALE ? R.id.bmi_gender_female : R.id.bmi_gender_male);
        genderGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || binding) return;
            gender = checkedId == R.id.bmi_gender_female ? Gender.FEMALE : Gender.MALE;
            styleGender(); render(); persist();
        });
        for (EditText field : new EditText[]{age, height, inches, weight}) {
            final EditText bound = field;
            field.setShowSoftInputOnFocus(false);
            field.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
                if (binding) return null;
                String next = dest.subSequence(0, dstart).toString() + source.subSequence(start, end) + dest.subSequence(dend, dest.length());
                if (bound == age) {
                    if (end == start && next.matches("[0-9]*")) return null;
                    return next.length() <= MAX_AGE_CHARACTERS && next.matches("[0-9]*") ? null : dest.subSequence(dstart, dend);
                }
                if (end == start && next.matches("[0-9]*(\\.[0-9]*)?")) return null;
                int decimal = next.indexOf('.');
                boolean wholeFeet = imperialHeight && bound == height;
                boolean valid = next.length() <= MAX_INPUT_CHARACTERS && next.matches("[0-9]*(\\.[0-9]*)?")
                        && (!wholeFeet || decimal < 0)
                        && (decimal < 0 || next.length() - decimal - 1 <= MAX_INPUT_FRACTION_DIGITS);
                return valid ? null : dest.subSequence(dstart, dend);
            }});
            field.setOnFocusChangeListener((v, focus) -> {
                if (focus) {
                    active = bound;
                    activeIndex = bound == age ? 0 : bound == height ? 1 : bound == inches ? 2 : 3;
                    render();
                    updateKeypad();
                }
            });
            field.setOnClickListener(v -> { field.requestFocus(); showKeypad(); });
            // EditText consumes the first tap when it gains focus; open after that touch too.
            field.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == android.view.MotionEvent.ACTION_UP)
                    v.post(() -> { if (getView() != null) { field.requestFocus(); showKeypad(); } });
                return false;
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
                    if (field == weight) canonicalWeight = null; else if (field != age) canonicalHeight = null;
                    render(); persist();
                }
            });
        }
        keypadBack = new androidx.activity.OnBackPressedCallback(keypadExpanded) {
            @Override public void handleOnBackPressed() { closeKeypad(); }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), keypadBack);
        action(R.id.bmi_results, () -> {
            if (getChildFragmentManager().isStateSaved() || getChildFragmentManager().findFragmentByTag(BmiDialogFragment.TAG) != null) return;
            Integer years = parsedAge();
            BmiDialogFragment.ranges(years == null ? -1 : years * 12 + ageMonths, gender == Gender.FEMALE,
                    result == null ? -1 : result.category().ordinal()).showNow(getChildFragmentManager(), BmiDialogFragment.TAG);
        });
        view.findViewById(R.id.bmi_ranges).setOnClickListener(v -> view.findViewById(R.id.bmi_results).performClick());
        action(R.id.bmi_age_months, () -> openDialog(BmiDialogFragment.MONTHS, ageMonths));
        action(R.id.bmi_height_unit, () -> openDialog(BmiDialogFragment.HEIGHT, imperialHeight ? 1 : 0));
        action(R.id.bmi_weight_unit, () -> openDialog(BmiDialogFragment.WEIGHT, pounds ? 1 : 0));
        getChildFragmentManager().setFragmentResultListener(BmiDialogFragment.RESULT, getViewLifecycleOwner(), (key, choice) ->
                {
                    if (BmiDialogFragment.MONTHS.equals(choice.getString("mode"))) {
                        ageMonths = choice.getInt("selected"); updateUnits(); render(); persist();
                    } else changeUnit(choice.getString("mode"), choice.getInt("selected") == 1);
                });
        updateUnits();
        active = fieldForIndex(activeIndex);
        binding = true;
        active.requestFocus(); active.setSelection(active.length());
        binding = false;
        updateKeypad();
        ThemeManager.addListener(themeListener);
        styleGender();
        render();
    }
    private EditText fieldForIndex(int index) {
        if (index == 0) return age;
        if (index == 2 && imperialHeight) return inches;
        if (index == 3) return weight;
        return height;
    }
    private static BigDecimal decimalOrNull(String value) {
        try { return value.isEmpty() ? null : new BigDecimal(value); } catch (NumberFormatException e) { return null; }
    }
    private void showKeypad() {
        if (binding || getView() == null) return;
        keypadExpanded = true;
        updateKeypad();
    }
    public boolean isKeypadExpanded() { return keypadExpanded; }
    public void closeKeypad() {
        keypadExpanded = false;
        render();
        updateKeypad();
    }
    private void updateKeypad() {
        if (getView() == null) return;
        requireView().findViewById(R.id.bmi_keypad_host).setVisibility(keypadExpanded ? View.VISIBLE : View.GONE);
        requireView().findViewById(R.id.bmi_results).setVisibility(keypadExpanded ? View.INVISIBLE : View.VISIBLE);
        View dot = requireView().findViewById(R.id.dot);
        if (dot != null) dot.setEnabled(active != age);
        if (keypadBack != null) keypadBack.setEnabled(keypadExpanded && !isHidden());
    }
    void onKeypadKey(int id) {
        if (active == null) return;
        if (id == R.id.equal) { closeKeypad(); return; }
        if (id == R.id.ac) { active.setText(""); return; }
        if (id == R.id.backspace) {
            int a = Math.max(0, active.getSelectionStart()), b = Math.max(0, active.getSelectionEnd());
            if (a != b) active.getText().delete(Math.min(a, b), Math.max(a, b));
            else if (a > 0) active.getText().delete(a - 1, a);
            return;
        }
        if (id == R.id.dot) { insert("."); return; }
        if (id == R.id.double_zero) { insert("00"); return; }
        int[] digits = {R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine};
        for (int i = 0; i < digits.length; i++) if (id == digits[i]) { insert(Integer.toString(i)); return; }
    }
    private void action(int id, Runnable run) {
        View control = requireView().findViewById(id); InteractionFeedbackManager.configure(control);
        control.setOnClickListener(v -> { InteractionFeedbackManager.perform(v, InteractionFeedbackManager.Type.INPUT); run.run(); });
    }
    private void insert(String value) {
        if (active == null) return;
        if (active == age && (value.contains("."))) return;
        Editable text = active.getText();
        int start = Math.max(0, Math.min(active.getSelectionStart(), active.getSelectionEnd()));
        int end = Math.max(start, Math.max(active.getSelectionStart(), active.getSelectionEnd()));
        text.replace(start, end, value);
        active.setSelection(Math.min(text.length(), start + value.length()));
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
                    height.setText(feet.stripTrailingZeros().toPlainString()); inches.setText(BmiFormatter.input(remaining));
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
        text(R.id.bmi_age_months, getString(R.string.bmi_months_button, ageMonths));
        requireView().findViewById(R.id.bmi_age_months).setContentDescription(getString(R.string.bmi_months_choice, ageMonths));
        View heightPanel = requireView().findViewById(R.id.bmi_height_column);
        View weightPanel = requireView().findViewById(R.id.bmi_weight_column);
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
    private void styleGender() {
        genderMale.setTag(gender == Gender.MALE ? "theme_filled_action" : "theme_outline_action");
        genderFemale.setTag(gender == Gender.FEMALE ? "theme_filled_action" : "theme_outline_action");
        if (getView() != null) ThemeApplier.apply(requireView().findViewById(R.id.bmi_gender_group));
    }
    private Integer parsedAge() {
        if (age.length() == 0) return null;
        try { return Integer.parseInt(age.getText().toString()); } catch (NumberFormatException e) { return null; }
    }
    private String genderLabel() {
        return getString(gender == Gender.FEMALE ? R.string.bmi_gender_female : R.string.bmi_gender_male);
    }
    private void render() {
        if (getView() == null || height == null) return;
        result = null; assessment = null;
        Integer years = parsedAge();
        if (years != null) {
            try {
                assessment = years < 2 ? new BmiAgeAssessment.Assessment(BmiAgeAssessment.Group.INFANT, null, -1, null)
                        : BmiAgeAssessment.assess(heightCm(), weightKg(), years * 12 + ageMonths, gender == Gender.FEMALE);
                result = assessment.result();
            } catch (IllegalArgumentException | ArithmeticException ignored) { }
        }
        var theme = ThemeManager.get();
        requireView().setBackgroundColor(theme.background()); ThemeApplier.apply(requireView());
        for (EditText field : new EditText[]{age, height, inches, weight}) {
            field.setTextColor(theme.primaryContentColor()); field.setHintTextColor(theme.secondaryContentColor());
            field.setBackgroundTintList(null);
        }
        BmiFormatter f = new BmiFormatter(requireContext());
        ((BmiSummaryLayout) requireView().findViewById(R.id.bmi_summary)).setHasResult(result != null);
        ((BmiGaugeView) requireView().findViewById(R.id.bmi_gauge)).setAssessment(assessment);
        text(R.id.bmi_value, result == null ? getString(R.string.bmi_no_result) : f.number(result.bmi()));
        boolean empty = height.length() == 0 || weight.length() == 0;
        boolean invalidImperialHeight = false;
        if (imperialHeight && height.length() > 0) {
            try { heightCm(); } catch (IllegalArgumentException ignored) { invalidImperialHeight = true; }
        }
        int prompt = empty ? R.string.bmi_empty : invalidImperialHeight ? R.string.bmi_invalid_imperial : R.string.bmi_invalid;
        boolean child = assessment != null && assessment.group() == BmiAgeAssessment.Group.CHILD;
        boolean infant = years != null && years < 2;
        text(R.id.bmi_category_value, infant ? getString(R.string.bmi_infant_category)
                : result == null ? getString(R.string.bmi_no_result) : child ? f.childCategory(assessment) : f.category(result));
        text(R.id.bmi_range_value, result == null ? getString(R.string.bmi_no_result) : child ? f.childRange(assessment)
                : getString(R.string.bmi_range_result, f.range(result, pounds)));
        text(R.id.bmi_difference_value, result == null ? getString(R.string.bmi_no_result) : child ? f.childBand(assessment) : f.difference(result, pounds));
        TextView message = requireView().findViewById(R.id.bmi_message);
        if (years == null) message.setText(R.string.bmi_age_required);
        else if (infant) message.setText(R.string.bmi_infant_note);
        else if (result == null) message.setText(prompt);
        else message.setText("");
        requireView().findViewById(R.id.bmi_results).setContentDescription(getString(R.string.bmi_result_accessibility,
                requireView().<TextView>findViewById(R.id.bmi_value).getText(),
                requireView().<TextView>findViewById(R.id.bmi_category_value).getText(),
                requireView().<TextView>findViewById(R.id.bmi_range_value).getText(),
                result == null ? message.getText() : requireView().<TextView>findViewById(R.id.bmi_difference_value).getText()));
        int color = result == null ? theme.secondaryContentColor() : BmiGaugeView.color(result.category());
        for (int id : new int[]{R.id.bmi_value, R.id.bmi_category_value, R.id.bmi_difference_value}) requireView().<TextView>findViewById(id).setTextColor(color);
        styleGender();
        if (result == null) history.invalidate();
        else {
            String ageText = getString(R.string.bmi_history_age, age.getText().toString(), ageMonths);
            String h = imperialHeight ? getString(R.string.bmi_history_feet_inches, height.getText(), inches.length() == 0 ? "0" : inches.getText())
                    : getString(R.string.bmi_history_measurement, height.getText(), getString(R.string.bmi_cm));
            String w = getString(R.string.bmi_history_measurement, weight.getText(), f.unit(pounds));
            history.update(getString(R.string.bmi_history_input, ageText, getString(gender == Gender.MALE ? R.string.bmi_history_male : R.string.bmi_history_female), h, w), f.summary(assessment, pounds), CalculatorCatalog.ID_BMI);
        }
    }
    private void text(int id, String value) { requireView().<TextView>findViewById(id).setText(value); }
    public void restoreFromHistory(String expression) {
        BmiHistoryInputs inputs = BmiHistoryInputs.parse(expression);
        if (inputs == null || height == null) return;
        binding = true;
        imperialHeight = inputs.imperial(); pounds = inputs.pounds();
        ageMonths = inputs.months(); gender = inputs.female() ? Gender.FEMALE : Gender.MALE;
        canonicalHeight = canonicalWeight = null;
        age.setText(inputs.years()); height.setText(inputs.height()); inches.setText(inputs.inches()); weight.setText(inputs.weight());
        updateUnits();
        ((MaterialButtonToggleGroup) requireView().findViewById(R.id.bmi_gender_group)).check(inputs.female() ? R.id.bmi_gender_female : R.id.bmi_gender_male);
        binding = false;
        closeKeypad(); persist();
    }

    public void reset() {
        if (height == null) return;
        binding = true; age.setText("25"); height.setText(""); inches.setText(""); weight.setText(""); gender = Gender.MALE; ageMonths = 0; updateUnits();
        canonicalHeight = canonicalWeight = null;
        ((MaterialButtonToggleGroup) requireView().findViewById(R.id.bmi_gender_group)).check(R.id.bmi_gender_male);
        binding = false; age.requestFocus(); styleGender(); render(); persist();
    }
    private String snapshot() {
        if (height == null) return restored;
        try {
            JSONObject saved = new JSONObject().put("version", 3).put("ageMonths", ageMonths).put("age", age.getText().toString())
                    .put("gender", gender.name()).put("height", height.getText().toString())
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
    @Override public void onHiddenChanged(boolean hidden) { super.onHiddenChanged(hidden); if (history != null) { persist(); history.visibilityChanged(); } updateKeypad(); }
    @Override public void onSaveInstanceState(@NonNull Bundle out) { super.onSaveInstanceState(out); out.putString("bmi", snapshot()); out.putBoolean("bmi_keypad_expanded", keypadExpanded); if (history != null) history.saveState(out); }
    @Override public void onDestroyView() {
        persist(); ThemeManager.removeListener(themeListener);
        if (history != null) { history.invalidate(); getLifecycle().removeObserver(history); }
        history = null; age = height = inches = weight = active = null; genderMale = genderFemale = null; super.onDestroyView();
    }
}
