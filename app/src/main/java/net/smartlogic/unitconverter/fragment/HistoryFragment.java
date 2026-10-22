package net.smartlogic.unitconverter.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.activity.MainActivity;
import net.smartlogic.unitconverter.helper.DatabaseHelper;
import net.smartlogic.unitconverter.helper.HistoryDateLabels;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.CalculationHistoryItem;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.utils.ExpressionDisplayFormatter;
import java.util.ArrayList;
import java.util.List;

/** History is a secondary, scrollable page. Database work is serialized off the UI thread. */
public class HistoryFragment extends Fragment {
    public interface HistorySelectionListener {
        void onHistoryItemSelected(@NonNull CalculationHistoryItem item);
    }
    private static final int PAGE_SIZE = 50;
    private static final String DIALOG_TAG = "history_dialog";
    private final List<CalculationHistoryItem> items = new ArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private DatabaseHelper dbHelper;
    private HistoryAdapter adapter;
    private RecyclerView list;
    private CoordinatorLayout root;
    private View emptyGroup, hint, progress;
    private boolean loading, hasMore = true, loadFailed;
    private int version, viewEpoch;
    private CalculationHistoryItem pendingUndo;
    private Snackbar undoSnackbar;
    private Parcelable scrollState;
    private int restoreCount;

