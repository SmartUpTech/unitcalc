package net.smartlogic.unitconverter.age;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;
import java.time.LocalDate;
import java.time.ZoneId;

/** Restorable date/calendar choices. Birth dates remain local to the tool session. */
public final class AgeDialogFragment extends DialogFragment {
    public static final String TAG = "age_dialog", RESULT = "age_choice";
    public static final String BIRTH = "birth", AS_OF = "asof", CALENDAR = "calendar";
    private DatePicker picker;

    public static AgeDialogFragment create(String mode, LocalDate date) {
        AgeDialogFragment dialog = new AgeDialogFragment();
        Bundle args = new Bundle();
        args.putString("mode", mode);
        args.putString("date", date.toString());
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull @Override public Dialog onCreateDialog(Bundle state) {
        var theme = ThemeManager.get();
        var builder = new MaterialAlertDialogBuilder(requireContext(), R.style.GraphyAlertDialogTheme)
                .setBackground(ThemeApplier.roundedFill(theme.background(), getResources().getDimension(R.dimen.converter_picker_corner)))
                .setNegativeButton(android.R.string.cancel, null);
        String mode = requireArguments().getString("mode");
        LocalDate date = LocalDate.parse(requireArguments().getString("date"));
        if (CALENDAR.equals(mode)) {
            LocalDate next = AgeCalculator.nextBirthday(date, LocalDate.now());
            builder.setTitle(R.string.age_add_calendar)
                    .setMessage(getString(R.string.age_calendar_question, new AgeFormatter(requireContext()).date(next)))
                    .setNeutralButton(R.string.age_calendar_once, (d, which) -> send(mode, date, false))
                    .setPositiveButton(R.string.age_calendar_yearly, (d, which) -> send(mode, date, true));
        } else {
            LocalDate initial = state == null ? date : LocalDate.parse(state.getString("selected", date.toString()));
            picker = new DatePicker(new ContextThemeWrapper(requireContext(), R.style.AgeDatePickerTheme));
            picker.setCalendarViewShown(false);
            picker.setMinDate(AgeCalculator.MIN_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
            picker.setMaxDate(AgeCalculator.MAX_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
            picker.init(initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth(), null);
            picker.setBackgroundColor(theme.background());
            stylePicker(picker);
            builder.setTitle(BIRTH.equals(mode) ? R.string.age_birth : R.string.age_as_of)
                    .setView(picker).setPositiveButton(android.R.string.ok, (d, which) -> {
                        picker.clearFocus();
                        send(mode, selectedDate(), false);
                    });
        }
        return builder.create();
    }

    private LocalDate selectedDate() { return LocalDate.of(picker.getYear(), picker.getMonth() + 1, picker.getDayOfMonth()); }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (picker != null) out.putString("selected", selectedDate().toString());
    }
    private void send(String mode, LocalDate date, boolean yearly) {
        Bundle result = new Bundle();
        result.putString("mode", mode);
        result.putString("date", date.toString());
        result.putBoolean("yearly", yearly);
        getParentFragmentManager().setFragmentResult(RESULT, result);
    }
    private void stylePicker(View view) {
        if (view instanceof TextView text) text.setTextColor(ThemeManager.get().primaryContentColor());
        if (Build.VERSION.SDK_INT >= 29 && view instanceof NumberPicker number) number.setTextColor(ThemeManager.get().primaryContentColor());
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) stylePicker(group.getChildAt(i));
    }
    @Override public void onStart() {
        super.onStart();
        ThemeApplier.applyDialog((AlertDialog) requireDialog());
    }
}
