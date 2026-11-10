package net.smartlogic.unitconverter.percentage;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButtonToggleGroup;
import java.math.BigDecimal;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.graphy.compose.GraphyPanelController;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import net.smartlogic.unitconverter.helper.IdleHistoryRecorder;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.helper.WorkspacePagerAdapter;
import net.smartlogic.unitconverter.helper.WorkspaceTabController;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;
import net.smartlogic.unitconverter.utils.NumberUtils;
import net.smartlogic.unitconverter.widget.AutoSizeEditText;
import org.json.JSONObject;

public final class PercentageCalculatorFragment extends Fragment {
    private static final int MAX_INPUT_CHARACTERS = 12;
    private static final int MAX_INPUT_FRACTION_DIGITS = 3;

    private EditText first, second, active;
    private TextView firstLabel, secondLabel, secondSuffix;
    private MaterialButtonToggleGroup modeGroup;
    private PercentageCalculator.Mode mode = PercentageCalculator.Mode.PERCENT_OF;
    private boolean binding;
    private int activeIndex;
    private boolean keypadExpanded;
    private PercentageCalculator.Result result;
    private GraphyOutput graphyOutput;
    private IdleHistoryRecorder history;
    private View calculatePage;
    private WorkspaceTabController tabs;
    private GraphyPanelController graphy;
    private int selectedTab;
    private String restored = "";
    private Bundle recorderState;
    private OnBackPressedCallback back;
    private final ThemeManager.Listener themeListener = theme -> {
        if (getView() != null) ThemeApplier.apply(getView());
        render();
    };

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        restored = state == null
                ? Preferences.getInstance(requireContext()).getPercentageState()
                : state.getString("percentage", "");
        recorderState = state;
        if (state != null) selectedTab = state.getInt("tab", 0);
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_calculator, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        back = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() {
                if (selectedTab == 1) tabs.selectTab(0);
                else closeKeypad();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), back);
        ViewPager2 pager = view.findViewById(R.id.workspace_pager);
        pager.setAdapter(new WorkspacePagerAdapter(
                new int[]{R.layout.pane_percentage_calculate, R.layout.pane_calculator_graphy},
                (position, page) -> {
                    ThemeApplier.apply(page);
                    if (position == 0) {
                        calculatePage = page;
                        bindCalculatePage(page);
                    } else {
                        page.findViewById(R.id.graphy_context).setVisibility(View.GONE);
                        graphy = new GraphyPanelController((ComposeView) page.findViewById(R.id.graphy_panel),
                                this, new GraphyViewTheme(requireContext()));
                        if (graphyOutput != null) graphy.update(graphyOutput);
                    }
                    render();
                }));
        int restoreTab = selectedTab;
        tabs = new WorkspaceTabController(pager, view.findViewById(R.id.workspace_tabs),
                new int[]{R.string.tab_calculate, R.string.graphy}, new WorkspaceTabController.Callback() {
                    @Override public void onTabSelected(int position) {
                        selectedTab = position;
                        updateBack();
                    }

                    @Override public boolean isTabEnabled(int position) {
                        return position == 0 || result != null;
                    }
                });
        if (restoreTab == 1 && result != null) {
            selectedTab = 1;
            tabs.selectTab(1);
        }
        ThemeManager.addListener(themeListener);
        ThemeApplier.apply(view);
    }

    private void bindCalculatePage(View page) {
        first = page.findViewById(R.id.percentage_first_input);
        second = page.findViewById(R.id.percentage_second_input);
        firstLabel = page.findViewById(R.id.percentage_first_label);
        secondLabel = page.findViewById(R.id.percentage_second_label);
        secondSuffix = page.findViewById(R.id.percentage_second_suffix);
        modeGroup = page.findViewById(R.id.percentage_mode_group);
        binding = true;
        Bundle savedRecorder = recorderState == null ? new Bundle() : recorderState;
        PercentageHistoryState draft = PercentageHistoryState.fromDraft(restored);
        if (draft != null) {
            mode = draft.mode;
            first.setText(draft.first);
            second.setText(draft.second);
            activeIndex = draft.focus;
        }
        keypadExpanded = recorderState != null && recorderState.getBoolean("percentage_keypad_expanded");
        modeGroup.check(modeButtonId(mode));
        history = new IdleHistoryRecorder(this, savedRecorder);
        binding = false;
        for (int i = 0; i < fields().length; i++) {
            final int index = i;
            EditText field = fields()[i];
            field.setShowSoftInputOnFocus(false);
            field.setFilters(new InputFilter[]{
                    (source, start, end, dest, dstart, dend) -> {
                        if (binding) return null;
                        String next = dest.subSequence(0, dstart) + source.subSequence(start, end).toString()
                                + dest.subSequence(dend, dest.length());
                        if (end == start && next.matches("-?[0-9]*\\.?[0-9]*")) return null;
                        int dot = next.indexOf('.');
                        if (dot >= 0 && next.length() - dot - 1 > MAX_INPUT_FRACTION_DIGITS) {
                            return dest.subSequence(dstart, dend);
                        }
                        return next.length() <= MAX_INPUT_CHARACTERS && next.matches("-?[0-9]*\\.?[0-9]*")
                                ? null : dest.subSequence(dstart, dend);
                    }});
            field.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable s) {
                    if (binding) return;
                    activeIndex = index;
                    active = field;
                    render();
                    persist();
                }
            });
            field.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    activeIndex = index;
                    active = field;
                    openKeypad();
                }
            });
            field.setOnClickListener(v -> {
                selectField(index);
                openKeypad();
            });
        }
        modeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || binding) return;
            mode = modeFromButton(checkedId);
            updateFieldLabels();
            render();
            persist();
        });
        updateFieldLabels();
        selectField(activeIndex);
        updateKeypad();
        render();
    }

    private EditText[] fields() {
        return new EditText[]{first, second};
    }

    private int modeButtonId(PercentageCalculator.Mode selected) {
        return switch (selected) {
            case PERCENT_OF -> R.id.percentage_mode_of;
            case INCREASE -> R.id.percentage_mode_increase;
            case DECREASE -> R.id.percentage_mode_decrease;
            case WHAT_PERCENT -> R.id.percentage_mode_what;
        };
    }

    private PercentageCalculator.Mode modeFromButton(int id) {
        if (id == R.id.percentage_mode_increase) return PercentageCalculator.Mode.INCREASE;
        if (id == R.id.percentage_mode_decrease) return PercentageCalculator.Mode.DECREASE;
        if (id == R.id.percentage_mode_what) return PercentageCalculator.Mode.WHAT_PERCENT;
        return PercentageCalculator.Mode.PERCENT_OF;
    }

    private void updateFieldLabels() {
        if (firstLabel == null) return;
        boolean what = mode == PercentageCalculator.Mode.WHAT_PERCENT;
        firstLabel.setText(what ? R.string.percentage_part_label : R.string.percentage_base_amount);
        secondLabel.setText(what ? R.string.percentage_whole_label : R.string.percentage_percent_label);
        first.setContentDescription(firstLabel.getText());
        second.setContentDescription(secondLabel.getText());
        secondSuffix.setVisibility(what ? View.GONE : View.VISIBLE);
    }

    private String modeHistoryName() {
        return switch (mode) {
            case PERCENT_OF -> getString(R.string.percentage_mode_of);
            case INCREASE -> getString(R.string.percentage_mode_increase);
            case DECREASE -> getString(R.string.percentage_mode_decrease);
            case WHAT_PERCENT -> getString(R.string.percentage_mode_what);
        };
    }

    public void onKeypadKey(int id) {
        if (active == null) return;
        if (id == R.id.equal) {
            closeKeypad();
            return;
        }
        if (id == R.id.ac) {
            active.setText("");
            return;
        }
        if (id == R.id.backspace) {
            int a = Math.max(0, active.getSelectionStart());
            int b = Math.max(0, active.getSelectionEnd());
            if (a != b) active.getText().delete(Math.min(a, b), Math.max(a, b));
            else if (a > 0) active.getText().delete(a - 1, a);
            return;
        }
        if (id == R.id.dot) {
            insert(".");
            return;
        }
        if (id == R.id.double_zero) {
            insert("00");
            return;
        }
        int[] digits = {R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six, R.id.seven,
                R.id.eight, R.id.nine};
        for (int i = 0; i < digits.length; i++) {
            if (id == digits[i]) {
                insert(Integer.toString(i));
                return;
            }
        }
    }

    private void insert(String value) {
        if (active == null) return;
        Editable text = active.getText();
        int start = Math.max(0, Math.min(active.getSelectionStart(), active.getSelectionEnd()));
        int end = Math.max(start, Math.max(active.getSelectionStart(), active.getSelectionEnd()));
        text.replace(start, end, value);
        active.setSelection(Math.min(text.length(), start + value.length()));
    }

    private void selectField(int index) {
        activeIndex = Math.max(0, Math.min(fields().length - 1, index));
        active = fields()[activeIndex];
        boolean wasExpanded = keypadExpanded;
        active.requestFocus();
        keypadExpanded = wasExpanded;
        updateKeypad();
    }

    private void openKeypad() {
        keypadExpanded = true;
        updateKeypad();
    }

    private void closeKeypad() {
        keypadExpanded = false;
        updateKeypad();
    }

    private void updateKeypad() {
        if (calculatePage == null) return;
        View host = calculatePage.findViewById(R.id.percentage_keypad_host);
        View results = calculatePage.findViewById(R.id.percentage_results);
        if (host == null) return;
        host.setVisibility(keypadExpanded ? View.VISIBLE : View.GONE);
        if (results != null) results.setVisibility(keypadExpanded ? View.GONE : View.VISIBLE);
        if (back != null) back.setEnabled(!isHidden() && (selectedTab == 1 || keypadExpanded));
    }

    private void updateBack() {
        if (back != null) back.setEnabled(!isHidden() && (selectedTab == 1 || keypadExpanded));
        updateKeypad();
    }

    private BigDecimal decimalOrNull(String text) {
        if (text == null || text.isBlank() || "-".equals(text.trim()) || ".".equals(text.trim())
                || "-.".equals(text.trim())) {
            return null;
        }
        try {
            return NumberUtils.parseDecimal(text.trim());
        } catch (NumberFormatException | ArithmeticException invalid) {
            return null;
        }
    }

    private String formatNumber(BigDecimal value) {
        Preferences preferences = Preferences.getInstance(requireContext());
        int decimals = preferences.getNumberDecimals();
        String decimal = preferences.getDecimalSeparator();
        String group = preferences.getGroupSeparator();
        char dec = decimal.isEmpty() ? '.' : decimal.charAt(0);
        char sep = group.isEmpty() ? ',' : group.charAt(0);
        boolean grouping = !group.isEmpty() && !"None".equalsIgnoreCase(group)
                && !group.equals(getString(R.string.group_separator_none)) && dec != sep;
        return NumberUtils.formatCalculator(value, decimals, dec, sep, grouping);
    }

    private void render() {
        if (calculatePage == null || first == null) return;
        result = null;
        graphyOutput = null;
        BigDecimal firstValue = decimalOrNull(first.getText().toString());
        BigDecimal secondValue = decimalOrNull(second.getText().toString());
        try {
            if (firstValue != null && secondValue != null) {
                result = PercentageCalculator.calculate(
                        new PercentageCalculator.Inputs(mode, firstValue, secondValue));
                String formatted = formatResult(result);
                graphyOutput = PercentageGraphBuilder.build(requireContext(), result, formatted);
            }
        } catch (IllegalArgumentException | ArithmeticException ignored) {
            result = null;
            graphyOutput = null;
        }
        var theme = ThemeManager.get();
        calculatePage.setBackgroundColor(theme.background());
        ThemeApplier.apply(calculatePage);
        refitAmountFields(calculatePage);
        TextView value = calculatePage.findViewById(R.id.percentage_result_value);
        TextView explain = calculatePage.findViewById(R.id.percentage_result_explain);
        if (result == null) {
            boolean empty = first.length() == 0 && second.length() == 0;
            value.setText(getString(R.string.percentage_no_result));
            value.setTextColor(theme.secondaryContentColor());
            explain.setText(empty ? "" : getString(R.string.percentage_invalid));
            explain.setTextColor(theme.secondaryContentColor());
            history.invalidate();
        } else {
            String formatted = formatResult(result);
            value.setText(formatted);
            value.setTextColor(theme.accentContentColor());
            explain.setText(explanation(result, formatted));
            explain.setTextColor(theme.primaryContentColor());
            String secondDisplay = mode == PercentageCalculator.Mode.WHAT_PERCENT
                    ? second.getText().toString()
                    : second.getText() + "%";
            history.update(
                    getString(R.string.percentage_history_input, modeHistoryName(), first.getText(), secondDisplay),
                    getString(R.string.percentage_history_value, formatted),
                    CalculatorCatalog.ID_PERCENTAGE,
                    currentHistoryState().toPayload());
        }
        if (graphy != null) graphy.update(graphyOutput);
        if (tabs != null && result == null && selectedTab != 0) tabs.selectTab(0);
    }

    private String formatResult(PercentageCalculator.Result current) {
        String number = formatNumber(current.result());
        return current.mode() == PercentageCalculator.Mode.WHAT_PERCENT ? number + "%" : number;
    }

    private String explanation(PercentageCalculator.Result current, String formatted) {
        String a = first.getText().toString();
        String b = second.getText().toString();
        return switch (current.mode()) {
            case PERCENT_OF -> getString(R.string.percentage_explain_of, b, a, formatted);
            case INCREASE -> getString(R.string.percentage_explain_increase, a, b, formatted);
            case DECREASE -> getString(R.string.percentage_explain_decrease, a, b, formatted);
            case WHAT_PERCENT -> getString(R.string.percentage_explain_what, a, b,
                    formatNumber(current.result()));
        };
    }

    private void refitAmountFields(View root) {
        if (root instanceof AutoSizeEditText amount) {
            amount.requestLayout();
        } else if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                refitAmountFields(group.getChildAt(i));
            }
        }
    }

    public void reset() {
        if (first == null) return;
        binding = true;
        mode = PercentageCalculator.Mode.PERCENT_OF;
        modeGroup.check(R.id.percentage_mode_of);
        first.setText("");
        second.setText("");
        binding = false;
        updateFieldLabels();
        selectField(0);
        closeKeypad();
        render();
        persist();
    }

    public void restoreFromHistory(@Nullable String expression, @Nullable String payload) {
        if (first == null) return;
        PercentageHistoryState state = PercentageHistoryState.fromPayload(payload);
        if (state == null) state = PercentageHistoryState.fromLegacyExpression(expression);
        if (state == null) return;
        applyHistoryState(state);
        history.markCurrentAsSaved();
    }

    private void applyHistoryState(PercentageHistoryState state) {
        binding = true;
        mode = state.mode;
        modeGroup.check(modeButtonId(mode));
        first.setText(state.first);
        second.setText(state.second);
        binding = false;
        updateFieldLabels();
        closeKeypad();
        selectField(state.focus);
        render();
        persist();
    }

    private PercentageHistoryState currentHistoryState() {
        return new PercentageHistoryState(mode, first.getText().toString(), second.getText().toString(), activeIndex);
    }

    private String snapshot() {
        if (first == null) return restored;
        try {
            return new JSONObject(currentHistoryState().toPayload())
                    .put("focus", activeIndex)
                    .toString();
        } catch (Exception ignored) {
            return currentHistoryState().toPayload();
        }
    }

    private void persist() {
        if (first != null) {
            restored = snapshot();
            Preferences.getInstance(requireContext()).setPercentageState(restored);
        }
    }

    @Override public void onResume() {
        super.onResume();
        render();
        if (history != null) history.visibilityChanged();
    }

    @Override public void onPause() {
        persist();
        super.onPause();
    }

    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (history != null) {
            persist();
            history.visibilityChanged();
        }
        updateKeypad();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("percentage", snapshot());
        out.putInt("tab", selectedTab);
        out.putBoolean("percentage_keypad_expanded", keypadExpanded);
        if (history != null) history.saveState(out);
    }

    @Override public void onDestroyView() {
        persist();
        ThemeManager.removeListener(themeListener);
        if (history != null) {
            history.invalidate();
            getLifecycle().removeObserver(history);
        }
        history = null;
        first = second = active = null;
        if (tabs != null) tabs.detach();
        super.onDestroyView();
    }
}
