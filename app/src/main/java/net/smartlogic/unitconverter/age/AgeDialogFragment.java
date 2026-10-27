package net.smartlogic.unitconverter.age;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;
import net.smartlogic.unitconverter.fragment.ConverterPickerDialogFragment;
import net.smartlogic.unitconverter.helper.InteractionFeedbackManager;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;

/** Restorable date/calendar choices. Birth dates remain local to the tool session. */
public final class AgeDialogFragment extends DialogFragment {
    public static final String TAG = "age_dialog", RESULT = "age_choice";
    public static final String BIRTH = "birth", AS_OF = "asof", CALENDAR = "calendar";
    private LocalDate selected;
    private View dateView;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        selected = LocalDate.parse(state == null ? requireArguments().getString("date")
                : state.getString("selected", requireArguments().getString("date")));
        getChildFragmentManager().setFragmentResultListener(ConverterPickerDialogFragment.RESULT_KEY, this, (key, result) -> {
            int group = result.getInt(ConverterPickerDialogFragment.GROUP);
            int index = result.getInt(ConverterPickerDialogFragment.INDEX);
            int year = group == 0 ? AgeCalculator.MIN_DATE.getYear() + index : selected.getYear();
            int month = group == 1 ? index + 1 : selected.getMonthValue();
            int day = group == 2 ? index + 1 : selected.getDayOfMonth();
            selected = LocalDate.of(year, month, Math.min(day, YearMonth.of(year, month).lengthOfMonth()));
            updateDateButtons();
        });
    }

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
            dateView = getLayoutInflater().inflate(R.layout.dialog_age_date, null);
            int[] ids = {R.id.age_pick_year, R.id.age_pick_month, R.id.age_pick_day};
            for (int i = 0; i < ids.length; i++) {
                final int part = i;
                View button = dateView.findViewById(ids[i]);
                InteractionFeedbackManager.configure(button);
                button.setOnClickListener(v -> pickPart(part));
            }
            updateDateButtons();
            builder.setTitle(BIRTH.equals(mode) ? R.string.age_birth : R.string.age_as_of)
                    .setView(dateView).setPositiveButton(android.R.string.ok, (d, which) -> send(mode, selected, false));
        }
        return builder.create();
    }

    private void updateDateButtons() {
        if (dateView == null) return;
        var locale = getResources().getConfiguration().getLocales().get(0);
        var number = NumberFormat.getIntegerInstance(locale);
        number.setGroupingUsed(false);
        ((TextView) dateView.findViewById(R.id.age_pick_year)).setText(getString(R.string.age_date_button, getString(R.string.age_year), number.format(selected.getYear())));
        ((TextView) dateView.findViewById(R.id.age_pick_month)).setText(getString(R.string.age_date_button, getString(R.string.age_month), selected.format(DateTimeFormatter.ofPattern("LLLL", locale))));
        ((TextView) dateView.findViewById(R.id.age_pick_day)).setText(getString(R.string.age_date_button, getString(R.string.age_day), number.format(selected.getDayOfMonth())));
        ThemeApplier.apply(dateView);
    }

    private void pickPart(int part) {
        var locale = getResources().getConfiguration().getLocales().get(0);
        var number = NumberFormat.getIntegerInstance(locale);
        number.setGroupingUsed(false);
        ArrayList<String> labels = new ArrayList<>();
        int min = part == 0 ? AgeCalculator.MIN_DATE.getYear() : 1;
        int max = part == 0 ? AgeCalculator.MAX_DATE.getYear() : part == 1 ? 12 : selected.lengthOfMonth();
        for (int value = min; value <= max; value++) labels.add(part == 1
                ? LocalDate.of(2000, value, 1).format(DateTimeFormatter.ofPattern("LLLL", locale)) : number.format(value));
        int index = part == 0 ? selected.getYear() - min : part == 1 ? selected.getMonthValue() - 1 : selected.getDayOfMonth() - 1;
        int title = part == 0 ? R.string.age_year : part == 1 ? R.string.age_month : R.string.age_day;
        ConverterPickerDialogFragment.forValues(getString(title), part, index, labels, R.string.age_search_date_values).showFor(this);
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("selected", selected.toString());
    }
    private void send(String mode, LocalDate date, boolean yearly) {
        Bundle result = new Bundle();
        result.putString("mode", mode);
        result.putString("date", date.toString());
        result.putBoolean("yearly", yearly);
        getParentFragmentManager().setFragmentResult(RESULT, result);
    }
    @Override public void onStart() {
        super.onStart();
        ThemeApplier.applyDialog((AlertDialog) requireDialog());
    }
}
