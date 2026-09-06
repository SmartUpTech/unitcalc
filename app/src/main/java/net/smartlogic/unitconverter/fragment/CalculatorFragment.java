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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.graphy.integration.CalculationSnapshot;
import net.smartlogic.unitconverter.graphy.integration.GraphyBridge;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.helper.DatabaseHelper;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.CalculationHistoryItem;
import net.smartlogic.unitconverter.utils.EvaluationResult;
import net.smartlogic.unitconverter.utils.ExpressionEvaluator;
import net.smartlogic.unitconverter.utils.GenericFunctions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CalculatorFragment extends Fragment implements View.OnClickListener {

    private TextView tvExpression, tvResult;
    private String expression = "";
    private Preferences mPrefs;
    private CoordinatorLayout mCoordinatorLayout;
    private boolean isResultDisplayed = false;
    
    private LinearLayout historyLayout;
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;
    private final List<CalculationHistoryItem> historyList = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private OnBackPressedCallback onBackPressedCallback;
    private final GraphyBridge graphyBridge = new GraphyBridge();
    private GraphyOutput latestGraphyOutput;

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
                toggleHistory(false);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);
    }

    private void initViews(View view) {
        tvExpression = view.findViewById(R.id.expression);
        tvResult = view.findViewById(R.id.result);
        mCoordinatorLayout = view.findViewById(R.id.cl);
        historyLayout = view.findViewById(R.id.history_layout);
        rvHistory = view.findViewById(R.id.rv_history);

        int[] ids = {
                R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four,
                R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine,
                R.id.dot, R.id.plus, R.id.minus, R.id.multiply, R.id.divide,
                R.id.equal, R.id.clear, R.id.backspace, R.id.copy,
                R.id.parenthesis_open, R.id.parenthesis_close, R.id.power, 
                R.id.sqrt, R.id.percent, R.id.btn_history,
                R.id.btn_close_history, R.id.btn_clear_history
        };

        for (int id : ids) {
            View v = view.findViewById(id);
            if (v != null) v.setOnClickListener(this);
        }

        historyAdapter = new HistoryAdapter(historyList, item -> {
            expression = item.result.replace(",", "");
            tvExpression.setText("");
            tvResult.setText(item.result);
            isResultDisplayed = true;
            toggleHistory(false);
        });
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(historyAdapter);

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

        tvResult.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                GenericFunctions.adjustTextSize(tvResult, 32);
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.clear) {
            expression = "";
            tvExpression.setText("");
            tvResult.setText("0");
            isResultDisplayed = false;
        } else if (id == R.id.backspace) {
            handleBackspace();
        } else if (id == R.id.copy) {
            copyToClipboard();
        } else if (id == R.id.equal) {
            calculateResult(true);
        } else if (id == R.id.btn_history) {
            toggleHistory(true);
        } else if (id == R.id.btn_close_history) {
            toggleHistory(false);
        } else if (id == R.id.btn_clear_history) {
            clearHistory();
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
        tvExpression.setText(expression);
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
        tvExpression.setText(expression);
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
        
        tvExpression.setText(expression);
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

    private void toggleHistory(boolean show) {
        historyLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        if (onBackPressedCallback != null) {
            onBackPressedCallback.setEnabled(show);
        }
        
        if (show) {
            historyList.clear();
            historyList.addAll(dbHelper.getAllHistory());
            if (getView() != null) {
                getView().findViewById(R.id.tv_empty_history).setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
            }
            historyAdapter.notifyDataSetChanged();
        }
    }

    private void clearHistory() {
        dbHelper.clearHistory();
        historyList.clear();
        historyAdapter.notifyDataSetChanged();
        if (getView() != null) {
            getView().findViewById(R.id.tv_empty_history).setVisibility(View.VISIBLE);
        }
        showToast(getString(R.string.history_cleared));
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

            if (isFinal) {
                dbHelper.addHistory(expression, formatted);
                expression = formatted.replace(",", "");
                tvExpression.setText("");
                isResultDisplayed = true;
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

    private void copyToClipboard() {
        String textToCopy = tvResult.getText().toString();
        if (textToCopy.equals("0") && expression.isEmpty()) {
            showToast(getString(R.string.toast_no_results_to_copy));
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Calculator Result", textToCopy);
        clipboard.setPrimaryClip(clip);
        showToast(getString(R.string.toast_result_copied));
    }

    private void showToast(String message) {
        Snackbar.make(mCoordinatorLayout, message, Snackbar.LENGTH_SHORT).show();
    }

    GraphyOutput getLatestGraphyOutput() {
        return latestGraphyOutput;
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<CalculationHistoryItem> items;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(CalculationHistoryItem item);
        }

        HistoryAdapter(List<CalculationHistoryItem> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CalculationHistoryItem item = items.get(position);
            holder.tvExpression.setText(item.expression);
            holder.tvResult.setText(item.result);
            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvExpression, tvResult;
            ViewHolder(View v) {
                super(v);
                tvExpression = v.findViewById(R.id.tv_history_expression);
                tvResult = v.findViewById(R.id.tv_history_result);
            }
        }
    }
}
