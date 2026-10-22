package net.smartlogic.unitconverter.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.model.CalculationHistoryItem;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;
import java.text.DateFormat;
import java.util.Date;

/** Restorable actions, full-value details and explicit clear-all confirmation. */
public class HistoryDialogFragment extends DialogFragment {
    public static final String RESULT_KEY = "history_action", ACTION = "action";
    public static final String OPTIONS = "options", DETAILS = "details", CLEAR = "clear";
    public static final String VIEW = "view", COPY_RESULT = "copy_result", COPY_CALCULATION = "copy_calculation",
            DELETE = "delete", REUSE = "reuse";

    public static HistoryDialogFragment create(String mode, CalculationHistoryItem item) {
        Bundle args = item == null ? new Bundle() : bundle(item);
        args.putString("mode", mode);
        HistoryDialogFragment dialog = new HistoryDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    public static Bundle bundle(CalculationHistoryItem item) {
        Bundle result = new Bundle();
        result.putString("expression", item.expression());
        result.putString("result", item.result());
        result.putString("calculator", item.calculatorId());
        result.putLong("created", item.createdAt());
        result.putLong("id", item.id());
        return result;
    }

    public static CalculationHistoryItem item(Bundle args) {
        return new CalculationHistoryItem(args.getString("expression", ""), args.getString("result", ""),
                args.getLong("created"), args.getString("calculator", CalculatorCatalog.ID_BASIC), args.getLong("id", -1));
    }

    private void send(String action) {
        Bundle result = new Bundle(requireArguments());
        result.putString(ACTION, action);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
    }

    @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        var theme = ThemeManager.get();
        var builder = new MaterialAlertDialogBuilder(requireContext(), R.style.GraphyAlertDialogTheme)
                .setBackground(ThemeApplier.roundedFill(theme.background(), getResources().getDimension(R.dimen.converter_picker_corner)));
        String mode = requireArguments().getString("mode", DETAILS);
        if (CLEAR.equals(mode)) {
            builder.setTitle(R.string.history_clear_all).setMessage(R.string.history_clear_question)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.history_clear_all, (dialog, which) -> send(CLEAR));
        } else if (OPTIONS.equals(mode)) {
            int[] labels = {R.string.history_view_full, R.string.history_copy_result,
                    R.string.history_copy_calculation, R.string.history_delete};
            String[] actions = {VIEW, COPY_RESULT, COPY_CALCULATION, DELETE};
            CharSequence[] titles = new CharSequence[labels.length];
            for (int i = 0; i < labels.length; i++) {
                SpannableString title = new SpannableString(getString(labels[i]));
                title.setSpan(new ForegroundColorSpan(theme.mainText()), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                titles[i] = title;
            }
            builder.setTitle(R.string.history_more).setItems(titles, (dialog, which) -> send(actions[which]));
        } else {
            CalculationHistoryItem item = item(requireArguments());
            View content = getLayoutInflater().inflate(R.layout.dialog_history_details, null);
            String calculator = CalculatorCatalog.titleForId(requireContext(), item.calculatorId());
            String date = item.createdAt() <= 0 ? "" : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(new Date(item.createdAt()));
            ((TextView) content.findViewById(R.id.tv_history_time)).setText(date.isEmpty() ? calculator
                    : getString(R.string.history_metadata, calculator, date));
            ((TextView) content.findViewById(R.id.tv_history_expression)).setText(item.expression());
            ((TextView) content.findViewById(R.id.tv_history_result)).setText(item.result());
            boolean reusable = CalculatorCatalog.ID_BASIC.equals(item.calculatorId());
            content.findViewById(R.id.history_legacy_hint).setVisibility(reusable ? View.GONE : View.VISIBLE);
            ThemeApplier.apply(content);
            builder.setTitle(R.string.history_details).setView(content)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(R.string.history_copy_result, (dialog, which) -> send(COPY_RESULT))
                    .setPositiveButton(reusable ? R.string.history_reuse : R.string.history_open_tool,
                            (dialog, which) -> send(REUSE));
        }
        return builder.create();
    }

    @Override public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) requireDialog();
        var theme = ThemeManager.get();
        TextView title = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        TextView message = dialog.findViewById(android.R.id.message);
        if (title != null) title.setTextColor(theme.mainText());
        if (message != null) message.setTextColor(theme.mainText());
        for (int button : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL}) {
            if (dialog.getButton(button) != null) dialog.getButton(button).setTextColor(theme.mainText());
        }
    }
}
