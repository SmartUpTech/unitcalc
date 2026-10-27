package net.smartlogic.unitconverter.fragment;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatDialog;
import com.google.android.material.textfield.TextInputLayout;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.InteractionFeedbackManager;
import net.smartlogic.unitconverter.theme.CalculatorTheme;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

/** Shared restorable selector. Results refer to original rows, never filtered positions. */
public final class ConverterPickerDialogFragment extends DialogFragment {
    public static final String RESULT_KEY = "converter_picker_result";
    public static final String INDEX = "index", SOURCE = "source", GROUP = "group";
    public static final String TAG = "converter_picker";
    private static final String TITLE = "title", LABELS = "labels", DETAILS = "details",
            KEYWORDS = "keywords", FLAGS = "flags", QUERY = "query", SCROLL = "scroll";
    private EditText search;
    private ListView list;
    private PickerAdapter adapter;
    private boolean selectionCommitted;
    private View dialogDecor;
    private ViewTreeObserver.OnGlobalLayoutListener visibleFrameListener;

    public static ConverterPickerDialogFragment newInstance(String title, boolean source,
            int group, int selected, ArrayList<String> labels, ArrayList<String> details,
            ArrayList<String> keywords, int[] flags) {
        ConverterPickerDialogFragment dialog = new ConverterPickerDialogFragment();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putBoolean(SOURCE, source);
        args.putInt(GROUP, group);
        args.putInt(INDEX, selected);
        args.putStringArrayList(LABELS, labels);
        args.putStringArrayList(DETAILS, details);
        args.putStringArrayList(KEYWORDS, keywords);
        args.putIntArray(FLAGS, flags);
        dialog.setArguments(args);
        return dialog;
    }

    public boolean showFor(Fragment parent) {
        var manager = parent.getChildFragmentManager();
        if (manager.isStateSaved() || manager.findFragmentByTag(TAG) != null) return false;
        showNow(manager, TAG);
        return true;
    }

    /** Reuse the same searchable, restorable picker for non-unit selections. */
    public static ConverterPickerDialogFragment forValues(String title, int group, int selected,
            ArrayList<String> labels, int searchHint) {
        ArrayList<String> details = new ArrayList<>();
        for (String ignored : labels) details.add("");
        var dialog = newInstance(title, false, group, selected, labels, details, labels, null);
        dialog.requireArguments().putInt("search_hint", searchHint);
        return dialog;
    }

