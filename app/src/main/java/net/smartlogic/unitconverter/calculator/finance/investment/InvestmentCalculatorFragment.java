package net.smartlogic.unitconverter.calculator.finance.investment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.tabs.TabLayout;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.calculator.tools.age.AgeDialogFragment;
import net.smartlogic.unitconverter.calculator.finance.emi.EmiFieldsLayout;
import net.smartlogic.unitconverter.calculator.finance.emi.EmiLayout;
import net.smartlogic.unitconverter.calculator.finance.emi.EmiDialogFragment;
import net.smartlogic.unitconverter.calculator.finance.emi.EmiFormatter;
import net.smartlogic.unitconverter.graphy.compose.GraphyPanelController;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import net.smartlogic.unitconverter.helper.*;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;
import net.smartlogic.unitconverter.widget.FixedTableView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/** Three independent destinations sharing one fixed-screen investment workspace. */
public final class InvestmentCalculatorFragment extends Fragment {
    private static final ExecutorService WORK = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(3), new ThreadPoolExecutor.DiscardOldestPolicy());
    private final Handler main = new Handler(Looper.getMainLooper());
    private EmiFormatter display;
    private InvestmentDraft draft;
    private InvestmentResult result;
    private Future<?> calculation;
    private int revision, epoch, focus, tab, pageStart;
    private int[] carets = {-1,-1,-1,-1,-1,-1,-1,-1};
    private boolean binding, keysOpen, yearly, restoring;
    private String error;
    private View form, schedule;
    private EditText[] editors;
    private WorkspaceTabController tabs;
    private GraphyPanelController graphy;
    private FixedTableView table;
    private IdleHistoryRecorder history;
    private final Runnable calculateLater = this::calculate;
    private final ThemeManager.Listener themeListener = theme -> {
        if (getView() != null) { ThemeApplier.apply(requireView()); render(); renderSchedule(); }
    };
    private final OnBackPressedCallback back = new OnBackPressedCallback(false) {
        @Override public void handleOnBackPressed() { handleBack(); }
    };

    public static InvestmentCalculatorFragment create(InvestmentInputs.Kind kind) {
        InvestmentCalculatorFragment f = new InvestmentCalculatorFragment();
        Bundle args = new Bundle(); args.putString("kind", kind.name()); f.setArguments(args); return f;
    }
    public InvestmentInputs.Kind kind() { return InvestmentInputs.Kind.valueOf(requireArguments().getString("kind")); }
    public static String toolId(InvestmentInputs.Kind kind) {
        return kind == InvestmentInputs.Kind.FD ? CalculatorCatalog.ID_FD : kind == InvestmentInputs.Kind.RD ? CalculatorCatalog.ID_RD : CalculatorCatalog.ID_SIP;
    }
    public static int title(InvestmentInputs.Kind kind) {
        return kind == InvestmentInputs.Kind.FD ? R.string.fd_calculator : kind == InvestmentInputs.Kind.RD ? R.string.rd_calculator : R.string.sip_calculator;
    }
    public static int amountLabel(InvestmentInputs.Kind kind) {
        return kind == InvestmentInputs.Kind.FD ? R.string.emi_principal : kind == InvestmentInputs.Kind.RD ? R.string.investment_deposit : R.string.investment_monthly;
    }
    public static int frequencyLabel(int months) {
        return months == 1 ? R.string.investment_monthly_frequency : months == 3 ? R.string.investment_quarterly
                : months == 6 ? R.string.investment_half_yearly : R.string.investment_yearly;
    }
    private String storage() { return toolId(kind()) + "_workspace_v1"; }
    @Override public void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        draft = fresh();
        String raw = saved == null ? Preferences.getInstance(requireContext()).getPreferences().getString(storage(), null) : saved.getString("draft");
        if (raw != null) {
            try { draft = read(raw); } catch (RuntimeException invalid) { error = "state"; }
        }
        if (saved != null) {
            keysOpen = saved.getBoolean("keys"); focus = saved.getInt("focus"); tab = saved.getInt("tab");
            pageStart = saved.getInt("page"); yearly = saved.getBoolean("yearly");
            int[] restored = saved.getIntArray("carets"); if (restored != null && restored.length == 8) carets = restored;
        }
    }
    private InvestmentDraft fresh() { return new InvestmentDraft(kind(), Preferences.getInstance(requireContext()).getPreferredCurrency()); }
    private InvestmentDraft read(String raw) {
        InvestmentDraft restored = InvestmentDraft.read(raw, kind()); PreferredCurrency.find(requireContext(), restored.currency); return restored;
    }
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle saved) {
        return inflater.inflate(R.layout.fragment_investment_calculator, parent, false);
    }
    @Override public void onViewCreated(@NonNull View root, @Nullable Bundle saved) {
        epoch++; history = new IdleHistoryRecorder(this, saved);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), back);
        getChildFragmentManager().setFragmentResultListener(EmiDialogFragment.RESULT, getViewLifecycleOwner(), (key, data) -> choice(data.getInt("action")));
        getChildFragmentManager().setFragmentResultListener(AgeDialogFragment.RESULT, getViewLifecycleOwner(), (key, data) -> {
            if ("investment_date".equals(data.getString("mode"))) {
                LocalDate date = LocalDate.parse(data.getString("date"));
                if (date.getYear() >= 1900 && date.getYear() <= 2100) { draft.start = date; bindOptions(); changed(); }
            }
        });
        ViewPager2 pager = root.findViewById(R.id.workspace_pager); pager.setOffscreenPageLimit(2);
        pager.setAdapter(new WorkspacePagerAdapter(new int[]{R.layout.pane_investment_calculate, R.layout.pane_emi_section, R.layout.pane_investment_graphy}, (position, page) -> {
            if (position == 0) { form = page; bindForm(); }
            else if (position == 1) { schedule = page; renderSchedule(); }
            else { graphy = new GraphyPanelController(page.findViewById(R.id.graphy_panel), this, new GraphyViewTheme(requireContext())); render(); }
            ThemeApplier.apply(page);
        }));
        int restoredTab = tab;
        tabs = new WorkspaceTabController(pager, root.findViewById(R.id.workspace_tabs), new int[]{R.string.tab_calculate, R.string.emi_schedule, R.string.graphy}, new WorkspaceTabController.Callback() {
            @Override public void onTabSelected(int position) { if (position != 0) setKeys(false); tab = position; updateBack(); }
            @Override public boolean isTabEnabled(int position) { return position == 0 || result != null; }
        });
        tab = restoredTab;
        ThemeManager.addListener(themeListener); ThemeApplier.apply(root); changed();
    }
    private void bindForm() {
        ((EmiLayout)form.findViewById(R.id.emi_layout)).setResultRowSpacing(
                getResources().getDimensionPixelSize(R.dimen.converter_spacing), getResources().getDimensionPixelSize(R.dimen.converter_small_spacing));
        View keypad = form.findViewById(R.id.keypad);
        keypad.setPadding(keypad.getPaddingLeft(), 0, keypad.getPaddingRight(), 0);
        binding = true;
        boolean sip = kind() == InvestmentInputs.Kind.SIP, fd = kind() == InvestmentInputs.Kind.FD;
        form.findViewById(R.id.investment_date_row).setVisibility(sip ? View.GONE : View.VISIBLE);
        form.findViewById(R.id.investment_step_frequency_row).setVisibility(sip ? View.VISIBLE : View.GONE);
        form.findViewById(R.id.investment_step_row).setVisibility(sip ? View.VISIBLE : View.GONE);
        form.findViewById(R.id.investment_fd_row).setVisibility(fd ? View.VISIBLE : View.GONE);
        var fields = (EmiFieldsLayout)form.findViewById(R.id.emi_fields);
        String amountCaption = getString(R.string.emi_currency_choice, getString(amountLabel(kind())), formatter().symbol(draft.currency));
        fields.setLabelAlternatives(R.id.emi_principal_label, amountCaption, kind() == InvestmentInputs.Kind.FD ? amountCaption : getString(R.string.investment_monthly_short, formatter().symbol(draft.currency)));
        fields.setLabelAlternatives(R.id.emi_rate_label, getString(kind() == InvestmentInputs.Kind.SIP ? R.string.investment_return : R.string.emi_annual_rate),
                getString(kind() == InvestmentInputs.Kind.SIP ? R.string.investment_return_short : R.string.investment_rate_short));
        text(R.id.emi_rate_label, getString(sip ? R.string.investment_return : R.string.emi_annual_rate));
        text(R.id.emi_date_label, getString(R.string.investment_start));
        editors = new EditText[]{form.findViewById(R.id.emi_principal), form.findViewById(R.id.emi_rate), form.findViewById(R.id.emi_tenure), form.findViewById(R.id.investment_step_value)};
        String[] values = {draft.amount, draft.rate, draft.tenure, draft.stepText()};
        int[] labels = {amountLabel(kind()), sip ? R.string.investment_return : R.string.emi_annual_rate, R.string.emi_tenure, R.string.investment_step};
        for (int n = 0; n < editors.length; n++) {
            int index = n; EditText field = editors[n]; field.setText(values[n]); field.setShowSoftInputOnFocus(false);
            field.setContentDescription(getString(labels[n]));
            field.setSelection(carets[n*2] < 0 ? field.length() : Math.min(field.length(), Math.max(0,carets[n*2])),
                    carets[n*2+1] < 0 ? field.length() : Math.min(field.length(), Math.max(0,carets[n*2+1])));
            field.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
                if (binding) return null;
                String next = dest.subSequence(0,dstart).toString() + source.subSequence(start,end) + dest.subSequence(dend,dest.length());
                if (acceptEdit(index, next)) return null;
                toast(R.string.investment_input_rejected); return dest.subSequence(dstart,dend);
            }});
            field.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
                public void onTextChanged(CharSequence s,int start,int before,int count) { }
                public void afterTextChanged(Editable value) {
                    if (binding) return;
                    switch (index) {
                        case 0 -> draft.amount = value.toString(); case 1 -> draft.rate = value.toString(); case 2 -> draft.tenure = value.toString();
                        case 3 -> { if (draft.percent) draft.stepPercent = value.toString(); else draft.stepAmount = value.toString(); }
                    }
                    changed();
                }
            });
            field.setOnFocusChangeListener((v, has) -> { if (has) { focus = index; updateSignKey(); } });
            field.setOnClickListener(v -> { focus = index; setKeys(true); });
            field.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == android.view.MotionEvent.ACTION_UP) {
                    v.post(() -> {
                        if (getView() != null) { focus = index; field.requestFocus(); setKeys(true); }
                    });
                }
                return false;
            });
        }
        form.findViewById(R.id.emi_units).setOnClickListener(v -> show(EmiDialogFragment.choices(R.string.emi_tenure, R.string.emi_years, R.string.emi_months)));
        form.findViewById(R.id.emi_date).setOnClickListener(v -> {
            if (!getChildFragmentManager().isStateSaved() && getChildFragmentManager().findFragmentByTag(AgeDialogFragment.TAG) == null)
                AgeDialogFragment.forDate("investment_date", draft.start, R.string.investment_start).show(getChildFragmentManager(), AgeDialogFragment.TAG);
        });
        MaterialButton stepFrequency = form.findViewById(R.id.investment_step_frequency);
        stepFrequency.setIconResource(R.drawable.ic_chevron_down);
        stepFrequency.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_END);
        stepFrequency.setIconSize(getResources().getDimensionPixelSize(R.dimen.converter_chevron_size));
        stepFrequency.setIconPadding(getResources().getDimensionPixelSize(R.dimen.converter_small_spacing));
        form.findViewById(R.id.investment_step_frequency).setOnClickListener(v -> show(EmiDialogFragment.choices(R.string.investment_step_frequency,
                R.string.investment_three_months, R.string.investment_six_months, R.string.investment_twelve_months)));
        ((SwitchCompat) form.findViewById(R.id.investment_step_switch)).setOnCheckedChangeListener((v, on) -> {
            if (!binding) { draft.stepEnabled = on; if (!on && focus == 3) { focus = 0; editors[0].requestFocus(); } bindOptions(); changed(); }
        });
        ((MaterialButtonToggleGroup) form.findViewById(R.id.investment_step_modes)).addOnButtonCheckedListener((group, id, checked) -> {
            if (!binding && checked) { saveCarets(); draft.percent = id == R.id.investment_percent_mode; binding = true;
                editors[3].setText(draft.stepText()); editors[3].setSelection(editors[3].length()); binding = false; changed(); }
        });
        form.findViewById(R.id.investment_payout_mode).setOnClickListener(v -> show(EmiDialogFragment.choices(R.string.investment_payout_mode, R.string.investment_cumulative, R.string.investment_periodic)));
        form.findViewById(R.id.investment_frequency).setOnClickListener(v -> show(EmiDialogFragment.choices(draft.periodic ? R.string.investment_payout_frequency : R.string.investment_compounding,
                R.string.investment_monthly_frequency, R.string.investment_quarterly, R.string.investment_half_yearly, R.string.investment_yearly)));
        text(R.id.emi_insights, getString(R.string.investment_reset));
        text(R.id.emi_more, getString(R.string.investment_details));
        form.findViewById(R.id.emi_insights).setOnClickListener(v -> show(EmiDialogFragment.confirm(R.string.investment_reset, getString(R.string.investment_reset_confirm), R.string.investment_reset)));
        form.findViewById(R.id.emi_more).setOnClickListener(v -> details());
        View keys = form.findViewById(R.id.emi_keys), minus = keys.findViewById(R.id.minus);
        ViewGroup row = (ViewGroup) minus.getParent(); int position = row.indexOfChild(minus); ViewGroup.LayoutParams lp = minus.getLayoutParams(); row.removeView(minus);
        View done = getLayoutInflater().inflate(R.layout.bmi_keypad_done, row, false); done.setContentDescription(getString(R.string.emi_done)); row.addView(done, position, lp);
        ((ImageView)keys.findViewById(R.id.copy)).setImageResource(R.drawable.ic_arrow_forward); keys.findViewById(R.id.copy).setContentDescription(getString(R.string.emi_next_field));
        for (int id : new int[]{R.id.zero,R.id.one,R.id.two,R.id.three,R.id.four,R.id.five,R.id.six,R.id.seven,R.id.eight,R.id.nine,R.id.double_zero,R.id.dot,R.id.ac,R.id.backspace,R.id.copy,R.id.equal}) {
            View key = keys.findViewById(id); InteractionFeedbackManager.configure(key);
            key.setOnClickListener(v -> { InteractionFeedbackManager.perform(v, InteractionFeedbackManager.Type.INPUT); keypad(id); });
        }
        binding = false; bindOptions(); setKeys(keysOpen); render(); ThemeApplier.apply(form);
    }
    private boolean acceptEdit(int index, String next) {
        int scale = index == 1 || index == 3 && draft.percent ? 4 : index == 2 ? 6 : java.util.Currency.getInstance(draft.currency).getDefaultFractionDigits();
        boolean signed = index == 1 && kind() == InvestmentInputs.Kind.SIP;
        if (next.length() > 20 || !next.matches((signed ? "-?" : "") + "[0-9]{0,12}" + (scale > 0 ? "(?:\\.[0-9]{0,"+scale+"})?" : ""))) return false;
        if (next.isEmpty() || next.equals("-") || next.equals(".") || next.equals("-.")) return true;
        BigDecimal value;
        try { value = new BigDecimal(next); } catch (NumberFormatException invalid) { return false; }
        if (index == 1) return value.compareTo(new BigDecimal("100")) <= 0 && value.compareTo(new BigDecimal(signed ? "-99.99" : "0")) >= 0;
        if (index == 2) return value.multiply(BigDecimal.valueOf(draft.years ? 12 : 1)).compareTo(BigDecimal.valueOf(600)) <= 0;
        if (index == 3 && draft.percent) return value.compareTo(new BigDecimal("100")) <= 0;
        return value.compareTo(InvestmentCalculator.MAX_AMOUNT) < 0;
    }
    private void bindOptions() {
        if (form == null) return; boolean previous = binding; binding = true;
        var fields = (EmiFieldsLayout)form.findViewById(R.id.emi_fields);
        String amountCaption = getString(R.string.emi_currency_choice, getString(amountLabel(kind())), formatter().symbol(draft.currency));
        fields.setLabelAlternatives(R.id.emi_principal_label, amountCaption, kind() == InvestmentInputs.Kind.FD ? amountCaption : getString(R.string.investment_monthly_short, formatter().symbol(draft.currency)));
        fields.setLabelAlternatives(R.id.emi_rate_label, getString(kind() == InvestmentInputs.Kind.SIP ? R.string.investment_return : R.string.emi_annual_rate),
                getString(kind() == InvestmentInputs.Kind.SIP ? R.string.investment_return_short : R.string.investment_rate_short));
        text(R.id.emi_units, getString(draft.years ? R.string.emi_years : R.string.emi_months));
        text(R.id.emi_date, formatter().date(draft.start));
        text(R.id.investment_step_frequency, getString(draft.stepMonths == 3 ? R.string.investment_three_months : draft.stepMonths == 6 ? R.string.investment_six_months : R.string.investment_twelve_months));
        ((SwitchCompat)form.findViewById(R.id.investment_step_switch)).setChecked(draft.stepEnabled);
        for (int id : new int[]{R.id.investment_step_frequency,R.id.investment_step_value,R.id.investment_amount_mode,R.id.investment_percent_mode}) form.findViewById(id).setEnabled(draft.stepEnabled);
        text(R.id.investment_amount_mode, formatter().symbol(draft.currency));
        ((MaterialButtonToggleGroup)form.findViewById(R.id.investment_step_modes)).check(draft.percent ? R.id.investment_percent_mode : R.id.investment_amount_mode);
        text(R.id.investment_payout_mode, getString(draft.periodic ? R.string.investment_periodic : R.string.investment_cumulative));
        text(R.id.investment_frequency, getString(frequencyLabel(draft.frequency)));
        form.findViewById(R.id.investment_frequency).setContentDescription(getString(draft.periodic ? R.string.investment_payout_frequency : R.string.investment_compounding) + ": " + getString(frequencyLabel(draft.frequency)));
        binding = previous;
    }
    private void choice(int action) {
        if (action == R.string.investment_reset) { draft = fresh(); pageStart = tab = focus = 0; yearly = false; carets = new int[]{-1,-1,-1,-1,-1,-1,-1,-1};
            if (form != null) { binding = true; String[] values={draft.amount,draft.rate,draft.tenure,draft.stepText()}; for (int n=0;n<4;n++) editors[n].setText(values[n]); binding = false; }
            if (tabs != null) tabs.selectTab(0); setKeys(false);
        } else if (action == R.string.emi_years || action == R.string.emi_months) {
            boolean years = action == R.string.emi_years;
            if (years != draft.years) {
                try {
                    String converted = "";
                    if (!draft.tenure.isEmpty()) {
                        int months = new BigDecimal(draft.tenure).multiply(BigDecimal.valueOf(draft.years ? 12 : 1)).intValueExact();
                        if (months < 1 || months > 600) throw new ArithmeticException();
                        converted = years ? new BigDecimal(months).divide(new BigDecimal("12")).stripTrailingZeros().toPlainString() : Integer.toString(months);
                    }
                    draft.years = years; draft.tenure = converted; binding = true; editors[2].setText(converted); binding = false;
                } catch (ArithmeticException invalid) { toast(R.string.emi_switch_units_error); return; }
            }
        } else if (action == R.string.investment_three_months) draft.stepMonths=3;
        else if (action == R.string.investment_six_months) draft.stepMonths=6;
        else if (action == R.string.investment_twelve_months) draft.stepMonths=12;
        else if (action == R.string.investment_monthly_frequency) draft.frequency=1;
        else if (action == R.string.investment_quarterly) draft.frequency=3;
        else if (action == R.string.investment_half_yearly) draft.frequency=6;
        else if (action == R.string.investment_yearly) draft.frequency=12;
        else if (action == R.string.investment_cumulative) draft.periodic=false;
        else if (action == R.string.investment_periodic) draft.periodic=true;
        bindOptions(); changed();
    }
    private void updateSignKey() {
        if (form == null) return;
        TextView key = form.findViewById(R.id.double_zero);
        boolean sign = kind() == InvestmentInputs.Kind.SIP && focus == 1;
        key.setText(sign ? getString(R.string.investment_sign) : getString(R.string.double_zero));
        key.setContentDescription(sign ? getString(R.string.investment_change_sign) : key.getText());
    }
    private void keypad(int id) {
        if (id == R.id.equal) { setKeys(false); return; }
        if (id == R.id.copy) { focus = (focus + 1) % (kind() == InvestmentInputs.Kind.SIP && draft.stepEnabled ? 4 : 3); editors[focus].requestFocus(); return; }
        EditText field = editors[Math.max(0,Math.min(3,focus))]; if (!field.isEnabled()) return;
        int start=Math.max(0,field.getSelectionStart()), end=Math.max(start,field.getSelectionEnd());
        if (id == R.id.ac) { field.getText().clear(); return; }
        if (id == R.id.backspace) { if (start==end && start>0) start--; field.getText().delete(start,end); return; }
        if (id == R.id.double_zero && kind() == InvestmentInputs.Kind.SIP && focus == 1) {
            if (field.getText().toString().startsWith("-")) field.getText().delete(0,1); else field.getText().insert(0,"-"); return;
        }
        String value = id==R.id.dot ? "." : "00";
        int[] ids={R.id.zero,R.id.one,R.id.two,R.id.three,R.id.four,R.id.five,R.id.six,R.id.seven,R.id.eight,R.id.nine};
        for(int n=0;n<ids.length;n++) if(id==ids[n]) value=Integer.toString(n);
        field.getText().replace(start,end,value);
    }
    private void setKeys(boolean open) {
        saveCarets(); keysOpen=open;
        if(form!=null) {
            form.findViewById(R.id.emi_keys).setVisibility(open?View.VISIBLE:View.GONE);
            form.findViewById(R.id.emi_actions).setVisibility(open?View.GONE:View.VISIBLE);
            if (open) editors[Math.max(0,Math.min(focus,editors.length-1))].requestFocus(); else for(EditText e:editors) e.clearFocus();
            updateSignKey();
            form.findViewById(R.id.emi_message).setVisibility(open && result != null ? View.GONE : View.VISIBLE);
            render();
        }
        updateBack();
    }
    private void changed() {
        revision++; if(calculation!=null) calculation.cancel(true);
        result=null; error=null; if(history!=null) history.invalidate();
        main.removeCallbacks(calculateLater); main.postDelayed(calculateLater,120);
        render(); renderSchedule(); persist();
    }
    private void calculate() {
        if(getView()==null) return;
        InvestmentInputs input;
        try { input=draft.inputs(); InvestmentCalculator.validate(input); }
        catch(RuntimeException invalid) { error=invalid.getMessage(); render(); return; }
        int request=revision, viewEpoch=epoch; String payload=draft.json();
        calculation=WORK.submit(() -> {
            InvestmentResult computed=null; String failure=null;
            try { computed=InvestmentCalculator.calculate(input); }
            catch(CancellationException cancelled) { return; }
            catch(IllegalArgumentException invalid) { failure=invalid.getMessage(); }
            InvestmentResult value=computed; String invalid=failure;
            main.post(() -> {
                if(getView()==null || viewEpoch!=epoch || revision!=request) return;
                result=value; error=invalid; render(); renderSchedule();
                if(result!=null) {
                    history.update(getString(R.string.investment_history, formatter().money(input.amount(),input.currency()), input.annualRate().toPlainString(),input.months()),
                            getString(R.string.investment_metric,getString(kind()==InvestmentInputs.Kind.SIP?R.string.investment_future:R.string.investment_maturity),formatter().money(result.maturity(),input.currency())),toolId(kind()),payload);
                    if(restoring) { history.markCurrentAsSaved(); restoring=false; }
                    if(tabs!=null && tab!=0) tabs.selectTab(tab);
                }
            });
        });
    }
    private int errorLabel() {
        return switch (error == null ? "" : error) {
            case "state" -> R.string.investment_restore_error;
            case "amount" -> R.string.investment_amount_error;
            case "rate" -> kind() == InvestmentInputs.Kind.SIP
                    ? R.string.investment_sip_rate_error : R.string.investment_rate_error;
            case "tenure" -> R.string.investment_tenure_error;
            case "step" -> R.string.investment_step_error;
            case "overflow" -> R.string.investment_overflow_error;
            default -> R.string.investment_calculation_error;
        };
    }
    private void render() {
        if(getView()==null) return;
        requireView().setBackgroundColor(ThemeManager.get().background());
        if(form!=null) {
            text(R.id.emi_value_label,getString(kind()==InvestmentInputs.Kind.SIP?R.string.investment_future:R.string.investment_maturity));
            text(R.id.emi_value,result==null?getString(R.string.emi_no_result):money(result.maturity()));
            if(kind()==InvestmentInputs.Kind.FD) {
                metric(R.id.emi_total_interest,R.string.investment_interest,result==null?null:money(result.interest()));
                metric(R.id.emi_total_payment,draft.periodic?R.string.investment_reinvestment_yield_short:R.string.investment_yield,result==null?null:getString(R.string.emi_rate_value,
                        net.smartlogic.unitconverter.utils.NumberUtils.formatCalculator(result.annualYield(),2)));
            } else {
                metric(R.id.emi_total_interest,kind()==InvestmentInputs.Kind.SIP?R.string.investment_invested:R.string.investment_total_deposits,result==null?null:money(result.invested()));
                metric(R.id.emi_total_payment,kind()==InvestmentInputs.Kind.SIP?R.string.investment_gains:R.string.investment_interest,result==null?null:money(result.interest()));
            }
            form.findViewById(R.id.investment_receipts).setVisibility(draft.periodic?View.VISIBLE:View.GONE);
            metric(R.id.investment_receipts,R.string.investment_receipts,result==null?null:money(result.receipts()));
            int notice = kind()==InvestmentInputs.Kind.SIP?R.string.investment_sip_notice:kind()==InvestmentInputs.Kind.RD?R.string.investment_rd_notice:draft.periodic?R.string.investment_periodic_notice:R.string.investment_estimate;
            boolean empty=draft.amount.isEmpty() || draft.rate.isEmpty() || draft.tenure.isEmpty();
            String fullMessage = getString(result!=null?notice:empty?R.string.investment_empty:error==null?R.string.emi_loading:errorLabel());
            text(R.id.emi_message, keysOpen && result == null ? getString(empty ? R.string.investment_complete_short
                    : "overflow".equals(error) ? R.string.investment_overflow_short : R.string.investment_check_short) : fullMessage);
            form.findViewById(R.id.emi_message).setContentDescription(fullMessage);
            form.findViewById(R.id.emi_message).setVisibility(keysOpen && result != null ? View.GONE : View.VISIBLE);
            // Errors stay associated with their field, without replacing entered text.
            for(int n=0;n<editors.length;n++) {
                String field=new String[]{"amount","rate","tenure","step"}[n];
                editors[n].setError(!empty && field.equals(error)?getString(errorLabel()):null);
            }
        }
        if(graphy!=null) graphy.update(result==null?null:InvestmentGraphBuilder.build(requireContext(),result));
        TabLayout bar=requireView().findViewById(R.id.workspace_tabs);
        for(int n=1;n<3;n++) { var item=bar.getTabAt(n); if(item!=null) { item.view.setEnabled(result!=null); item.view.setAlpha(result==null?.5f:1f); } }
        updateBack();
    }
    private void renderSchedule() {
        if(schedule==null) return;
        LinearLayout header=schedule.findViewById(R.id.emi_header), footer=schedule.findViewById(R.id.emi_footer);
        FrameLayout body=schedule.findViewById(R.id.emi_body); header.removeAllViews(); footer.removeAllViews(); body.removeAllViews(); table=null;
        if(result==null) return;
        InvestmentResult snapshot=result;
        LinearLayout modes=row(header); button(modes,R.string.emi_monthly_table,()->{yearly=false;pageStart=0;renderSchedule();}); button(modes,R.string.emi_yearly_table,()->{yearly=true;pageStart=0;renderSchedule();});
        var periods=periods(snapshot,yearly); List<List<String>> cells=new ArrayList<>();
        for(var p:periods) {
            String label=yearly?getString(R.string.investment_year,(p.month()-1)/12+1):formatter().date(p.date());
            cells.add(java.util.Arrays.asList(label,money(kind()==InvestmentInputs.Kind.FD?p.interest():p.contribution()),money(draft.periodic?p.payout():p.balance())));
        }
        table=new FixedTableView(requireContext()); FixedTableView current=table;
        TextView range=(TextView)getLayoutInflater().inflate(R.layout.emi_label,footer,false); footer.addView(range);
        LinearLayout navigation=row(footer); MaterialButton previous=button(navigation,R.string.emi_previous,current::previous), next=button(navigation,R.string.emi_next,current::next);
        current.setData(java.util.Arrays.asList(getString(R.string.emi_period),getString(kind()==InvestmentInputs.Kind.FD?R.string.investment_interest:R.string.investment_contribution),getString(draft.periodic?R.string.investment_payout:R.string.investment_balance)),cells,pageStart,index->{
            var p=periods.get(index); show(EmiDialogFragment.info(R.string.emi_schedule,getString(R.string.investment_period_detail,
                    formatter().date(p.date()),money(p.contribution()),money(p.invested()),money(p.interest()),money(p.payout()),money(p.balance()))));
        },()->{if(table!=current)return;pageStart=current.start();range.setText(getString(R.string.emi_page_range,current.start()+1,current.end(),current.count()));previous.setEnabled(current.start()>0);next.setEnabled(current.end()<current.count());});
        body.addView(current,new FrameLayout.LayoutParams(-1,-1));ThemeApplier.apply(schedule);
    }
    static List<InvestmentPeriod> periods(InvestmentResult r,boolean yearly) {
        if(!yearly)return r.periods();List<InvestmentPeriod> rows=new ArrayList<>();
        for(int n=0;n<r.periods().size();n+=12) {
            BigDecimal contribution=BigDecimal.ZERO,interest=BigDecimal.ZERO,payout=BigDecimal.ZERO;
            int end=Math.min(n+12,r.periods().size());
            for(int m=n;m<end;m++){var p=r.periods().get(m);contribution=contribution.add(p.contribution());interest=interest.add(p.interest());payout=payout.add(p.payout());}
            var last=r.periods().get(end-1);rows.add(new InvestmentPeriod(last.month(),last.date(),contribution,last.invested(),interest,payout,last.balance()));
        }
        return rows;
    }
    private void details() {
        String detail=getString(R.string.investment_assumptions)+"\n\n"+getString(kind()==InvestmentInputs.Kind.SIP?R.string.investment_sip_assumptions:kind()==InvestmentInputs.Kind.RD?R.string.investment_rd_assumptions:R.string.investment_fd_assumptions);
        show(EmiDialogFragment.info(title(kind()),detail));
    }
    public void restoreFromHistory(String payload) {
        try {
            InvestmentDraft restored=read(payload); InvestmentCalculator.validate(restored.inputs());
            draft=restored; restoring=true;tab=pageStart=focus=0;
            if(form!=null){binding=true;String[] values={draft.amount,draft.rate,draft.tenure,draft.stepText()};for(int n=0;n<4;n++)editors[n].setText(values[n]);binding=false;bindOptions();}
            setKeys(false);if(tabs!=null)tabs.selectTab(0);changed();
        } catch(RuntimeException invalid){toast(R.string.investment_restore_error);}
    }
    public boolean handleBack(){if(keysOpen){setKeys(false);return true;}if(tab!=0&&tabs!=null){tabs.selectTab(0);return true;}return false;}
    private void updateBack(){back.setEnabled(isResumed()&&!isHidden()&&(keysOpen||tab!=0));}
    private void saveCarets(){if(editors!=null)for(int n=0;n<4;n++){carets[n*2]=editors[n].getSelectionStart();carets[n*2+1]=editors[n].getSelectionEnd();}}
    private void persist(){if(draft!=null&&isAdded())Preferences.getInstance(requireContext()).getPreferences().edit().putString(storage(),draft.json()).apply();}
    private void text(int id,String value){((TextView)form.findViewById(id)).setText(value);}
    private void metric(int id,int label,String value){text(id,getString(R.string.investment_metric,getString(label),value==null?getString(R.string.emi_no_result):value));}
    private EmiFormatter formatter(){if(display==null)display=new EmiFormatter(requireContext());return display;}
    private String money(BigDecimal value){return formatter().money(value,draft.currency);}
    private void toast(int message){if(isAdded())Toast.makeText(requireContext(),message,Toast.LENGTH_SHORT).show();}
    private void show(EmiDialogFragment dialog){if(!getChildFragmentManager().isStateSaved()&&getChildFragmentManager().findFragmentByTag(EmiDialogFragment.TAG)==null)dialog.showNow(getChildFragmentManager(),EmiDialogFragment.TAG);}
    private LinearLayout row(LinearLayout parent){LinearLayout row=new LinearLayout(requireContext());parent.addView(row,new LinearLayout.LayoutParams(-1,-2));return row;}
    private MaterialButton button(LinearLayout parent,int label,Runnable action){MaterialButton b=(MaterialButton)getLayoutInflater().inflate(R.layout.emi_action,parent,false);b.setText(label);b.setOnClickListener(v->action.run());parent.addView(b,new LinearLayout.LayoutParams(0,-2,1));return b;}
    @Override public void onResume(){super.onResume();display=null;if(getView()!=null){ThemeApplier.apply(requireView());render();renderSchedule();}updateBack();}
    @Override public void onPause(){persist();super.onPause();updateBack();}
    @Override public void onHiddenChanged(boolean hidden){super.onHiddenChanged(hidden);if(history!=null)history.visibilityChanged();updateBack();if(!hidden&&getView()!=null){display=null;ThemeApplier.apply(requireView());render();renderSchedule();}}
    @Override public void onSaveInstanceState(@NonNull Bundle out){saveCarets();super.onSaveInstanceState(out);out.putString("draft",draft.json());out.putBoolean("keys",keysOpen);out.putInt("focus",focus);out.putInt("tab",tab);out.putInt("page",pageStart);out.putBoolean("yearly",yearly);out.putIntArray("carets",carets);if(history!=null)history.saveState(out);}
    @Override public void onDestroyView(){persist();epoch++;if(calculation!=null)calculation.cancel(true);main.removeCallbacks(calculateLater);if(history!=null)history.invalidate();ThemeManager.removeListener(themeListener);if(tabs!=null)tabs.detach();tabs=null;graphy=null;display=null;form=schedule=null;editors=null;table=null;back.setEnabled(false);super.onDestroyView();}
}
