package net.smartlogic.unitconverter.fragment;

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
import net.smartlogic.unitconverter.helper.DatabaseHelper;
import net.smartlogic.unitconverter.model.CalculationHistoryItem;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private final List<CalculationHistoryItem> historyList = new ArrayList<>();
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

        historyAdapter = new HistoryAdapter(historyList);
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

    private void refreshHistory() {
        historyList.clear();
        historyList.addAll(dbHelper.getAllHistory());
        historyAdapter.notifyDataSetChanged();
        emptyHistoryView.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void clearHistory() {
        dbHelper.clearHistory();
        refreshHistory();
        Snackbar.make(rootLayout, getString(R.string.history_cleared), Snackbar.LENGTH_SHORT).show();
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<CalculationHistoryItem> items;

        HistoryAdapter(List<CalculationHistoryItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CalculationHistoryItem item = items.get(position);
            holder.tvExpression.setText(item.expression);
            holder.tvResult.setText(item.result);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvExpression;
            final TextView tvResult;

            ViewHolder(View view) {
                super(view);
                tvExpression = view.findViewById(R.id.tv_history_expression);
                tvResult = view.findViewById(R.id.tv_history_result);
            }
        }
    }
}
