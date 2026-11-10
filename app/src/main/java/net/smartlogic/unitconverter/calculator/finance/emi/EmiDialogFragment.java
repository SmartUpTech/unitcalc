package net.smartlogic.unitconverter.calculator.finance.emi;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;

/** Restorable menus and user-reviewed text; no financial values enter diagnostic output. */
public final class EmiDialogFragment extends DialogFragment {
    public static final String TAG = "emi_dialog", RESULT = "emi_choice";
    public static EmiDialogFragment choices(int title, int... actions) {
        EmiDialogFragment d = info(title, null); d.requireArguments().putIntArray("actions", actions); return d;
    }
    public static EmiDialogFragment info(int title, String message) {
        EmiDialogFragment d = new EmiDialogFragment(); Bundle a = new Bundle(); a.putInt("title", title); a.putString("message", message); d.setArguments(a); return d;
    }
    public static EmiDialogFragment confirm(int title, String message, int action) {
        EmiDialogFragment d = info(title, message); d.requireArguments().putInt("confirm", action); return d;
    }
    public static EmiDialogFragment infoWithAction(int title, String message, int action) {
        EmiDialogFragment d = info(title, message); d.requireArguments().putInt("neutral", action); return d;
    }
    @NonNull @Override public Dialog onCreateDialog(Bundle state) {
        var theme = ThemeManager.get(); Bundle a = requireArguments();
        var builder = new MaterialAlertDialogBuilder(requireContext(), R.style.GraphyAlertDialogTheme)
                .setBackground(ThemeApplier.roundedFill(theme.background(), getResources().getDimension(R.dimen.converter_picker_corner)))
                .setTitle(a.getInt("title"));
        int[] actions = a.getIntArray("actions");
        if (actions != null) {
            CharSequence[] labels = new CharSequence[actions.length];
            for (int i = 0; i < actions.length; i++) {
                var label = new android.text.SpannableString(getString(actions[i]));
                label.setSpan(new android.text.style.ForegroundColorSpan(theme.primaryContentColor()), 0, label.length(), 0); labels[i] = label;
            }
            builder.setItems(labels, (dialog, which) -> send(actions[which])).setNegativeButton(android.R.string.cancel, null);
        } else {
            builder.setMessage(a.getString("message"));
            if (a.containsKey("confirm")) builder.setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (d, which) -> send(a.getInt("confirm")));
            else builder.setPositiveButton(android.R.string.ok, null);
            if (a.containsKey("neutral")) builder.setNeutralButton(a.getInt("neutral"),
                    (d, which) -> send(a.getInt("neutral")));
        }
        return builder.create();
    }
    private void send(int action) {
        var manager = getParentFragmentManager();
        if (manager.isStateSaved()) return;
        Bundle result = new Bundle(); result.putInt("action", action);
        dismiss(); manager.executePendingTransactions();
        manager.setFragmentResult(RESULT, result);
    }
    @Override public void onStart() { super.onStart(); ThemeApplier.applyDialog((AlertDialog) requireDialog()); }
}
