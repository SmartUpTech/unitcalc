package net.smartlogic.unitconverter.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.graphy.compose.GraphyPanelController;
import net.smartlogic.unitconverter.graphy.integration.CalculationSnapshot;
import net.smartlogic.unitconverter.graphy.integration.GraphyBridge;
import net.smartlogic.unitconverter.graphy.integration.GraphyPanelHost;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.graphy.renderer.FlowchartRenderer;
import net.smartlogic.unitconverter.graphy.renderer.GraphyRenderer;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import net.smartlogic.unitconverter.helper.DatabaseHelper;
import net.smartlogic.unitconverter.helper.HistoryDateLabels;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.helper.WorkspacePagerAdapter;
import net.smartlogic.unitconverter.helper.WorkspaceTabController;
import net.smartlogic.unitconverter.model.CalculationHistoryItem;
import net.smartlogic.unitconverter.utils.EvaluationResult;
import net.smartlogic.unitconverter.utils.ExpressionDisplayFormatter;
import net.smartlogic.unitconverter.utils.ExpressionEvaluator;
import net.smartlogic.unitconverter.utils.GenericFunctions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CalculatorFragment extends Fragment implements View.OnClickListener, OnLongClickListener, GraphyPanelHost {

    private TextView tvExpression, tvResult;
    private String expression = "";
    private String lastEvaluatedExpression = "";
    private Preferences mPrefs;
    private CoordinatorLayout mCoordinatorLayout;
    private boolean isResultDisplayed = false;
    
    private TextView graphyEmpty;
    private TextView graphyContext;
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;
    private final List<Object> historyRows = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private OnBackPressedCallback onBackPressedCallback;
    private final GraphyBridge graphyBridge = new GraphyBridge();
    private final GraphyRenderer graphyRenderer = new FlowchartRenderer();
    private GraphyViewTheme graphyTheme;
    private ExpressionDisplayFormatter.GraphySemanticTheme semanticTheme;
    private GraphyOutput latestGraphyOutput;

    private TabLayout workspaceTabs;
    private ViewPager2 workspacePager;
    private WorkspaceTabController workspaceTabController;
    private ComposeView graphyPanel;
    private GraphyPanelController graphyPanelController;
    private boolean graphyVisible = false;
    private int lastWorkspaceTab = 0;
    private boolean calculatePageBound;
    private boolean graphyPageBound;
    private boolean historyPageBound;

    private static final int[] WORKSPACE_LAYOUTS = {
            R.layout.pane_calculator_calculate,
            R.layout.pane_calculator_graphy,
            R.layout.pane_calculator_history
    };

    private static final int[] WORKSPACE_TAB_TITLES = {
            R.string.tab_calculate,
            R.string.graphy,
            R.string.nav_history
    };

    public static CalculatorFragment newInstance() {
        return new CalculatorFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = Preferences.getInstance(getActivity());
        dbHelper = new DatabaseHelper(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calculator, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        onBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                selectWorkspaceTab(0);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);
    }

    @Override
    public void onDestroyView() {
        if (workspaceTabController != null) {
            workspaceTabController.detach();
            workspaceTabController = null;
        }
        super.onDestroyView();
    }

    private void initViews(View view) {
        mCoordinatorLayout = view.findViewById(R.id.cl);
        workspaceTabs = view.findViewById(R.id.workspace_tabs);
        workspacePager = view.findViewById(R.id.workspace_pager);
        graphyTheme = new GraphyViewTheme(requireContext());
        semanticTheme = ExpressionDisplayFormatter.GraphySemanticTheme.from(requireContext());

        WorkspacePagerAdapter pagerAdapter = new WorkspacePagerAdapter(
                WORKSPACE_LAYOUTS,
                this::bindWorkspacePage
        );
        workspacePager.setAdapter(pagerAdapter);

        workspaceTabController = new WorkspaceTabController(
                workspacePager,
                workspaceTabs,
                WORKSPACE_TAB_TITLES,
                new WorkspaceTabController.Callback() {
                    @Override
                    public void onTabSelected(int position) {
                        lastWorkspaceTab = position;
                        applyWorkspaceTab(position);
                    }

                    @Override
                    public boolean isTabEnabled(int position) {
                        return position != 1 || hasGraphyContent();
                    }
                }
        );
        updateGraphyTabState();
    }

    private void bindWorkspacePage(int position, @NonNull View pageView) {
        if (position == 0) {
            bindCalculatePage(pageView);
        } else if (position == 1) {
            bindGraphyPage(pageView);
        } else if (position == 2) {
            bindHistoryPage(pageView);
        }
    }

    private void bindCalculatePage(@NonNull View pageView) {
        if (calculatePageBound) {
            return;
        }
        calculatePageBound = true;

        tvExpression = pageView.findViewById(R.id.expression);
        tvResult = pageView.findViewById(R.id.result);

        int[] ids = {
                R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four,
                R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine,
                R.id.dot, R.id.plus, R.id.minus, R.id.multiply, R.id.divide,
                R.id.equal, R.id.backspace, R.id.sign_toggle,
                R.id.parenthesis_open, R.id.parenthesis_close, R.id.power,
                R.id.sqrt, R.id.percent
        };

        for (int id : ids) {
            View v = pageView.findViewById(id);
            if (v != null) {
                v.setOnClickListener(this);
            }
        }

        View backspace = pageView.findViewById(R.id.backspace);
        if (backspace != null) {
            backspace.setOnLongClickListener(this);
        }
        tvExpression.setOnLongClickListener(this);
        tvResult.setOnLongClickListener(this);

        tvExpression.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                GenericFunctions.adjustTextSize(tvExpression, 21);
            }
        });
    }

    private void bindGraphyPage(@NonNull View pageView) {
        if (graphyPageBound) {
            return;
        }
        graphyPageBound = true;

        graphyEmpty = pageView.findViewById(R.id.graphy_empty);
        graphyContext = pageView.findViewById(R.id.graphy_context);
        graphyPanel = pageView.findViewById(R.id.graphy_panel);
        graphyPanelController = new GraphyPanelController(graphyPanel, this, graphyTheme);
    }

    private void bindHistoryPage(@NonNull View pageView) {
        if (historyPageBound) {
            return;
        }
        historyPageBound = true;

        rvHistory = pageView.findViewById(R.id.rv_history);
        historyAdapter = new HistoryAdapter(historyRows, this::restoreFromHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(historyAdapter);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.backspace) {
            handleBackspace();
        } else if (id == R.id.sign_toggle) {
            handleSignToggle();
        } else if (id == R.id.equal) {
            calculateResult(true);
        } else if (id == R.id.dot) {
            handleDotInput();
        } else {
            String input = "";
            if (v instanceof Button) {
                input = ((Button) v).getText().toString();
            }
            handleInput(input);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        int id = v.getId();
        if (id == R.id.backspace) {
            clearAll();
        } else if (id == R.id.expression || id == R.id.result) {
            copyExpressionAndResult();
        }
        return true;
    }

    private void clearAll() {
        expression = "";
        lastEvaluatedExpression = "";
        updateExpressionDisplay();
        tvResult.setText("0");
        isResultDisplayed = false;
        latestGraphyOutput = null;
        hideGraphy();
        updateGraphyTabState();
    }

    public void openHistoryTab() {
        selectWorkspaceTab(2);
    }

    private void restoreFromHistory(@NonNull CalculationHistoryItem item) {
        expression = item.expression;
        lastEvaluatedExpression = item.expression;
        isResultDisplayed = false;
        updateExpressionDisplay();
        tvResult.setText(item.result);
        calculateResult(false);
        updateGraphyTabState();
        selectWorkspaceTab(0);
    }

    private void handleSignToggle() {
        if (isResultDisplayed) {
            String result = tvResult.getText().toString().replace(",", "");
            if (result.startsWith("-")) {
                expression = result.substring(1);
            } else {
                expression = "-" + result;
            }
            isResultDisplayed = false;
            updateExpressionDisplay();
            calculateResult(false);
            return;
        }

        if (expression.isEmpty()) {
            expression = "-";
            updateExpressionDisplay();
            return;
        }

        int end = expression.length();
        int start = findLastOperandStart(expression, end);
        if (start < 0) {
            return;
        }

        String operand = expression.substring(start, end);
        String negated = negateOperand(operand);
        expression = expression.substring(0, start) + negated;
        updateExpressionDisplay();
        calculateResult(false);
    }

    private int findLastOperandStart(@NonNull String expr, int end) {
        if (end == 0) {
            return -1;
        }
        int i = end - 1;
        if (expr.charAt(i) == ')') {
            int depth = 1;
            i--;
            while (i >= 0 && depth > 0) {
                if (expr.charAt(i) == ')') {
                    depth++;
                } else if (expr.charAt(i) == '(') {
                    depth--;
                }
                i--;
            }
            if (i >= 0 && expr.charAt(i) == '-') {
                char before = i > 0 ? expr.charAt(i - 1) : '\0';
                if (i == 0 || before == '(' || isOperatorChar(before) || before == '^') {
                    return i;
                }
            }
            return i + 1;
        }

        while (i >= 0) {
            char c = expr.charAt(i);
            if (Character.isDigit(c) || c == '.' || c == '%') {
                i--;
                continue;
            }
            break;
        }
        if (i >= 0 && expr.charAt(i) == '-') {
            char before = i > 0 ? expr.charAt(i - 1) : '\0';
            if (i == 0 || before == '(' || isOperatorChar(before) || before == '^') {
                return i;
            }
        }
        return i + 1;
    }

    @NonNull
    private String negateOperand(@NonNull String operand) {
        if (operand.startsWith("-")) {
            return operand.substring(1);
        }
        return "-" + operand;
    }

    private void handleBackspace() {
        if (isResultDisplayed) {
            expression = "";
            isResultDisplayed = false;
        } else if (!expression.isEmpty()) {
            if (expression.endsWith("sqrt(")) {
                expression = expression.substring(0, expression.length() - 5);
            } else {
                expression = expression.substring(0, expression.length() - 1);
            }
        }
        updateExpressionDisplay();
        calculateResult(false);
    }

    private void handleDotInput() {
        if (isResultDisplayed) {
            expression = "0.";
            isResultDisplayed = false;
        } else {
            if (expression.isEmpty()) {
                expression = "0.";
            } else {
                char lastChar = expression.charAt(expression.length() - 1);
                if (isOperatorChar(lastChar) || lastChar == '(') {
                    expression += "0.";
                } else if (Character.isDigit(lastChar)) {
                    // Check if current number already has a dot
                    int lastOpIndex = -1;
                    String ops = "+-×÷^%()";
                    for (int i = 0; i < ops.length(); i++) {
                        int idx = expression.lastIndexOf(ops.charAt(i));
                        if (idx > lastOpIndex) lastOpIndex = idx;
                    }
                    String lastNumber = expression.substring(lastOpIndex + 1);
                    if (!lastNumber.contains(".")) {
                        expression += ".";
                    }
                }
            }
        }
        updateExpressionDisplay();
    }

    private void handleInput(String input) {
        if (TextUtils.isEmpty(input)) return;

        if (isResultDisplayed) {
            if (isOperatorChar(input.charAt(0)) || input.equals("^") || input.equals("%")) {
                expression = tvResult.getText().toString().replace(",", "");
            } else {
                expression = "";
            }
            isResultDisplayed = false;
        }

        if (input.equals("√")) {
            // Implicit multiplication: 2√ -> 2*sqrt(
            if (!expression.isEmpty()) {
                char lastChar = expression.charAt(expression.length() - 1);
                if (Character.isDigit(lastChar) || lastChar == ')' || lastChar == '%') {
                    expression += "×";
                }
            }
            expression += "sqrt(";
        } else if (input.equals("(")) {
            if (!expression.isEmpty()) {
                char lastChar = expression.charAt(expression.length() - 1);
                if (Character.isDigit(lastChar) || lastChar == ')' || lastChar == '%' || lastChar == '.') {
                    expression += "×";
                }
            }
            expression += "(";
        } else if (input.equals(")")) {
            // Only allow ) if there's an open bracket
            int openCount = countOccurrences(expression, '(');
            int closeCount = countOccurrences(expression, ')');
            if (openCount > closeCount && !expression.isEmpty()) {
                char lastChar = expression.charAt(expression.length() - 1);
                if (Character.isDigit(lastChar) || lastChar == ')' || lastChar == '%' || lastChar == '.') {
                    expression += ")";
                }
            }
        } else if (isOperatorChar(input.charAt(0)) || input.equals("^") || input.equals("%")) {
            if (expression.isEmpty()) {
                if (input.equals("-")) expression = "-";
            } else {
                char lastChar = expression.charAt(expression.length() - 1);
                if (isOperatorChar(lastChar) || lastChar == '^') {
                    // Replace operator
                    expression = expression.substring(0, expression.length() - 1) + input;
                } else if (lastChar == '.') {
                    // 2.+ -> 2+
                    expression = expression.substring(0, expression.length() - 1) + input;
                } else if (lastChar != '(') {
                    expression += input;
                } else if (input.equals("-")) {
                    // Allow negative sign after parenthesis: (-
                    expression += "-";
                }
            }
        } else {
            // Number input
            if (!expression.isEmpty()) {
                char lastChar = expression.charAt(expression.length() - 1);
                if (lastChar == ')' || lastChar == '%') {
                    expression += "×";
                }
            }
            expression += input;
        }
        
        updateExpressionDisplay();
        calculateResult(false);
    }

    private int countOccurrences(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) count++;
        }
        return count;
    }

    private boolean isOperatorChar(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷';
    }

    private void updateExpressionDisplay() {
        if (tvExpression == null) {
            return;
        }
        if (isResultDisplayed || expression.isEmpty()) {
            tvExpression.setText("");
            GenericFunctions.resetTextSize(tvExpression, 21);
            return;
        }
        tvExpression.setText(
                ExpressionDisplayFormatter.formatSpannable(expression, mPrefs, semanticTheme)
        );
        GenericFunctions.adjustTextSize(tvExpression, 21);
    }

    private boolean hasGraphyContent() {
        return latestGraphyOutput != null && graphyRenderer.supports(latestGraphyOutput);
    }

    private void updateGraphyTabState() {
        TabLayout.Tab graphyTab = workspaceTabs.getTabAt(1);
        if (graphyTab == null) {
            return;
        }
        boolean enabled = hasGraphyContent();
        View tabView = graphyTab.view;
        tabView.setEnabled(enabled);
        tabView.setAlpha(enabled ? 1f : 0.4f);
    }

    private void selectWorkspaceTab(int index) {
        if (workspaceTabController != null) {
            workspaceTabController.selectTab(index);
        }
    }

    private void applyWorkspaceTab(int position) {
        if (onBackPressedCallback != null) {
            onBackPressedCallback.setEnabled(position != 0);
        }
        if (position == 1) {
            if (latestGraphyOutput != null && graphyRenderer.supports(latestGraphyOutput)) {
                showGraphy(latestGraphyOutput);
            } else {
                hideGraphy();
                if (graphyEmpty != null) {
                    graphyEmpty.setVisibility(View.VISIBLE);
                }
                if (graphyPanel != null) {
                    graphyPanel.setVisibility(View.GONE);
                }
                if (graphyContext != null) {
                    graphyContext.setText("");
                }
            }
        }
        if (position == 2) {
            loadHistory();
        }
    }

    private void loadHistory() {
        List<CalculationHistoryItem> items = dbHelper.getAllHistory();
        historyRows.clear();
        String lastSection = null;
        for (CalculationHistoryItem item : items) {
            String section = HistoryDateLabels.sectionLabel(requireContext(), item.createdAt);
            if (!section.equals(lastSection)) {
                historyRows.add(section);
                lastSection = section;
            }
            historyRows.add(item);
        }
        if (getView() != null) {
            getView().findViewById(R.id.tv_empty_history).setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
        historyAdapter.notifyDataSetChanged();
    }

    private void toggleHistory(boolean show) {
        selectWorkspaceTab(show ? 2 : 0);
    }

    private void calculateResult(boolean isFinal) {
        if (expression.isEmpty()) {
            tvResult.setText("0");
            return;
        }

        try {
            String sanitized = expression.replace('×', '*').replace('÷', '/').replace("√", "sqrt");
            
            // Auto-close parentheses for final calculation
            if (isFinal) {
                int openCount = countOccurrences(sanitized, '(');
                int closeCount = countOccurrences(sanitized, ')');
                while (openCount > closeCount) {
                    sanitized += ")";
                    openCount--;
                }
                // Trim trailing operators
                while (sanitized.length() > 0 && "+-*/^".indexOf(sanitized.charAt(sanitized.length() - 1)) != -1) {
                    sanitized = sanitized.substring(0, sanitized.length() - 1);
                }
            }

            EvaluationResult evaluationResult = ExpressionEvaluator.evaluate(sanitized);
            if (!evaluationResult.isValid()) {
                if (isFinal) showToast(getString(R.string.invalid_expression));
                return;
            }

            double res = evaluationResult.getValue();
            String formatted = formatResult(res);
            tvResult.setText(formatted);

            CalculationSnapshot snapshot = CalculationSnapshot.create(
                    CalculationSnapshot.BASIC_CALCULATOR_ID,
                    expression,
                    sanitized,
                    formatted,
                    evaluationResult
            );
            latestGraphyOutput = graphyBridge.build(snapshot);
            updateGraphyTabState();
            if (graphyVisible && latestGraphyOutput != null && graphyRenderer.supports(latestGraphyOutput)) {
                renderGraphy(latestGraphyOutput);
            }

            if (isFinal) {
                lastEvaluatedExpression = expression;
                dbHelper.addHistory(expression, formatted);
                expression = formatted.replace(",", "");
                isResultDisplayed = true;
                updateExpressionDisplay();
            }
        } catch (Exception e) {
            if (isFinal) {
                showToast(getString(R.string.invalid_expression));
            }
        }
    }

    private String formatResult(double value) {
        DecimalFormat df = new DecimalFormat("#,###.########");
        df.setMaximumFractionDigits(mPrefs.getNumberDecimals());
        return df.format(value);
    }

    private void copyExpressionAndResult() {
        String result = tvResult.getText().toString();
        if (result.equals("0") && expression.isEmpty() && lastEvaluatedExpression.isEmpty()) {
            return;
        }

        String exprPart;
        if (!expression.isEmpty() && !isResultDisplayed) {
            exprPart = expression;
        } else if (!lastEvaluatedExpression.isEmpty()) {
            exprPart = lastEvaluatedExpression;
        } else {
            exprPart = result;
        }

        String textToCopy = exprPart + " = " + result;
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.brand_name), textToCopy);
        clipboard.setPrimaryClip(clip);
        showToast(getString(R.string.toast_copied));
    }

    private void showToast(String message) {
        Snackbar.make(mCoordinatorLayout, message, Snackbar.LENGTH_SHORT).show();
    }

    GraphyOutput getLatestGraphyOutput() {
        return latestGraphyOutput;
    }

    @Override
    public void showGraphy(@NonNull GraphyOutput output) {
        if (!graphyRenderer.supports(output)) {
            return;
        }
        graphyVisible = true;
        if (graphyEmpty != null) {
            graphyEmpty.setVisibility(View.GONE);
        }
        if (graphyPanel != null) {
            graphyPanel.setVisibility(View.VISIBLE);
        }
        if (graphyContext != null) {
            graphyContext.setVisibility(View.GONE);
        }
        renderGraphy(output);
    }

    @Override
    public void hideGraphy() {
        graphyVisible = false;
        if (graphyPanelController != null) {
            graphyPanelController.update("", null);
        }
    }

    @Override
    public boolean isGraphyVisible() {
        return graphyVisible;
    }

    private void renderGraphy(@NonNull GraphyOutput output) {
        if (graphyPanelController == null) {
            return;
        }
        String expression = output.getExpression();
        if (expression == null) {
            expression = "";
        }
        graphyPanelController.update(expression, output);
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;

        private final List<Object> rows;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(CalculationHistoryItem item);
        }

        HistoryAdapter(List<Object> rows, OnItemClickListener listener) {
            this.rows = rows;
            this.listener = listener;
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                return new HeaderHolder(inflater.inflate(R.layout.item_history_header, parent, false));
            }
            return new ItemHolder(inflater.inflate(R.layout.item_history, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderHolder) {
                ((HeaderHolder) holder).title.setText((String) rows.get(position));
            } else {
                CalculationHistoryItem item = (CalculationHistoryItem) rows.get(position);
                ItemHolder itemHolder = (ItemHolder) holder;
                ExpressionDisplayFormatter.GraphySemanticTheme theme =
                        ExpressionDisplayFormatter.GraphySemanticTheme.from(itemHolder.itemView.getContext());
                itemHolder.tvExpression.setText(
                        ExpressionDisplayFormatter.formatSpannable(item.expression, Preferences.getInstance(itemHolder.itemView.getContext()), theme)
                );
                itemHolder.tvResult.setText(item.result);
                itemHolder.tvTime.setText(HistoryDateLabels.formatTime(item.createdAt));
                itemHolder.itemView.setOnClickListener(v -> listener.onItemClick(item));
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class HeaderHolder extends RecyclerView.ViewHolder {
            final TextView title;

            HeaderHolder(View v) {
                super(v);
                title = v.findViewById(R.id.tv_history_section);
            }
        }

        static class ItemHolder extends RecyclerView.ViewHolder {
            final TextView tvExpression;
            final TextView tvResult;
            final TextView tvTime;

            ItemHolder(View v) {
                super(v);
                tvExpression = v.findViewById(R.id.tv_history_expression);
                tvResult = v.findViewById(R.id.tv_history_result);
                tvTime = v.findViewById(R.id.tv_history_time);
            }
        }
    }
}
