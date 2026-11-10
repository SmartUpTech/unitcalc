package net.smartlogic.unitconverter.calculator.tools.fuel;

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
import com.google.android.material.materialswitch.MaterialSwitch;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.calculator.finance.emi.EmiFormatter;
import net.smartlogic.unitconverter.converter.ConverterPickerDialogFragment;
import net.smartlogic.unitconverter.graphy.compose.GraphyPanelController;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import net.smartlogic.unitconverter.helper.IdleHistoryRecorder;
import net.smartlogic.unitconverter.helper.InteractionFeedbackManager;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.helper.WorkspacePagerAdapter;
import net.smartlogic.unitconverter.helper.WorkspaceTabController;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.model.Unit;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;
import net.smartlogic.unitconverter.utils.Conversions;
import org.json.JSONObject;

public final class FuelEfficiencyCalculatorFragment extends Fragment {
    private static final int MAX_INPUT_CHARACTERS = 12;
    private static final int MAX_INPUT_FRACTION_DIGITS = 3;
    private static final int MAX_PASSENGER_CHARACTERS = 2;
    private static final int PICK_DISTANCE = 10;
    private static final int PICK_EFFICIENCY = 11;

    private EditText distance, efficiency, price, passengers, active;
    private View distanceUnitSelector, efficiencyUnitSelector;
    private TextView distanceUnitLabel, efficiencyUnitLabel, priceSymbol;
    private MaterialSwitch roundTrip;
    private int distanceUnitId = Unit.KILOMETRE;
    private int efficiencyUnitId = Unit.L_100K;
    private String currencyCode;
    private boolean binding;
    private int activeIndex;
    private boolean keypadExpanded;
    private FuelEfficiencyCalculator.Result result;
    private GraphyOutput graphyOutput;
    private IdleHistoryRecorder history;
    private View calculatePage;
    private ViewPager2 pager;
    private WorkspaceTabController tabs;
    private GraphyPanelController graphy;
    private int selectedTab;
    private String restored = "";
    private Bundle recorderState;
    private OnBackPressedCallback back;
    private final Conversions conversions = Conversions.getInstance();
    private final ThemeManager.Listener themeListener = theme -> {
        if (getView() != null) ThemeApplier.apply(getView());
        render();
    };

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        restored = state == null ? Preferences.getInstance(requireContext()).getFuelEfficiencyState() : state.getString("fuel", "");
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
        getChildFragmentManager().setFragmentResultListener(ConverterPickerDialogFragment.RESULT_KEY,
                getViewLifecycleOwner(), (key, bundle) -> {
                    int group = bundle.getInt(ConverterPickerDialogFragment.GROUP);
                    int index = bundle.getInt(ConverterPickerDialogFragment.INDEX);
                    if (group == PICK_DISTANCE) {
                        distanceUnitId = index == 0 ? Unit.KILOMETRE : Unit.MILE;
                    } else if (group == PICK_EFFICIENCY) {
                        efficiencyUnitId = fuelUnits().get(index).getId();
                    } else {
                        return;
                    }
                    updateUnits();
                    render();
                    persist();
                });
        pager = view.findViewById(R.id.workspace_pager);
        pager.setAdapter(new WorkspacePagerAdapter(
                new int[]{R.layout.pane_fuel_efficiency_calculate, R.layout.pane_calculator_graphy},
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
        distance = page.findViewById(R.id.fe_distance_input);
        efficiency = page.findViewById(R.id.fe_efficiency_input);
        price = page.findViewById(R.id.fe_price_input);
        passengers = page.findViewById(R.id.fe_passengers_input);
        distanceUnitSelector = page.findViewById(R.id.fe_distance_unit);
        efficiencyUnitSelector = page.findViewById(R.id.fe_efficiency_unit);
        distanceUnitLabel = page.findViewById(R.id.fe_distance_unit_label);
        efficiencyUnitLabel = page.findViewById(R.id.fe_efficiency_unit_label);
        priceSymbol = page.findViewById(R.id.fe_price_symbol);
        roundTrip = page.findViewById(R.id.fe_round_trip);
        binding = true;
        currencyCode = Preferences.getInstance(requireContext()).getPreferredCurrency();
        Bundle savedRecorder = recorderState == null ? new Bundle() : recorderState;
        try {
            JSONObject saved = new JSONObject(restored);
            FuelHistoryState draft = FuelHistoryState.fromDraft(restored);
            if (draft != null) {
                currencyCode = restoredCurrency(draft.currencyCode);
                distance.setText(draft.distance);
                efficiency.setText(draft.efficiency);
                price.setText(draft.price);
                passengers.setText(draft.passengers);
                distanceUnitId = draft.distanceUnitId;
                efficiencyUnitId = draft.efficiencyUnitId;
                roundTrip.setChecked(draft.roundTrip);
            }
            activeIndex = saved.optInt("focus", 0);
            keypadExpanded = recorderState != null && recorderState.getBoolean("fuel_keypad_expanded");
        } catch (Exception ignored) {
            passengers.setText("1");
        }
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
                if (field == passengers) {
                    if (end == start && next.matches("[0-9]*")) return null;
                    return next.length() <= MAX_PASSENGER_CHARACTERS && next.matches("[0-9]*") ? null : dest.subSequence(dstart, dend);
                }
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
        InteractionFeedbackManager.configure(distanceUnitSelector);
        distanceUnitSelector.setOnClickListener(v -> openDistancePicker());
        InteractionFeedbackManager.configure(efficiencyUnitSelector);
        efficiencyUnitSelector.setOnClickListener(v -> openEfficiencyPicker());
        roundTrip.setOnCheckedChangeListener((button, checked) -> {
            if (binding) return;
            render();
            persist();
        });
        updateUnits();
        selectField(activeIndex);
        updateKeypad();
        render();
    }

    private EditText[] fields() {
        return new EditText[]{distance, efficiency, price, passengers};
    }

    private List<Unit> fuelUnits() {
        return conversions.getById(Conversion.FUEL).getUnits();
    }

    private Unit distanceUnit() {
        return conversions.getById(Conversion.LENGTH).getUnits().stream()
                .filter(u -> u.getId() == distanceUnitId).findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    private Unit efficiencyUnit() {
        return fuelUnits().stream().filter(u -> u.getId() == efficiencyUnitId).findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    private void openDistancePicker() {
        ArrayList<String> labels = new ArrayList<>();
        labels.add(getString(R.string.kilometre));
        labels.add(getString(R.string.mile));
        int selected = distanceUnitId == Unit.MILE ? 1 : 0;
        ConverterPickerDialogFragment.forValues(getString(R.string.fuel_pick_distance), PICK_DISTANCE, selected, labels,
                R.string.explore_search_hint).showFor(this);
    }

    private void openEfficiencyPicker() {
        List<Unit> units = fuelUnits();
        ArrayList<String> labels = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            labels.add(getString(unit.getLabelResource()) + " (" + unit.getSymbol() + ")");
            if (unit.getId() == efficiencyUnitId) selected = i;
        }
        ConverterPickerDialogFragment.forValues(getString(R.string.fuel_pick_efficiency), PICK_EFFICIENCY, selected,
                labels, R.string.explore_search_hint).showFor(this);
    }

    private void updateUnits() {
        if (distanceUnitLabel != null) {
            distanceUnitLabel.setText(distanceUnit().getSymbol());
        }
        if (efficiencyUnitLabel != null) {
            efficiencyUnitLabel.setText(efficiencyUnitId == Unit.MPG_US ? getString(R.string.fuel_mpg_us)
                    : efficiencyUnitId == Unit.MPG_UK ? getString(R.string.fuel_mpg_uk)
                    : efficiencyUnit().getSymbol());
        }
        if (priceSymbol != null) {
            String code = currencyCode;
            priceSymbol.setText(new EmiFormatter(requireContext()).symbol(code));
        }
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
            if (active == passengers) return;
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
        View host = calculatePage.findViewById(R.id.fe_keypad_host);
        View results = calculatePage.findViewById(R.id.fe_results);
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
        if (text == null || text.isBlank()) return null;
        return new BigDecimal(text.trim());
    }

    private Integer passengersValue() {
        if (passengers == null || passengers.length() == 0) return null;
        try {
            return Integer.parseInt(passengers.getText().toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void render() {
        if (calculatePage == null || distance == null) return;
        updateUnits();
        result = null;
        graphyOutput = null;
        EmiFormatter money = new EmiFormatter(requireContext());
        String currency = currencyCode;
        try {
            BigDecimal distanceValue = decimalOrNull(distance.getText().toString());
            BigDecimal efficiencyValue = decimalOrNull(efficiency.getText().toString());
            BigDecimal priceValue = decimalOrNull(price.getText().toString());
            Integer passengerCount = passengersValue();
            if (distanceValue != null && efficiencyValue != null && priceValue != null && passengerCount != null) {
                result = FuelEfficiencyCalculator.calculate(
                        new FuelEfficiencyCalculator.Inputs(
                                distanceValue,
                                distanceUnit(),
                                efficiencyValue,
                                efficiencyUnit(),
                                priceValue,
                                currency,
                                passengerCount,
                                roundTrip.isChecked()),
                        conversions);
                graphyOutput = FuelEfficiencyGraphBuilder.build(requireContext(), result);
            }
        } catch (IllegalArgumentException | ArithmeticException ignored) {
        }
        var theme = ThemeManager.get();
        calculatePage.setBackgroundColor(theme.background());
        ThemeApplier.apply(calculatePage);
        refitAmountFields(calculatePage);
        TextView tripCost = calculatePage.findViewById(R.id.fe_trip_cost_value);
        TextView fuelNeeded = calculatePage.findViewById(R.id.fe_fuel_needed);
        TextView perPassenger = calculatePage.findViewById(R.id.fe_cost_per_passenger);
        if (result == null) {
            boolean empty = distance.length() == 0 && efficiency.length() == 0 && price.length() == 0;
            tripCost.setText(getString(R.string.fuel_no_result));
            tripCost.setTextColor(theme.secondaryContentColor());
            fuelNeeded.setText("");
            perPassenger.setText(empty ? "" : getString(R.string.fuel_invalid));
            perPassenger.setTextColor(theme.secondaryContentColor());
            history.invalidate();
        } else {
            String fuel = FuelEfficiencyGraphBuilder.formatVolume(requireContext(), result.fuelLitres());
            tripCost.setText(money.money(result.tripCost(), currency));
            tripCost.setTextColor(theme.accentContentColor());
            fuelNeeded.setText(getString(R.string.fuel_needed_label) + ": " + fuel);
            perPassenger.setText(getString(R.string.fuel_cost_per_passenger) + ": "
                    + money.money(result.costPerPassenger(), currency));
            perPassenger.setTextColor(theme.primaryContentColor());
            String expression = getString(R.string.fuel_history_input,
                    distance.getText(),
                    efficiency.getText() + " " + efficiencyUnit().getSymbol(),
                    money.money(result.inputs().pricePerLitre(), currency) + "/L",
                    result.inputs().passengers(),
                    result.inputs().roundTrip() ? getString(R.string.fuel_history_round_trip) : "");
            history.update(expression, getString(R.string.fuel_history_value, money.money(result.tripCost(), currency)),
                    CalculatorCatalog.ID_FUEL_EFFICIENCY, currentHistoryState().toPayload());
        }
        if (graphy != null) graphy.update(graphyOutput);
        if (tabs != null && result == null && selectedTab != 0) tabs.selectTab(0);
    }

    private void refitAmountFields(View root) {
        if (root instanceof net.smartlogic.unitconverter.widget.AutoSizeEditText amount) {
            amount.requestLayout();
        } else if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                refitAmountFields(group.getChildAt(i));
            }
        }
    }

    public void reset() {
        if (distance == null) return;
        binding = true;
        currencyCode = Preferences.getInstance(requireContext()).getPreferredCurrency();
        distance.setText("");
        efficiency.setText("");
        price.setText("");
        passengers.setText("1");
        roundTrip.setChecked(false);
        binding = false;
        selectField(0);
        render();
        persist();
    }

    public void restoreFromHistory(@Nullable String expression, @Nullable String payload) {
        if (distance == null) return;
        FuelHistoryState state = FuelHistoryState.fromPayload(payload);
        if (state == null) state = FuelHistoryState.fromLegacyExpression(expression);
        if (state == null) return;
        applyHistoryState(state, 0);
        history.markCurrentAsSaved();
    }

    private void applyHistoryState(FuelHistoryState state, int focusIndex) {
        binding = true;
        currencyCode = restoredCurrency(state.currencyCode);
        distance.setText(state.distance);
        efficiency.setText(state.efficiency);
        price.setText(state.price);
        passengers.setText(state.passengers);
        distanceUnitId = state.distanceUnitId == Unit.MILE ? Unit.MILE : Unit.KILOMETRE;
        efficiencyUnitId = fuelUnits().stream().anyMatch(unit -> unit.getId() == state.efficiencyUnitId)
                ? state.efficiencyUnitId : Unit.L_100K;
        roundTrip.setChecked(state.roundTrip);
        binding = false;
        updateUnits();
        closeKeypad();
        selectField(focusIndex);
        render();
        persist();
    }

    private String restoredCurrency(String code) {
        if (code != null) {
            try {
                new EmiFormatter(requireContext()).symbol(code);
                return code;
            } catch (IllegalArgumentException ignored) { }
        }
        // Older records did not retain currency identity.
        return Preferences.getInstance(requireContext()).getPreferredCurrency();
    }

    private FuelHistoryState currentHistoryState() {
        return new FuelHistoryState(
                distance.getText().toString(),
                efficiency.getText().toString(),
                price.getText().toString(),
                passengers.getText().toString(),
                distanceUnitId,
                efficiencyUnitId,
                roundTrip.isChecked(), currencyCode);
    }

    private String snapshot() {
        if (distance == null) return restored;
        try {
            return currentHistoryState().toJson()
                    .put("focus", activeIndex)
                    .toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void persist() {
        if (distance != null) {
            restored = snapshot();
            Preferences.getInstance(requireContext()).setFuelEfficiencyState(restored);
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
        out.putString("fuel", snapshot());
        out.putInt("tab", selectedTab);
        out.putBoolean("fuel_keypad_expanded", keypadExpanded);
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
        distance = efficiency = price = passengers = active = null;
        if (tabs != null) tabs.detach();
        super.onDestroyView();
    }
}
