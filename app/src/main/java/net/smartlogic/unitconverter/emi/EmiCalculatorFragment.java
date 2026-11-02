package net.smartlogic.unitconverter.emi;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.widget.FixedTableView;
import net.smartlogic.unitconverter.age.AgeDialogFragment;
import net.smartlogic.unitconverter.fragment.ConverterPickerDialogFragment;
import net.smartlogic.unitconverter.graphy.chart.ValueChartView;
import net.smartlogic.unitconverter.graphy.compose.GraphyPanelController;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import net.smartlogic.unitconverter.helper.*;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.json.JSONObject;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Native fixed-screen loan workspace. Arithmetic, formatting, plotting and IO have shared owners. */
public final class EmiCalculatorFragment extends Fragment {
    private static final String STORAGE = "emi_workspace_v1", EXPORT_DIR = "emi_exports", EXPORT_PREFIX = "emi-";
    private static final ExecutorService CALCULATIONS = new java.util.concurrent.ThreadPoolExecutor(1, 1, 0L,
            java.util.concurrent.TimeUnit.MILLISECONDS, new java.util.concurrent.ArrayBlockingQueue<>(1),
            new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());
    private static final ExecutorService DOCUMENTS = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private Future<?> calculation;
    private EmiDraft a, b;
    private EmiResult resultA, resultB;
    private String screen = "main", backup, ruleAmount = "", ruleFirst = "1", ruleLast = "1", pendingExport;
    private int[] caretA = {-1,-1,-1,-1,-1,-1}, caretB = {-1,-1,-1,-1,-1,-1}, caretRule = {-1,-1,-1,-1,-1,-1};
    private int errorA, errorB;
    private int tab, focus, pageStart, rulePage, ruleIndex = -1, ruleInterval, revision, epoch;
    private boolean editingB, yearly, balanceChart, prepaymentGraph, keysOpen, binding, restoreHistory, exportBusy;
    private View mainPage, formPage, schedulePage;
    private FrameLayout secondary;
    private EditText[] editors;
    private ViewPager2 pager;
    private WorkspaceTabController tabs;
    private GraphyPanelController graphy;
    private View graphPage;
    private IdleHistoryRecorder history;
    private boolean restoreSecondary;
    private final OnBackPressedCallback back = new OnBackPressedCallback(false) {
        @Override public void handleOnBackPressed() { handleBack(); }
    };
    private FixedTableView scheduleTable, rulesTable;
    private final Runnable calculateLater = this::calculate;
    private final ThemeManager.Listener themeListener = theme -> { if (getView() != null) { ThemeApplier.apply(getView()); render(); refreshSecondary(); } };
    private final ActivityResultLauncher<Intent> saveDocument = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), response -> {
        String path = pendingExport; pendingExport = null; exportBusy = false;
        if (path == null) return;
        Context context = requireContext().getApplicationContext();
        var uri = response.getResultCode() == android.app.Activity.RESULT_OK && response.getData() != null ? response.getData().getData() : null;
        DOCUMENTS.execute(() -> {
            try {
                if (uri == null) PreparedDocumentFiles.discard(context, EXPORT_DIR, EXPORT_PREFIX, path);
                else PreparedDocumentFiles.save(context, EXPORT_DIR, EXPORT_PREFIX, path, uri);
                if (uri != null) main.post(() -> toast(R.string.emi_export_saved));
            } catch (Exception failure) { main.post(() -> toast(R.string.emi_export_error)); }
        });
    });

    @Override public void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        a = new EmiDraft(Preferences.getInstance(requireContext()).getPreferredCurrency());
        String state = saved == null ? Preferences.getInstance(requireContext()).getPreferences().getString(STORAGE, null) : saved.getString("workspace");
        if (state != null) readWorkspace(state);
        if (saved != null) {
            if (saved.getIntArray("caret_a") != null) caretA = saved.getIntArray("caret_a");
            if (saved.getIntArray("caret_b") != null) caretB = saved.getIntArray("caret_b");
            if (saved.getIntArray("caret_rule") != null) caretRule = saved.getIntArray("caret_rule");
            keysOpen = saved.getBoolean("keys"); screen = saved.getString("screen", "main"); backup = saved.getString("backup");
            editingB = saved.getBoolean("editing_b"); pendingExport = saved.getString("pending_export"); exportBusy = pendingExport != null;
            ruleAmount = saved.getString("rule_amount", ""); ruleFirst = saved.getString("rule_first", "1"); ruleLast = saved.getString("rule_last", "1");
            ruleIndex = saved.getInt("rule_index", -1); ruleInterval = saved.getInt("rule_interval");
        }
        if (editingB && b == null) { editingB = false; screen = "main"; }
    }
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_emi_calculator, parent, false);
    }
    @Override public void onViewCreated(@NonNull View root, @Nullable Bundle saved) {
        epoch++; history = new IdleHistoryRecorder(this, saved); secondary = root.findViewById(R.id.emi_secondary);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), back);
        getChildFragmentManager().setFragmentResultListener(EmiDialogFragment.RESULT, getViewLifecycleOwner(), this::onChoiceResult);
        getChildFragmentManager().setFragmentResultListener(AgeDialogFragment.RESULT, getViewLifecycleOwner(), (key, value) -> {
            if ("emi_date".equals(value.getString("mode"))) { draft().firstEmi = LocalDate.parse(value.getString("date")); bindDate(); changed(); }
        });
        getChildFragmentManager().setFragmentResultListener(ConverterPickerDialogFragment.RESULT_KEY, getViewLifecycleOwner(), (key, value) -> {
            if (scheduleTable != null && value.getInt(ConverterPickerDialogFragment.GROUP) == 1) scheduleTable.jump(value.getInt(ConverterPickerDialogFragment.INDEX));
        });
        pager = root.findViewById(R.id.workspace_pager); pager.setOffscreenPageLimit(2);
        pager.setAdapter(new WorkspacePagerAdapter(new int[]{R.layout.pane_emi_calculate, R.layout.pane_emi_section, R.layout.pane_emi_graphy}, this::bindWorkspacePage));
        int restoredTab = tab;
        tabs = new WorkspaceTabController(pager, root.findViewById(R.id.workspace_tabs), new int[]{R.string.tab_calculate, R.string.emi_schedule, R.string.graphy}, new WorkspaceTabController.Callback() {
            @Override public void onTabSelected(int position) {
                if (position != 0) { saveSelection(); clearEditorFocus(); setKeys(false); }
                tab = position; updateBack();
            }
            @Override public boolean isTabEnabled(int position) { return position == 0 || resultA != null; }
        });
        tab = restoredTab; ThemeManager.addListener(themeListener); ThemeApplier.apply(root);
        restoreSecondary = !screen.equals("main");
        root.findViewById(R.id.emi_workspace).setVisibility(restoreSecondary ? View.INVISIBLE : View.VISIBLE);
        changed();
    }
    private void onChoiceResult(String key, Bundle value) { choice(value.getInt("action")); }
    private void bindWorkspacePage(int position, View page) {
            if (position == 0) { mainPage = page; if (screen.equals("main")) bindForm(page, false); }
            else if (position == 1) { schedulePage = page; renderSchedule(); }
            else { graphPage = page; graphy = new GraphyPanelController(page.findViewById(R.id.graphy_panel), this, new GraphyViewTheme(requireContext()));
                page.findViewById(R.id.emi_graph_mode).setOnClickListener(v -> {
                    if (resultA != null && !a.rules.isEmpty()) { prepaymentGraph = !prepaymentGraph; renderGraph(); }
                }); }
            ThemeApplier.apply(page); render();
    }
    private EmiDraft draft() { return editingB && b != null ? b : a; }
    private EmiResult result() { return editingB ? resultB : resultA; }
    private void bindForm(View page, boolean rule) {
        formPage = page; binding = true; boolean firstBinding = page.getTag(R.id.emi_layout) == null;
        editors = new EditText[]{page.findViewById(R.id.emi_principal), page.findViewById(R.id.emi_rate), page.findViewById(R.id.emi_tenure)};
        String[] values = rule ? new String[]{ruleAmount, ruleFirst, ruleLast} : new String[]{draft().principal, draft().rate, draft().tenure};
        int[] labels = rule ? new int[]{R.string.emi_rule_amount, R.string.emi_rule_first, R.string.emi_rule_last} : new int[]{R.string.emi_loan_amount, R.string.emi_annual_rate, R.string.emi_tenure};
        for (int i = 0; i < editors.length; i++) {
            EditText field = editors[i]; int index = i;
            field.setText(values[i]); field.setShowSoftInputOnFocus(false);
            int[] caret = rule ? caretRule : editingB ? caretB : caretA;
            int length = field.length();
            field.setSelection(caret[i*2] < 0 ? length : Math.min(length, caret[i*2]), caret[i*2+1] < 0 ? length : Math.min(length, caret[i*2+1]));
            field.setContentDescription(getString(labels[i]));
            ((TextView) page.findViewById(new int[]{R.id.emi_principal_label, R.id.emi_rate_label, R.id.emi_tenure_label}[i])).setText(labels[i]);
            field.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
                if (binding) return null;
                String next = dest.subSequence(0, dstart).toString() + source.subSequence(start, end) + dest.subSequence(dend, dest.length());
                int scale = rule && index > 0 ? 0 : index == 1 && !rule ? 4 : index == 2 && !rule ? 6 : java.util.Currency.getInstance(draft().currency).getDefaultFractionDigits();
                int digits = rule && index > 0 ? 3 : index == 1 && !rule ? 3 : index == 2 && !rule ? 3 : 12;
                boolean accepted = next.matches("[0-9]{0," + digits + "}" + (scale > 0 ? "(?:\\.[0-9]{0," + scale + "})?" : ""));
                if (accepted && !next.isEmpty() && !next.equals(".")) {
                    BigDecimal candidate = new BigDecimal(next);
                    if (!rule && index == 1) accepted = candidate.compareTo(new BigDecimal("100")) <= 0;
                    else if (!rule && index == 2) accepted = candidate.multiply(new BigDecimal(draft().years ? "12" : "1")).compareTo(new BigDecimal(EmiCalculator.MAX_MONTHS)) <= 0;
                    else if (rule && index > 0) {
                        EmiInputs loan = validInputs(draft());
                        accepted = candidate.compareTo(new BigDecimal(loan == null ? EmiCalculator.MAX_MONTHS : loan.months())) <= 0;
                    }
                }
                if (!accepted) { toast(R.string.emi_input_rejected); return dest.subSequence(dstart, dend); }
                return null;
            }});
            if (firstBinding) field.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable text) {
                    if (binding) return;
                    if (rule) { if (index == 0) ruleAmount = text.toString(); else if (index == 1) ruleFirst = text.toString(); else ruleLast = text.toString(); }
                    else { if (index == 0) draft().principal = text.toString(); else if (index == 1) draft().rate = text.toString(); else draft().tenure = text.toString(); changed(); }
                }
            });
            field.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) focus = index; });
            field.setOnClickListener(v -> { focus = index; setKeys(true); });
            field.setOnTouchListener((v, e) -> { if (e.getActionMasked() == MotionEvent.ACTION_UP) v.post(() -> { if (getView() != null) { focus = index; field.requestFocus(); setKeys(true); } }); return false; });
        }
        text(page, R.id.emi_principal_label, getString(R.string.emi_currency_choice, getString(rule ? R.string.emi_rule_amount : R.string.emi_loan_amount), new EmiFormatter(requireContext()).symbol(draft().currency)));
        page.findViewById(R.id.emi_units).setVisibility(rule ? View.GONE : View.VISIBLE);
        page.findViewById(R.id.emi_units).setOnClickListener(v -> show(EmiDialogFragment.choices(R.string.emi_tenure, R.string.emi_years, R.string.emi_months)));
        text(page, R.id.emi_date_label, getString(rule ? R.string.emi_rule_frequency : R.string.emi_first_date));
        page.findViewById(R.id.emi_date).setOnClickListener(v -> {
            if (rule) show(EmiDialogFragment.choices(R.string.emi_rule_frequency, R.string.emi_once, R.string.emi_every_month, R.string.emi_every_year));
            else if (!getChildFragmentManager().isStateSaved() && getChildFragmentManager().findFragmentByTag(AgeDialogFragment.TAG) == null)
                AgeDialogFragment.forDate("emi_date", draft().firstEmi, R.string.emi_first_date).show(getChildFragmentManager(), AgeDialogFragment.TAG);
        });
        page.findViewById(R.id.emi_insights).setOnClickListener(v -> {
            if (rule) saveRule(); else if (screen.equals("edit")) finishEdit(true); else open("insights");
        });
        page.findViewById(R.id.emi_more).setOnClickListener(v -> {
            if (rule) open("rules"); else if (screen.equals("edit")) show(EmiDialogFragment.choices(R.string.emi_more, R.string.emi_prepayments, R.string.emi_apply, R.string.emi_discard)); else more();
        });
        text(page, R.id.emi_insights, getString(rule ? R.string.emi_save : screen.equals("edit") ? R.string.emi_apply : R.string.emi_insights));
        text(page, R.id.emi_more, getString(rule ? R.string.emi_cancel : R.string.emi_more));
        View keys = page.findViewById(R.id.emi_keys);
        View minus = keys.findViewById(R.id.minus);
        if (minus != null) { ViewGroup row = (ViewGroup) minus.getParent();
        int position = row.indexOfChild(minus); ViewGroup.LayoutParams lp = minus.getLayoutParams(); row.removeView(minus);
        View done = getLayoutInflater().inflate(R.layout.bmi_keypad_done, row, false); done.setContentDescription(getString(R.string.emi_done)); row.addView(done, position, lp); }
        ((ImageView) keys.findViewById(R.id.copy)).setImageResource(R.drawable.ic_arrow_forward); keys.findViewById(R.id.copy).setContentDescription(getString(R.string.emi_next_field));
        for (int id : new int[]{R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine, R.id.double_zero, R.id.dot, R.id.ac, R.id.backspace, R.id.copy, R.id.equal}) {
            View key = keys.findViewById(id); InteractionFeedbackManager.configure(key);
            key.setOnClickListener(v -> { InteractionFeedbackManager.perform(v, InteractionFeedbackManager.Type.INPUT); keypad(id); });
        }
        page.setTag(R.id.emi_layout, Boolean.TRUE); binding = false; bindDate(); setKeys(keysOpen); renderForm(); ThemeApplier.apply(page);
    }
    private void bindDate() {
        if (formPage == null) return;
        text(formPage, R.id.emi_date, screen.equals("rule") ? getString(frequency(ruleInterval)) : new EmiFormatter(requireContext()).date(draft().firstEmi));
        text(formPage, R.id.emi_units, getString(draft().years ? R.string.emi_years : R.string.emi_months));
    }
    private void keypad(int id) {
        if (editors == null) return;
        if (id == R.id.equal) { setKeys(false); return; }
        if (id == R.id.copy) { focus = (focus + 1) % editors.length; editors[focus].requestFocus(); return; }
        EditText field = editors[Math.max(0, Math.min(focus, editors.length - 1))];
        int start = Math.max(0, field.getSelectionStart()), end = Math.max(start, field.getSelectionEnd());
        if (id == R.id.ac) { field.getText().clear(); return; }
        if (id == R.id.backspace) { if (start == end && start > 0) start--; field.getText().delete(start, end); return; }
        String value = id == R.id.dot ? "." : "00";
        int[] digits = {R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine};
        for (int i = 0; i < digits.length; i++) if (id == digits[i]) value = Integer.toString(i);
        field.getText().replace(start, end, value);
    }
    private void setKeys(boolean open) {
        keysOpen = open;
        if (formPage != null) {
            formPage.findViewById(R.id.emi_keys).setVisibility(open ? View.VISIBLE : View.GONE);
            formPage.findViewById(R.id.emi_actions).setVisibility(open ? View.GONE : View.VISIBLE);
            formPage.findViewById(R.id.emi_message).setVisibility(open && result() != null && !screen.equals("rule") ? View.GONE : View.VISIBLE);
            if (open && editors != null) editors[Math.min(focus, editors.length - 1)].requestFocus();
        }
        updateBack();
        renderForm();
    }
    private void changed() {
        revision++; if (calculation != null) calculation.cancel(true);
        resultA = resultB = null; errorA = errorB = 0; if (history != null) history.invalidate();
        main.removeCallbacks(calculateLater); main.postDelayed(calculateLater, 120);
        render(); persist();
    }
    private static EmiInputs validInputs(EmiDraft d) {
        if (d == null || d.principal.endsWith(".") || d.rate.endsWith(".") || d.tenure.endsWith(".")) return null;
        try { EmiInputs input = d.inputs(); EmiCalculator.validate(input); return input; } catch (RuntimeException invalid) { return null; }
    }
    private void calculate() {
        if (!isAdded() || getView() == null) return;
        EmiInputs inputA = validInputs(a), inputB = validInputs(b); int request = revision, viewEpoch = epoch;
        calculation = CALCULATIONS.submit(() -> {
            EmiResult ra = null, rb = null; int failedA = 0, failedB = 0;
            try { if (inputA != null) ra = EmiCalculator.calculate(inputA); } catch (IllegalArgumentException e) { failedA = "rounding".equals(e.getMessage()) ? R.string.emi_rounding : R.string.emi_check_fields; }
            if (Thread.currentThread().isInterrupted()) return;
            try { if (inputB != null) rb = EmiCalculator.calculate(inputB); } catch (IllegalArgumentException e) { failedB = "rounding".equals(e.getMessage()) ? R.string.emi_rounding : R.string.emi_check_fields; }
            EmiResult finalA = ra, finalB = rb; int finalErrorA = failedA, finalErrorB = failedB;
            main.post(() -> {
                if (getView() == null || request != revision || viewEpoch != epoch) return;
                resultA = finalA; resultB = finalB; errorA = finalErrorA; errorB = finalErrorB; render(); renderSchedule(); refreshSecondary();
                if (resultA != null && !screen.equals("edit")) {
                    EmiFormatter f = new EmiFormatter(requireContext());
                    history.update(getString(R.string.emi_history_summary, f.money(a.inputs().principal(), resultA), a.rate, a.inputs().months()),
                            getString(R.string.emi_monthly) + ": " + f.money(resultA.regularEmi(), resultA), CalculatorCatalog.ID_EMI, a.json().toString());
                    if (restoreHistory) { history.markCurrentAsSaved(); restoreHistory = false; }
                }
                if (tab != 0 && resultA != null && screen.equals("main")) tabs.selectTab(tab);
            });
        });
    }
    private void render() {
        if (!isAdded() || getView() == null) return;
        requireView().setBackgroundColor(ThemeManager.get().background());
        renderForm(); renderGraph();
        TabLayout bar = requireView().findViewById(R.id.workspace_tabs);
        for (int i = 1; i < 3; i++) { var item = bar.getTabAt(i); if (item != null) { item.view.setEnabled(resultA != null); item.view.setAlpha(resultA == null ? 0.5f : 1f); } }
        updateBack();
    }
    private void renderForm() {
        if (formPage == null || !isAdded()) return;
        for (int id : new int[]{R.id.emi_value, R.id.emi_total_interest, R.id.emi_total_payment})
            formPage.findViewById(id).setVisibility(screen.equals("rule") ? View.GONE : View.VISIBLE);
        if (screen.equals("rule")) {
            formPage.findViewById(R.id.emi_insights).setEnabled(true);
            text(formPage, R.id.emi_value_label, getString(R.string.emi_prepayments)); text(formPage, R.id.emi_value, "");
            text(formPage, R.id.emi_total_interest, ""); text(formPage, R.id.emi_total_payment, ""); text(formPage, R.id.emi_message, getString(R.string.emi_rule_after_payoff)); return;
        }
        EmiResult r = result(); EmiFormatter f = new EmiFormatter(requireContext());
        formPage.findViewById(R.id.emi_insights).setEnabled(r != null);
        int failure = editingB ? errorB : errorA;
        boolean changes = !draft().rules.isEmpty() && draft().method == EmiInputs.Method.REDUCE_EMI;
        text(formPage, R.id.emi_value_label, getString(changes ? R.string.emi_initial_monthly : R.string.emi_monthly));
        text(formPage, R.id.emi_value, r == null ? getString(R.string.emi_no_result) : f.money(r.regularEmi(), r));
        text(formPage, R.id.emi_total_interest, getString(R.string.emi_interest) + ": " + (r == null ? getString(R.string.emi_no_result) : f.money(r.totalInterest(), r)));
        text(formPage, R.id.emi_total_payment, getString(R.string.emi_total) + ": " + (r == null ? getString(R.string.emi_no_result) : f.money(r.totalPayment(), r)));
        text(formPage, R.id.emi_message, getString(r == null && failure != 0 ? failure : r != null ? changes ? R.string.emi_changing_payments : R.string.emi_fixed : draft().principal.isEmpty() || draft().rate.isEmpty() ? R.string.emi_empty : validInputs(draft()) != null ? R.string.emi_loading : R.string.emi_check_fields));
        if (keysOpen && r == null) {
            TextView message = formPage.findViewById(R.id.emi_message);
            message.setContentDescription(message.getText());
            if (failure == R.string.emi_rounding) message.setText(R.string.emi_rounding_short);
            else if (failure != 0 || validInputs(draft()) == null && !draft().principal.isEmpty() && !draft().rate.isEmpty()) message.setText(R.string.emi_check_short);
        } else formPage.findViewById(R.id.emi_message).setContentDescription(null);
        formPage.findViewById(R.id.emi_message).setVisibility(keysOpen && r != null ? View.GONE : View.VISIBLE);
    }
    private void renderGraph() {
        if (graphy == null) return;
        boolean extra = prepaymentGraph && !a.rules.isEmpty();
        text(graphPage, R.id.emi_graph_mode, getString(extra ? R.string.emi_formula : R.string.emi_graph_prepay));
        graphPage.findViewById(R.id.emi_graph_mode).setVisibility(resultA != null && !a.rules.isEmpty() ? View.VISIBLE : View.GONE);
        graphy.update(resultA == null ? null : EmiGraphBuilder.build(requireContext(), resultA, extra, a.years));
    }
    private void more() { show(EmiDialogFragment.choices(R.string.emi_more, R.string.emi_prepayments, R.string.emi_compare, R.string.emi_report, R.string.emi_share, R.string.emi_reset)); }
    private void choice(int action) {
        if (action == R.string.emi_prepayments) {
            try { EmiCalculator.validate(draft().inputs().withoutPrepayments()); open("rules"); }
            catch (RuntimeException invalid) { toast(R.string.emi_invalid); }
        }
        else if (action == R.string.emi_compare) {
            if (resultA == null) { toast(R.string.emi_invalid); return; }
            if (b == null) b = EmiDraft.read(a.json().toString()); editingB = false; open("compare"); changed();
        } else if (action == R.string.emi_report || action == R.string.emi_export_comparison) show(EmiDialogFragment.choices(R.string.emi_choose_export, R.string.emi_pdf, R.string.emi_csv));
        else if (action == R.string.emi_share) show(EmiDialogFragment.choices(R.string.emi_share, R.string.emi_share_text, R.string.emi_share_pdf, R.string.emi_share_csv));
        else if (action == R.string.emi_pdf || action == R.string.emi_csv || action == R.string.emi_share_pdf || action == R.string.emi_share_csv)
            export(action == R.string.emi_pdf || action == R.string.emi_share_pdf, action == R.string.emi_share_pdf || action == R.string.emi_share_csv);
        else if (action == R.string.emi_share_text) shareText();
        else if (action == R.string.emi_reset) show(EmiDialogFragment.confirm(R.string.emi_reset, getString(R.string.emi_reset_question), R.string.emi_clear_field));
        else if (action == R.string.emi_clear_field) { a = new EmiDraft(Preferences.getInstance(requireContext()).getPreferredCurrency()); b = null; editingB = false; tab = 0; pageStart = 0; keysOpen = false; open("main"); changed(); }
        else if (action == R.string.emi_apply) finishEdit(true);
        else if (action == R.string.emi_discard) finishEdit(false);
        else if (action == R.string.emi_years || action == R.string.emi_months) switchUnits(action == R.string.emi_years);
        else if (action == R.string.emi_once || action == R.string.emi_every_month || action == R.string.emi_every_year) { ruleInterval = action == R.string.emi_once ? 0 : action == R.string.emi_every_month ? 1 : 12; bindDate(); }
        else if (action == R.string.emi_reduce_tenure || action == R.string.emi_reduce_emi) { draft().method = action == R.string.emi_reduce_tenure ? EmiInputs.Method.REDUCE_TENURE : EmiInputs.Method.REDUCE_EMI; changed(); refreshSecondary(); }
        else if (action == R.string.emi_edit_rule) showRule(ruleIndex);
        else if (action == R.string.emi_delete) show(EmiDialogFragment.confirm(R.string.emi_delete, getString(R.string.emi_rule_remove_question), R.string.emi_rule_remove_question));
        else if (action == R.string.emi_rule_remove_question && ruleIndex >= 0 && ruleIndex < draft().rules.size()) { draft().rules.remove(ruleIndex); changed(); refreshSecondary(); }
    }
    private void switchUnits(boolean years) {
        if (draft().years == years) return;
        try {
            BigDecimal value = new BigDecimal(draft().tenure);
            BigDecimal converted = years ? value.divide(new BigDecimal("12")) : value.multiply(new BigDecimal("12"));
            String text = converted.stripTrailingZeros().toPlainString();
            if (text.length() > 10) throw new ArithmeticException();
            draft().years = years; draft().tenure = text; binding = true; editors[2].setText(text); binding = false; bindDate(); changed();
        } catch (RuntimeException invalid) { toast(R.string.emi_switch_units_error); }
    }
    private void saveSelection() {
        if (editors == null) return;
        int[] caret = screen.equals("rule") ? caretRule : editingB ? caretB : caretA;
        for (int i = 0; i < editors.length; i++) { caret[i*2] = Math.max(0, editors[i].getSelectionStart()); caret[i*2+1] = Math.max(0, editors[i].getSelectionEnd()); }
    }
    private void clearEditorFocus() { if (editors != null) for (EditText editor : editors) editor.clearFocus(); }
    private void open(String destination) {
        saveSelection(); clearEditorFocus(); setKeys(false); screen = destination;
        if (destination.equals("main")) {
            requireView().findViewById(R.id.emi_workspace).setVisibility(View.VISIBLE);
            editingB = false; secondary.removeAllViews(); secondary.setVisibility(View.GONE); formPage = null; editors = null;
            if (mainPage != null) bindForm(mainPage, false);
            if (tabs != null) tabs.selectTab(0);
        } else showSecondary();
        updateBack(); persist();
    }
    private void showSecondary() {
        if (secondary == null) return;
        requireView().findViewById(R.id.emi_workspace).setVisibility(View.INVISIBLE);
        secondary.removeAllViews(); secondary.setVisibility(View.VISIBLE); secondary.setBackgroundColor(ThemeManager.get().background());
        if (screen.equals("edit") || screen.equals("rule")) {
            View page = getLayoutInflater().inflate(R.layout.pane_emi_calculate, secondary, false); secondary.addView(page); bindForm(page, screen.equals("rule"));
        } else {
            formPage = null; editors = null;
            View page = getLayoutInflater().inflate(R.layout.pane_emi_section, secondary, false); secondary.addView(page);
            if (screen.equals("insights")) renderInsights(page); else if (screen.equals("rules")) renderRules(page); else if (screen.equals("compare")) renderComparison(page);
        }
        ThemeApplier.apply(secondary);
    }
    private void refreshSecondary() { if (secondary != null && !screen.equals("main") && !screen.equals("edit") && !screen.equals("rule")) showSecondary(); }
    private LinearLayout header(View page) { return page.findViewById(R.id.emi_header); }
    private LinearLayout footer(View page) { return page.findViewById(R.id.emi_footer); }
    private FrameLayout body(View page) { return page.findViewById(R.id.emi_body); }
    private TextView label(ViewGroup parent, String value) {
        TextView v = (TextView) getLayoutInflater().inflate(R.layout.emi_label, parent, false); v.setText(value); parent.addView(v); return v;
    }
    private MaterialButton button(ViewGroup parent, int label, Runnable action) {
        MaterialButton v = (MaterialButton) getLayoutInflater().inflate(R.layout.emi_action, parent, false); v.setText(label); InteractionFeedbackManager.configure(v); v.setOnClickListener(view -> action.run()); parent.addView(v); return v;
    }
    private LinearLayout row(LinearLayout parent) { LinearLayout row = new LinearLayout(requireContext()); row.setOrientation(LinearLayout.HORIZONTAL); parent.addView(row, new LinearLayout.LayoutParams(-1, -2)); return row; }
    private void equalButtons(LinearLayout row) { for (int i = 0; i < row.getChildCount(); i++) row.getChildAt(i).setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1)); }
    private void renderSchedule() {
        if (schedulePage == null || !isAdded()) return;
        LinearLayout h = header(schedulePage), foot = footer(schedulePage); h.removeAllViews(); foot.removeAllViews(); body(schedulePage).removeAllViews();
        if (resultA == null) { label(h, getString(R.string.emi_empty)); return; }
        EmiResult r = resultA; EmiFormatter f = new EmiFormatter(requireContext()); List<EmiTableRows.Row> rows = EmiTableRows.build(r, yearly);
        LinearLayout options = row(h); button(options, R.string.emi_monthly_table, () -> { yearly = false; pageStart = 0; renderSchedule(); }); button(options, R.string.emi_yearly_table, () -> { yearly = true; pageStart = 0; renderSchedule(); }); equalButtons(options);
        label(h, getString(R.string.emi_table_hint, f.symbol(a.currency)));
        TextView range = label(foot, ""); LinearLayout navigation = row(foot);
        FixedTableView table = new FixedTableView(requireContext()); scheduleTable = table;
        MaterialButton previous = button(navigation, R.string.emi_previous, table::previous);
        button(navigation, R.string.emi_jump, () -> {
            ArrayList<String> periods = new ArrayList<>(); for (var item : rows) periods.add(item.label(requireContext(), yearly));
            ConverterPickerDialogFragment.forValues(getString(R.string.emi_jump), 1, table.start(), periods, R.string.emi_jump).showFor(this);
        });
        MaterialButton next = button(navigation, R.string.emi_next, table::next); equalButtons(navigation);
        LinearLayout exports = row(foot); button(exports, R.string.emi_pdf, () -> export(true, false)); button(exports, R.string.emi_csv, () -> export(false, false)); button(exports, R.string.emi_share, () -> choice(R.string.emi_share)); equalButtons(exports);
        List<List<String>> cells = new ArrayList<>();
        for (var item : rows) { List<String> values = new ArrayList<>(java.util.Arrays.asList(item.label(requireContext(), yearly), f.money(item.principal(), r), f.money(item.interest(), r))); cells.add(values); }
        List<String> headings = new ArrayList<>(java.util.Arrays.asList(getString(R.string.emi_period), getString(R.string.emi_principal), getString(R.string.emi_interest_paid)));
        table.setData(headings, cells, pageStart, index -> {
            var item = rows.get(index);
            show(EmiDialogFragment.info(R.string.emi_schedule, getString(R.string.emi_schedule_detail, item.label(requireContext(), yearly), f.money(item.opening(), r), f.money(item.payment(), r), f.money(item.principal(), r), f.money(item.interest(), r), f.money(item.extra(), r), f.money(item.closing(), r), f.money(item.unused(), r))));
        }, () -> { pageStart = table.start(); range.setText(getString(R.string.emi_page_range, table.count() == 0 ? 0 : table.start() + 1, table.end(), table.count())); previous.setEnabled(table.start() > 0); next.setEnabled(table.end() < table.count()); });
        body(schedulePage).addView(table, new FrameLayout.LayoutParams(-1, -1)); ThemeApplier.apply(schedulePage);
    }
    private void renderInsights(View page) {
        LinearLayout h = header(page), foot = footer(page); button(h, R.string.emi_back, () -> open("main"));
        LinearLayout toggle = row(h); button(toggle, R.string.emi_breakdown, () -> { balanceChart = false; showSecondary(); }); button(toggle, R.string.emi_balance_chart, () -> { balanceChart = true; showSecondary(); }); equalButtons(toggle);
        if (resultA == null) { label(h, getString(R.string.emi_empty)); return; }
        EmiFormatter f = new EmiFormatter(requireContext());
        label(h, getString(R.string.emi_total) + ": " + f.money(resultA.totalPayment(), resultA));
        label(h, getString(R.string.emi_payments_value, resultA.installments().size()) + " · " + f.date(resultA.payoffDate()));
        label(h, getString(R.string.emi_final) + ": " + f.money(resultA.finalPayment(), resultA));
        if (!a.rules.isEmpty()) label(h, getString(R.string.emi_saved_interest) + ": " + f.money(resultA.interestSaved(), resultA) + " · " + getString(R.string.emi_saved_months) + ": " + resultA.monthsSaved());
        var data = balanceChart ? EmiCharts.balance(requireContext(), resultA, null) : EmiCharts.breakdown(requireContext(), resultA);
        button(foot, R.string.emi_view_schedule, () -> { open("main"); tabs.selectTab(1); });
        body(page).addView(new ValueChartView(requireContext(), data), new FrameLayout.LayoutParams(-1, -1));
    }
    private void renderRules(View page) {
        LinearLayout h = header(page), foot = footer(page); button(h, R.string.emi_back, () -> open(editingB || backup != null ? "edit" : "main"));
        MaterialButton method = button(h, R.string.emi_method, () -> show(EmiDialogFragment.choices(R.string.emi_method, R.string.emi_reduce_tenure, R.string.emi_reduce_emi)));
        method.setText(draft().method == EmiInputs.Method.REDUCE_TENURE ? R.string.emi_reduce_tenure : R.string.emi_reduce_emi);
        List<List<String>> cells = new ArrayList<>(); EmiFormatter f = new EmiFormatter(requireContext());
        for (PrepaymentRule rule : draft().rules) cells.add(java.util.Arrays.asList(getString(R.string.emi_rule_label, f.money(rule.amount(), draft().currency), getString(frequency(rule.interval())), rule.firstMonth(), rule.lastMonth())));
        if (cells.isEmpty()) label(h, getString(R.string.emi_no_rules));
        TextView range = label(foot, ""); FixedTableView table = new FixedTableView(requireContext()); rulesTable = table;
        LinearLayout navigation = row(foot); MaterialButton prev = button(navigation, R.string.emi_previous, table::previous); MaterialButton next = button(navigation, R.string.emi_next, table::next); equalButtons(navigation);
        button(foot, R.string.emi_add_rule, () -> { if (draft().rules.size() >= EmiCalculator.MAX_RULES) toast(R.string.emi_rule_limit); else showRule(-1); });
        table.setData(java.util.Arrays.asList(), cells, rulePage, index -> { ruleIndex = index; show(EmiDialogFragment.choices(R.string.emi_prepayments, R.string.emi_edit_rule, R.string.emi_delete)); },
                () -> { rulePage = table.start(); range.setText(getString(R.string.emi_page_range, cells.isEmpty() ? 0 : table.start() + 1, table.end(), table.count())); prev.setEnabled(table.start() > 0); next.setEnabled(table.end() < table.count()); });
        body(page).addView(table, new FrameLayout.LayoutParams(-1, -1));
    }
    private void showRule(int index) {
        ruleIndex = index; caretRule = new int[]{-1,-1,-1,-1,-1,-1};
        if (index >= 0) { PrepaymentRule r = draft().rules.get(index); ruleAmount = r.amount().toPlainString(); ruleFirst = Integer.toString(r.firstMonth()); ruleLast = Integer.toString(r.lastMonth()); ruleInterval = r.interval(); }
        else { ruleAmount = ""; ruleFirst = "1"; ruleLast = Integer.toString(draft().inputs().months()); ruleInterval = 0; }
        open("rule");
    }
    private void saveRule() {
        try {
            PrepaymentRule rule = new PrepaymentRule(new BigDecimal(ruleAmount), Integer.parseInt(ruleFirst), ruleInterval == 0 ? Integer.parseInt(ruleFirst) : Integer.parseInt(ruleLast), ruleInterval);
            List<PrepaymentRule> prospective = new ArrayList<>(draft().rules);
            if (ruleIndex >= 0) prospective.set(ruleIndex, rule); else prospective.add(rule);
            EmiInputs i = draft().inputs(); if (prospective.size() > EmiCalculator.MAX_RULES) throw new IllegalArgumentException("rules");
            EmiCalculator.validate(new EmiInputs(i.principal(), i.annualRate(), i.months(), i.firstEmi(), i.currencyCode(), i.method(), java.util.Collections.singletonList(rule)));
            draft().rules.clear(); draft().rules.addAll(prospective); open("rules"); changed();
        } catch (RuntimeException invalid) { text(formPage, R.id.emi_message, getString(R.string.emi_rule_error)); }
    }
    private int frequency(int interval) { return interval == 0 ? R.string.emi_once : interval == 1 ? R.string.emi_every_month : R.string.emi_every_year; }
    private void renderComparison(View page) {
        LinearLayout h = header(page), foot = footer(page); button(h, R.string.emi_back, () -> { editingB = false; open("main"); });
        if (resultA == null || resultB == null) { label(h, getString(R.string.emi_missing_comparison)); }
        else {
            EmiFormatter f = new EmiFormatter(requireContext());
            label(h, getString(R.string.emi_loan_a) + " · " + f.money(resultA.regularEmi(), resultA) + "\n" + getString(R.string.emi_loan_b) + " · " + f.money(resultB.regularEmi(), resultB));
            label(h, getString(R.string.emi_interest) + ": " + f.money(resultA.totalInterest(), resultA) + " / " + f.money(resultB.totalInterest(), resultB));
            label(h, getString(R.string.emi_total) + ": " + f.money(resultA.totalPayment(), resultA) + " / " + f.money(resultB.totalPayment(), resultB));
            label(h, getString(R.string.emi_count) + ": " + resultA.installments().size() + " / " + resultB.installments().size());
            label(h, getString(R.string.emi_difference) + ": " + f.money(resultB.totalPayment().subtract(resultA.totalPayment()), resultA) + " · " + (resultB.installments().size() - resultA.installments().size()) + " " + getString(R.string.emi_months));
            LinearLayout dates = new LinearLayout(requireContext()); dates.setOrientation(LinearLayout.VERTICAL);
            label(dates, getString(R.string.emi_payoff) + "\n" + getString(R.string.emi_loan_a) + ": " + f.date(resultA.payoffDate())
                    + "\n" + getString(R.string.emi_loan_b) + ": " + f.date(resultB.payoffDate()));
            body(page).addView(dates, new FrameLayout.LayoutParams(-1, -2));
        }
        LinearLayout edits = row(foot); button(edits, R.string.emi_edit_a, () -> beginEdit(false)); button(edits, R.string.emi_edit_b, () -> beginEdit(true)); equalButtons(edits);
        LinearLayout actions = row(foot); button(actions, R.string.emi_export_comparison, () -> choice(R.string.emi_export_comparison)); button(actions, R.string.emi_share, () -> choice(R.string.emi_share)); equalButtons(actions);
    }
    private void beginEdit(boolean loanB) { editingB = loanB; backup = draft().json().toString(); open("edit"); }
    private void finishEdit(boolean apply) {
        saveSelection();
        if (apply && result() == null) { toast(R.string.emi_invalid); return; }
        if (!apply && backup != null) { if (editingB) b = EmiDraft.read(backup); else a = EmiDraft.read(backup); }
        backup = null; editors = null; editingB = false; open("compare"); changed();
    }
    public boolean handleBack() {
        if (keysOpen) { setKeys(false); return true; }
        if (screen.equals("rule")) { open("rules"); return true; }
        if (screen.equals("edit")) { finishEdit(false); return true; }
        if (!screen.equals("main")) { if (screen.equals("rules") && backup != null) open("edit"); else open("main"); return true; }
        if (tab != 0 && tabs != null) { tabs.selectTab(0); return true; }
        return false;
    }
    private void updateBack() { if (back != null) back.setEnabled(isResumed() && !isHidden() && (keysOpen || tab != 0 || !screen.equals("main"))); }
    private void show(EmiDialogFragment dialog) { if (!getChildFragmentManager().isStateSaved() && getChildFragmentManager().findFragmentByTag(EmiDialogFragment.TAG) == null) dialog.showNow(getChildFragmentManager(), EmiDialogFragment.TAG); }
    private void text(View parent, int id, String value) { ((TextView) parent.findViewById(id)).setText(value); }
    private void toast(int message) { if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show(); }
    public void restoreFromHistory(String payload) {
        try { EmiDraft restored = EmiDraft.read(payload); PreferredCurrency.find(requireContext(), restored.currency); if (validInputs(restored) == null) throw new IllegalArgumentException("history"); a = restored; b = null; editingB = false; backup = null; restoreHistory = true; open("main"); changed(); }
        catch (RuntimeException invalid) { toast(R.string.emi_state_error); }
    }
    private String workspace() {
        try { JSONObject state = new JSONObject().put("a", a.json()).put("tab", tab).put("page", pageStart).put("yearly", yearly).put("balance", balanceChart).put("graph", prepaymentGraph).put("focus", focus).put("rule_page", rulePage);
            if (b != null) state.put("b", b.json()); return state.toString();
        } catch (org.json.JSONException impossible) { throw new IllegalStateException(impossible); }
    }
    private void readWorkspace(String text) {
        try { if (text.length() > 70000) return; JSONObject state = new JSONObject(text); EmiDraft restored = EmiDraft.read(state.getJSONObject("a").toString()); PreferredCurrency.find(requireContext(), restored.currency); a = restored;
            if (state.has("b")) { b = EmiDraft.read(state.getJSONObject("b").toString()); if (!b.currency.equals(a.currency)) b = null; }
            tab = Math.max(0, Math.min(2, state.optInt("tab"))); pageStart = Math.max(0, state.optInt("page")); yearly = state.optBoolean("yearly"); balanceChart = state.optBoolean("balance"); prepaymentGraph = state.optBoolean("graph"); focus = Math.max(0, Math.min(2, state.optInt("focus"))); rulePage = Math.max(0, state.optInt("rule_page"));
        } catch (Exception ignored) { /* Keep valid current state; never log loan values. */ }
    }
    private void persist() { if (a != null && isAdded()) Preferences.getInstance(requireContext()).getPreferences().edit().putString(STORAGE, workspace()).apply(); }
    private void shareText() {
        if (resultA == null) { toast(R.string.emi_invalid); return; }
        EmiFormatter f = new EmiFormatter(requireContext()); String text = f.summary(resultA);
        if (screen.equals("compare") && resultB != null) text = getString(R.string.emi_loan_a) + "\n" + text + "\n\n" + getString(R.string.emi_loan_b) + "\n" + f.summary(resultB);
        try { startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), getString(R.string.emi_share))); }
        catch (ActivityNotFoundException e) { toast(R.string.emi_share_error); }
    }
    private void export(boolean pdf, boolean share) {
        if (exportBusy) { toast(R.string.emi_export_busy); return; }
        EmiResult first = resultA, second = screen.equals("compare") ? resultB : null; boolean annual = yearly;
        if (first == null || screen.equals("compare") && second == null) { toast(R.string.emi_invalid); return; }
        exportBusy = true; toast(R.string.emi_export_busy); Context context = requireContext().getApplicationContext(); int viewEpoch = epoch;
        DOCUMENTS.execute(() -> {
            try {
                File prepared = PreparedDocumentFiles.create(context, EXPORT_DIR, EXPORT_PREFIX, pdf ? "pdf" : "csv", out -> {
                    if (pdf) EmiReportRenderer.pdf(context, out, first, second, annual); else EmiReportRenderer.csv(context, out, first, second, annual);
                });
                main.post(() -> {
                    if (!isAdded() || getView() == null || epoch != viewEpoch || isHidden()) { prepared.delete(); exportBusy = false; return; }
                    String mime = pdf ? "application/pdf" : "text/csv";
                    try {
                        if (share) {
                            var uri = FileProvider.getUriForFile(context, context.getPackageName() + ".reports", prepared);
                            Intent intent = new Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
                                     .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.setClipData(ClipData.newRawUri(getString(R.string.emi_report_title), uri));
                            startActivity(Intent.createChooser(intent, getString(R.string.emi_share))); exportBusy = false;
                        } else {
                            pendingExport = prepared.getAbsolutePath();
                            saveDocument.launch(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(mime).addCategory(Intent.CATEGORY_OPENABLE)
                                    .putExtra(Intent.EXTRA_TITLE, "EMI-" + first.inputs().firstEmi() + (pdf ? ".pdf" : ".csv")));
                        }
                    } catch (RuntimeException failure) { prepared.delete(); pendingExport = null; exportBusy = false; toast(R.string.emi_export_error); }
                });
            } catch (Exception failure) { main.post(() -> { exportBusy = false; toast(R.string.emi_export_error); }); }
        });
    }
    @Override public void onResume() { super.onResume(); if (getView() != null) { if (restoreSecondary) { restoreSecondary = false; showSecondary(); } ThemeApplier.apply(requireView()); render(); renderSchedule(); refreshSecondary(); } updateBack(); }
    @Override public void onPause() { persist(); super.onPause(); updateBack(); }
    @Override public void onHiddenChanged(boolean hidden) { super.onHiddenChanged(hidden); if (history != null) history.visibilityChanged(); updateBack(); if (!hidden && getView() != null) { render(); refreshSecondary(); } }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        saveSelection(); super.onSaveInstanceState(out); out.putIntArray("caret_a", caretA); out.putIntArray("caret_b", caretB); out.putIntArray("caret_rule", caretRule); out.putString("workspace", workspace()); out.putString("screen", screen); out.putBoolean("keys", keysOpen); out.putString("backup", backup);
        out.putBoolean("editing_b", editingB); out.putString("pending_export", pendingExport); out.putString("rule_amount", ruleAmount); out.putString("rule_first", ruleFirst); out.putString("rule_last", ruleLast); out.putInt("rule_index", ruleIndex); out.putInt("rule_interval", ruleInterval);
        if (history != null) history.saveState(out);
    }
    @Override public void onDestroyView() {
        persist(); if (history != null) history.invalidate(); epoch++; if (calculation != null) calculation.cancel(true); main.removeCallbacks(calculateLater); ThemeManager.removeListener(themeListener);
        if (tabs != null) tabs.detach(); tabs = null; pager = null; graphy = null; graphPage = null; formPage = null; mainPage = null; schedulePage = null; secondary = null; editors = null; back.setEnabled(false); scheduleTable = null; rulesTable = null;
        super.onDestroyView();
    }
}
