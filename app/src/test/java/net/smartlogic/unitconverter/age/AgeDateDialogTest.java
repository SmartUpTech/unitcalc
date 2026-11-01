package net.smartlogic.unitconverter.age;

import android.app.Application;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.fragment.ConverterPickerDialogFragment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.time.LocalDate;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34)
public class AgeDateDialogTest {
    @Test public void comparisonDateCanBeChangedWithoutAResetAction() {
        var controller = Robolectric.buildActivity(AppCompatActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup();
        try {
            var activity = controller.get();
            Bundle[] received = new Bundle[1];
            activity.getSupportFragmentManager().setFragmentResultListener(AgeDialogFragment.RESULT, activity,
                    (key, value) -> received[0] = value);
            var dialog = AgeDialogFragment.create(AgeDialogFragment.AS_OF, LocalDate.of(2020, 1, 21));
            dialog.showNow(activity.getSupportFragmentManager(), AgeDialogFragment.TAG);
            assertEquals(android.view.View.GONE, ((androidx.appcompat.app.AlertDialog) dialog.requireDialog()).getButton(-3).getVisibility());
            ((androidx.appcompat.app.AlertDialog) dialog.requireDialog()).getButton(-1).performClick();
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertEquals(AgeDialogFragment.AS_OF, received[0].getString("mode"));
            assertEquals("2020-01-21", received[0].getString("date"));
        } finally { controller.pause().stop().destroy(); }
    }
    @Test public void existingPickerClampsDayAndRestoresUncommittedDate() {
        var controller = Robolectric.buildActivity(AppCompatActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup();
        try {
            var dialog = AgeDialogFragment.create(AgeDialogFragment.BIRTH, LocalDate.of(2024, 1, 31));
            dialog.showNow(controller.get().getSupportFragmentManager(), AgeDialogFragment.TAG);
            Bundle choice = new Bundle();
            choice.putInt(ConverterPickerDialogFragment.GROUP, 1);
            choice.putInt(ConverterPickerDialogFragment.INDEX, 1);
            dialog.getChildFragmentManager().setFragmentResult(ConverterPickerDialogFragment.RESULT_KEY, choice);
            assertTrue(((TextView) dialog.requireDialog().findViewById(R.id.age_pick_day)).getText().toString().contains("29"));
            choice = new Bundle();
            choice.putInt(ConverterPickerDialogFragment.GROUP, 0);
            choice.putInt(ConverterPickerDialogFragment.INDEX, 123); // 2023, a non-leap year.
            dialog.getChildFragmentManager().setFragmentResult(ConverterPickerDialogFragment.RESULT_KEY, choice);
            assertTrue(((TextView) dialog.requireDialog().findViewById(R.id.age_pick_day)).getText().toString().contains("28"));
            var activity = controller.recreate().get();
            var restored = (AgeDialogFragment) activity.getSupportFragmentManager().findFragmentByTag(AgeDialogFragment.TAG);
            assertNotNull(restored);
            assertTrue(((TextView) restored.requireDialog().findViewById(R.id.age_pick_year)).getText().toString().contains("2023"));
            assertTrue(((TextView) restored.requireDialog().findViewById(R.id.age_pick_day)).getText().toString().contains("28"));
        } finally { controller.pause().stop().destroy(); }
    }
}