    @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = requireArguments();
        View root = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_converter_picker, null);
        CalculatorTheme theme = ThemeManager.get();
        TextView title = root.findViewById(R.id.converter_picker_title);
        title.setText(args.getString(TITLE));
        title.setTextColor(theme.primaryContentColor());
        ImageView close = root.findViewById(R.id.converter_picker_close);
        close.setImageTintList(ColorStateList.valueOf(theme.primaryContentColor()));
        close.setOnClickListener(v -> dismiss());
        TextInputLayout searchBox = root.findViewById(R.id.converter_picker_search_box);
        boolean currency = args.getIntArray(FLAGS) != null;
        searchBox.setHint(getString(args.getInt("search_hint", currency ? R.string.converter_picker_search_currencies
                : R.string.converter_picker_search_units)));
        searchBox.setBoxStrokeColorStateList(new ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}},
                new int[]{theme.primaryContentColor(), theme.secondaryContentColor()}));
        searchBox.setDefaultHintTextColor(ColorStateList.valueOf(theme.secondaryContentColor()));
        searchBox.setHintTextColor(ColorStateList.valueOf(theme.secondaryContentColor()));
        searchBox.setStartIconTintList(ColorStateList.valueOf(theme.secondaryContentColor()));
        searchBox.setEndIconTintList(ColorStateList.valueOf(theme.secondaryContentColor()));
        search = root.findViewById(R.id.converter_picker_search);
        search.setTextColor(theme.primaryContentColor());
        search.setSelectAllOnFocus(false);
        list = root.findViewById(R.id.converter_picker_list);
        TextView empty = root.findViewById(R.id.converter_picker_empty);
        empty.setTextColor(theme.secondaryContentColor());
        list.setEmptyView(empty);
        adapter = new PickerAdapter(args);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (selectionCommitted) return;
            selectionCommitted = true;
            Bundle result = new Bundle();
            result.putInt(INDEX, adapter.originalIndex(position));
            result.putBoolean(SOURCE, args.getBoolean(SOURCE));
            result.putInt(GROUP, args.getInt(GROUP));
            InteractionFeedbackManager.perform(view, InteractionFeedbackManager.Type.ACTION);
            ((InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(search.getWindowToken(), 0);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            dismiss();
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.search(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        if (savedInstanceState != null) {
            search.setText(savedInstanceState.getString(QUERY, ""));
            search.setSelection(search.length());
            Parcelable scroll = savedInstanceState.getParcelable(SCROLL);
            if (scroll != null) list.onRestoreInstanceState(scroll);
        } else {
            list.setSelection(Math.max(0, args.getInt(INDEX)));
        }
        root.requestFocus();
        AppCompatDialog dialog = new AppCompatDialog(requireContext(), R.style.GraphyAlertDialogTheme);
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(ThemeApplier.roundedFill(theme.background(),
                    getResources().getDimension(R.dimen.converter_picker_corner)));
        }
        return dialog;
    }

    @Override public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window == null) return;
        var metrics = getResources().getDisplayMetrics();
        int margin = getResources().getDimensionPixelSize(R.dimen.converter_picker_margin);
        int width = Math.min(metrics.widthPixels - 2 * margin,
                getResources().getDimensionPixelSize(R.dimen.converter_picker_max_width));
        int height = Math.min(Math.round(metrics.heightPixels * getResources().getFraction(
                R.fraction.converter_picker_height_fraction, 1, 1)),
                getResources().getDimensionPixelSize(R.dimen.converter_picker_max_height));
        window.setLayout(width, height);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        window.setTitle(requireArguments().getString(TITLE));
        removeVisibleFrameListener();
        dialogDecor = window.getDecorView();
        Rect visibleFrame = new Rect();
        // Floating windows may pan instead of resizing for the IME. Bound the
        // whole dialog to the visible frame so its list never extends behind it.
        visibleFrameListener = () -> {
            dialogDecor.getWindowVisibleDisplayFrame(visibleFrame);
            if (visibleFrame.height() <= 2 * margin) return;
            int available = Math.min(height, visibleFrame.height() - 2 * margin);
            if (window.getAttributes().height != available) window.setLayout(width, available);
        };
        dialogDecor.getViewTreeObserver().addOnGlobalLayoutListener(visibleFrameListener);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (search != null) outState.putString(QUERY, search.getText().toString());
        if (list != null) outState.putParcelable(SCROLL, list.onSaveInstanceState());
    }

    @Override public void onDestroyView() {
        removeVisibleFrameListener();
        search = null;
        list = null;
        adapter = null;
        super.onDestroyView();
    }

    private void removeVisibleFrameListener() {
        if (dialogDecor != null && visibleFrameListener != null) {
            dialogDecor.getViewTreeObserver().removeOnGlobalLayoutListener(visibleFrameListener);
        }
        dialogDecor = null;
        visibleFrameListener = null;
    }

    static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).trim();
    }

    private final class PickerAdapter extends BaseAdapter {
        private final ArrayList<String> labels, details, searchable = new ArrayList<>();
        private final ArrayList<Integer> visible = new ArrayList<>();
        private final int[] flags;
        private final int selected;

        PickerAdapter(Bundle args) {
            labels = args.getStringArrayList(LABELS);
            details = args.getStringArrayList(DETAILS);
            ArrayList<String> keywords = args.getStringArrayList(KEYWORDS);
            flags = args.getIntArray(FLAGS);
            selected = args.getInt(INDEX);
            for (int i = 0; i < labels.size(); i++) {
                searchable.add(normalize(labels.get(i) + " " + details.get(i) + " " + keywords.get(i)));
            }
            search("");
        }

        void search(String query) {
            String[] terms = normalize(query).split("\\s+");
            visible.clear();
            for (int i = 0; i < labels.size(); i++) {
                boolean match = true;
                for (String term : terms) if (!searchable.get(i).contains(term)) { match = false; break; }
                if (match) visible.add(i);
            }
            notifyDataSetChanged();
        }

        int originalIndex(int position) { return visible.get(position); }
        @Override public int getCount() { return visible.size(); }
        @Override public Object getItem(int position) { return labels.get(originalIndex(position)); }
        @Override public long getItemId(int position) { return originalIndex(position); }
        @Override public boolean hasStableIds() { return true; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView != null ? convertView : LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_converter_picker, parent, false);
            int index = originalIndex(position);
            CalculatorTheme theme = ThemeManager.get();
            boolean checked = index == selected;
            int surface = checked ? ColorUtils.compositeColors(ColorUtils.setAlphaComponent(theme.primaryContentColor(),
                    getResources().getInteger(R.integer.converter_picker_selected_alpha)), theme.background())
                    : theme.background();
            TextView name = row.findViewById(R.id.converter_picker_name);
            name.setText(labels.get(index));
            name.setTextColor(CalculatorTheme.readableColor(theme.primaryContentColor(), surface));
            TextView detail = row.findViewById(R.id.converter_picker_detail);
            detail.setText(details.get(index));
            detail.setTextColor(CalculatorTheme.readableColor(theme.secondaryContentColor(), surface));
            detail.setVisibility(details.get(index).isEmpty() ? View.GONE : View.VISIBLE);
            ImageView flag = row.findViewById(R.id.converter_picker_flag);
            flag.setVisibility(flags == null ? View.GONE : View.VISIBLE);
            if (flags != null) flag.setImageResource(flags[index]);
            ImageView check = row.findViewById(R.id.converter_picker_check);
            check.setVisibility(checked ? View.VISIBLE : View.INVISIBLE);
            check.setImageTintList(ColorStateList.valueOf(CalculatorTheme.readableColor(theme.primaryContentColor(), surface)));
            row.setSelected(checked);
            String label = labels.get(index) + (details.get(index).isEmpty() ? "" : ", " + details.get(index));
            row.setContentDescription(checked ? getString(R.string.converter_picker_selected, label) : label);
            row.setBackground(new RippleDrawable(ContextCompat.getColorStateList(requireContext(),
                    R.color.pad_button_ripple_color), ThemeApplier.roundedFill(surface,
                    getResources().getDimension(R.dimen.converter_picker_corner)), null));
            return row;
        }
    }
}
