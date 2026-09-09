package net.smartlogic.unitconverter.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.activity.MainActivity;
import net.smartlogic.unitconverter.helper.DatabaseHelper;
import net.smartlogic.unitconverter.helper.HistoryDateLabels;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.CalculationHistoryItem;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.utils.ExpressionDisplayFormatter;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    public interface HistorySelectionListener {
        void onHistoryItemSelected(@NonNull CalculationHistoryItem item);
    }

    private final List<Object> historyRows = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private HistoryAdapter historyAdapter;
    private TextView emptyHistoryView;
    private CoordinatorLayout rootLayout;

    public HistoryFragment() {
    }

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootLayout = view.findViewById(R.id.history_root);
        emptyHistoryView = view.findViewById(R.id.tv_empty_history);
        RecyclerView recyclerView = view.findViewById(R.id.rv_history);

        historyAdapter = new HistoryAdapter(historyRows, this::onHistoryItemSelected);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(historyAdapter);

        view.findViewById(R.id.btn_clear_history).setOnClickListener(v -> clearHistory());
        refreshHistory();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHistory();
    }

    private void onHistoryItemSelected(@NonNull CalculationHistoryItem item) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).restoreFromHistory(item);
            return;
        }
        if (getParentFragment() instanceof HistorySelectionListener) {
            ((HistorySelectionListener) getParentFragment()).onHistoryItemSelected(item);
        }
    }

    private void refreshHistory() {
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
        historyAdapter.notifyDataSetChanged();
        emptyHistoryView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void clearHistory() {
        dbHelper.clearHistory();
        refreshHistory();
        Snackbar.make(rootLayout, getString(R.string.history_cleared), Snackbar.LENGTH_SHORT).show();
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;

        private final List<Object> rows;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(@NonNull CalculationHistoryItem item);
        }

        HistoryAdapter(@NonNull List<Object> rows, @NonNull OnItemClickListener listener) {
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
                return;
            }

            CalculationHistoryItem item = (CalculationHistoryItem) rows.get(position);
            ItemHolder itemHolder = (ItemHolder) holder;
            Context context = itemHolder.itemView.getContext();
            Preferences prefs = Preferences.getInstance(context);
            ExpressionDisplayFormatter.GraphySemanticTheme theme =
                    ExpressionDisplayFormatter.GraphySemanticTheme.from(context);

            itemHolder.tvCalculator.setText(CalculatorCatalog.titleForId(context, item.calculatorId));
            itemHolder.tvTime.setText(HistoryDateLabels.formatTime(item.createdAt));
            itemHolder.tvExpression.setText(
                    ExpressionDisplayFormatter.formatSpannable(item.expression, prefs, theme)
            );
            itemHolder.tvResult.setText(item.result);
            itemHolder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class HeaderHolder extends RecyclerView.ViewHolder {
            final TextView title;

            HeaderHolder(@NonNull View view) {
                super(view);
                title = view.findViewById(R.id.tv_history_section);
            }
        }

        static class ItemHolder extends RecyclerView.ViewHolder {
            final TextView tvCalculator;
            final TextView tvTime;
            final TextView tvExpression;
            final TextView tvResult;

            ItemHolder(@NonNull View view) {
                super(view);
                tvCalculator = view.findViewById(R.id.tv_history_calculator);
                tvTime = view.findViewById(R.id.tv_history_time);
                tvExpression = view.findViewById(R.id.tv_history_expression);
                tvResult = view.findViewById(R.id.tv_history_result);
            }
        }
    }
}