    public static HistoryFragment newInstance() { return new HistoryFragment(); }

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        dbHelper = new DatabaseHelper(requireContext().getApplicationContext());
        if (state != null) {
            scrollState = state.getParcelable("scroll");
            restoreCount = state.getInt("loaded", PAGE_SIZE);
            Bundle undo = state.getBundle("undo");
            if (undo != null) pendingUndo = HistoryDialogFragment.item(undo);
        }
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_history, parent, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        root = view.findViewById(R.id.history_root);
        emptyGroup = view.findViewById(R.id.history_empty_group);
        hint = view.findViewById(R.id.history_reuse_hint);
        progress = view.findViewById(R.id.history_loading);
        list = view.findViewById(R.id.rv_history);
        adapter = new HistoryAdapter(this::onHistoryItemSelected, item -> showDialog(HistoryDialogFragment.OPTIONS, item));
        adapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView recycler, int dx, int dy) {
                LinearLayoutManager manager = (LinearLayoutManager) recycler.getLayoutManager();
                if (dy >= 0 && manager.findLastVisibleItemPosition() >= adapter.getItemCount() - 6) loadPage(false);
            }
        });
        view.findViewById(R.id.history_start).setOnClickListener(v -> {
            if (loadFailed) loadPage(true);
            else if (getActivity() instanceof MainActivity activity) activity.navigateToEntry(CalculatorCatalog.getById(CalculatorCatalog.ID_BASIC));
        });
        getChildFragmentManager().setFragmentResultListener(HistoryDialogFragment.RESULT_KEY, getViewLifecycleOwner(),
                (key, result) -> handleAction(result));
        ThemeApplier.apply(view);
    }

    @Override public void onResume() {
        super.onResume();
        if (!isHidden()) loadPage(true);
        ThemeApplier.apply(getView());
    }

    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden && undoSnackbar != null) undoSnackbar.dismiss();
        if (!hidden && isResumed() && root != null) loadPage(true);
    }

    public boolean hasHistory() { return !items.isEmpty(); }
    public void showClearHistoryConfirmation() {
        if (hasHistory()) showDialog(HistoryDialogFragment.CLEAR, null);
    }

    private void showDialog(String mode, CalculationHistoryItem item) {
        if (!isAdded() || getChildFragmentManager().isStateSaved()) return;
        if (getChildFragmentManager().findFragmentByTag(DIALOG_TAG) == null) {
            HistoryDialogFragment.create(mode, item).show(getChildFragmentManager(), DIALOG_TAG);
        }
    }

    private void onHistoryItemSelected(CalculationHistoryItem item) {
        if (!CalculatorCatalog.ID_BASIC.equals(item.calculatorId())) {
            showDialog(HistoryDialogFragment.DETAILS, item);
        } else reuse(item);
    }

    private void reuse(CalculationHistoryItem item) {
        if (getActivity() instanceof MainActivity activity) activity.restoreFromHistory(item);
        else if (getParentFragment() instanceof HistorySelectionListener listener) listener.onHistoryItemSelected(item);
    }

    private void handleAction(Bundle args) {
        CalculationHistoryItem item = HistoryDialogFragment.item(args);
        switch (args.getString(HistoryDialogFragment.ACTION, "")) {
            case HistoryDialogFragment.VIEW -> {
                if (!isAdded() || getChildFragmentManager().isStateSaved()) return;
                Fragment previous = getChildFragmentManager().findFragmentByTag(DIALOG_TAG);
                if (previous instanceof androidx.fragment.app.DialogFragment dialog) dialog.dismissNow();
                showDialog(HistoryDialogFragment.DETAILS, item);
            }
            case HistoryDialogFragment.REUSE -> reuse(item);
            case HistoryDialogFragment.COPY_RESULT -> copy(item.result());
            case HistoryDialogFragment.COPY_CALCULATION -> copy(getString(R.string.history_copy_content, item.expression(), item.result()));
            case HistoryDialogFragment.DELETE -> delete(item);
            case HistoryDialogFragment.CLEAR -> clearHistory();
            default -> { }
        }
    }

    private void copy(String value) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.history_details), value));
        Snackbar.make(root, R.string.history_copied, Snackbar.LENGTH_SHORT).show();
    }

    private void loadPage(boolean reset) {
        if (root == null || !isAdded()) return;
        if (reset) {
            restoreCount = Math.max(restoreCount, items.size());
            if (scrollState == null && list != null) scrollState = list.getLayoutManager().onSaveInstanceState();
            version++;
            loading = false;
            hasMore = true;
            items.clear();
        }
        if (loading || !hasMore) return;
        loading = true;
        loadFailed = false;
        progress.setVisibility(View.VISIBLE);
        long before = items.isEmpty() ? Long.MAX_VALUE : items.get(items.size() - 1).id();
        int request = version, epoch = viewEpoch;
        DatabaseHelper.executeHistoryWork(() -> {
            try {
                List<CalculationHistoryItem> page = dbHelper.getHistoryPage(before, PAGE_SIZE);
                main.post(() -> {
                    if (root == null || epoch != viewEpoch || request != version) return;
                    loading = false;
                    hasMore = page.size() == PAGE_SIZE;
                    items.addAll(page);
                    publishRows();
                    if (pendingUndo != null) showUndo();
                });
            } catch (RuntimeException failure) {
                main.post(() -> {
                    if (root == null || epoch != viewEpoch || request != version) return;
                    loading = false;
                    loadFailed = true;
                    updateState();
                    Snackbar.make(root, R.string.history_loading_error, Snackbar.LENGTH_LONG)
                            .setAction(R.string.history_retry, v -> loadPage(true)).show();
                });
            }
        });
    }

    private void publishRows() {
        List<Object> rows = new ArrayList<>();
        String section = null;
        for (CalculationHistoryItem item : items) {
            String next = HistoryDateLabels.sectionLabel(requireContext(), item.createdAt());
            if (!next.equals(section)) { rows.add(next); section = next; }
            rows.add(item);
        }
        int epoch = viewEpoch;
        adapter.submitList(rows, () -> {
            if (root == null || epoch != viewEpoch) return;
            if (hasMore && items.size() < restoreCount) {
                loadPage(false);
            } else if (scrollState != null && !items.isEmpty()) {
                list.getLayoutManager().onRestoreInstanceState(scrollState);
                scrollState = null;
                restoreCount = 0;
            }
        });
        updateState();
    }

    private void updateState() {
        progress.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
        emptyGroup.setVisibility(!loading && items.isEmpty() ? View.VISIBLE : View.GONE);
        hint.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.history_empty_description).setVisibility(loadFailed ? View.GONE : View.VISIBLE);
        ((TextView) root.findViewById(R.id.tv_empty_history)).setText(loadFailed ? R.string.history_loading_error : R.string.no_history);
        ((TextView) root.findViewById(R.id.history_start)).setText(loadFailed ? R.string.history_retry : R.string.history_start);
        if (getActivity() != null) getActivity().invalidateOptionsMenu();
    }

    private void delete(CalculationHistoryItem item) {
        if (item.id() < 0) return;
        if (undoSnackbar != null) undoSnackbar.dismiss();
        pendingUndo = item;
        version++;
        loading = false;
        DatabaseHelper.executeHistoryWork(() -> {
            boolean deleted;
            try { deleted = dbHelper.deleteHistoryItem(item.id()); }
            catch (RuntimeException failure) { deleted = false; }
            boolean success = deleted;
            main.post(() -> {
                if (!success && pendingUndo != null && pendingUndo.id() == item.id()) pendingUndo = null;
                if (root == null) return;
                if (!success) Snackbar.make(root, R.string.history_error, Snackbar.LENGTH_LONG).show();
                loadPage(true);
            });
        });
    }

    private void showUndo() {
        if (pendingUndo == null || isHidden()) return;
        if (undoSnackbar != null && undoSnackbar.isShownOrQueued()) return;
        CalculationHistoryItem deleted = pendingUndo;
        int epoch = viewEpoch;
        undoSnackbar = Snackbar.make(root, R.string.history_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.history_undo, v -> {
                    pendingUndo = null;
                    DatabaseHelper.executeHistoryWork(() -> {
                        boolean restored;
                        try { restored = dbHelper.restoreHistoryItem(deleted); }
                        catch (RuntimeException failure) { restored = false; }
                        boolean success = restored;
                        main.post(() -> {
                            if (root == null) return;
                            if (!success) Snackbar.make(root, R.string.history_error, Snackbar.LENGTH_LONG).show();
                            loadPage(true);
                        });
                    });
                }).addCallback(new Snackbar.Callback() {
                    @Override public void onDismissed(Snackbar bar, int event) {
                        if (epoch == viewEpoch && !isHidden() && pendingUndo != null && pendingUndo.id() == deleted.id()
                                && event != DISMISS_EVENT_ACTION) pendingUndo = null;
                    }
                });
        undoSnackbar.show();
    }

    private void clearHistory() {
        pendingUndo = null;
        if (undoSnackbar != null) undoSnackbar.dismiss();
        version++;
        loading = false;
        DatabaseHelper.executeHistoryWork(() -> {
            boolean cleared;
            try { dbHelper.clearHistory(); cleared = true; }
            catch (RuntimeException failure) { cleared = false; }
            boolean success = cleared;
            main.post(() -> {
                if (root == null) return;
                Snackbar.make(root, success ? R.string.history_cleared : R.string.history_error, Snackbar.LENGTH_LONG).show();
                loadPage(true);
            });
        });
    }

    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (list != null) out.putParcelable("scroll", list.getLayoutManager().onSaveInstanceState());
        out.putInt("loaded", items.size());
        if (pendingUndo != null) out.putBundle("undo", HistoryDialogFragment.bundle(pendingUndo));
    }

    @Override public void onDestroyView() {
        viewEpoch++;
        version++;
        loading = false;
        if (list != null) {
            scrollState = list.getLayoutManager().onSaveInstanceState();
            list.setAdapter(null);
        }
        if (undoSnackbar != null) undoSnackbar.dismiss();
        undoSnackbar = null;
        root = null;
        list = null;
        adapter = null;
        emptyGroup = hint = progress = null;
        super.onDestroyView();
    }

    private interface ItemAction { void run(CalculationHistoryItem item); }

    private static final class HistoryAdapter extends ListAdapter<Object, RecyclerView.ViewHolder> {
        private final ItemAction reuse, options;
        HistoryAdapter(ItemAction reuse, ItemAction options) {
            super(new DiffUtil.ItemCallback<>() {
                @Override public boolean areItemsTheSame(@NonNull Object a, @NonNull Object b) {
                    if (a instanceof CalculationHistoryItem x && b instanceof CalculationHistoryItem y) return x.id() == y.id();
                    return a instanceof String && a.equals(b);
                }
                @Override public boolean areContentsTheSame(@NonNull Object a, @NonNull Object b) { return a.equals(b); }
            });
            this.reuse = reuse;
            this.options = options;
        }
        @Override public int getItemViewType(int position) { return getItem(position) instanceof String ? 0 : 1; }
        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View view = LayoutInflater.from(parent.getContext()).inflate(type == 0 ? R.layout.item_history_header : R.layout.item_history, parent, false);
            return new RecyclerView.ViewHolder(view) { };
        }
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object row = getItem(position);
            View view = holder.itemView;
            if (row instanceof String title) ((TextView) view).setText(title);
            else {
                CalculationHistoryItem item = (CalculationHistoryItem) row;
                Context context = view.getContext();
                String calculator = CalculatorCatalog.titleForId(context, item.calculatorId());
                String time = HistoryDateLabels.formatTime(item.createdAt());
                ((TextView) view.findViewById(R.id.tv_history_calculator)).setText(time.isEmpty() ? calculator
                        : context.getString(R.string.history_metadata, calculator, time));
                ((TextView) view.findViewById(R.id.tv_history_expression)).setText(ExpressionDisplayFormatter.formatSpannable(
                        item.expression(), Preferences.getInstance(context), ExpressionDisplayFormatter.GraphySemanticTheme.from(context)));
                ((TextView) view.findViewById(R.id.tv_history_result)).setText(item.result());
                view.setOnClickListener(v -> reuse.run(item));
                view.findViewById(R.id.history_item_more).setOnClickListener(v -> options.run(item));
            }
            ThemeApplier.apply(view);
        }
    }
}
