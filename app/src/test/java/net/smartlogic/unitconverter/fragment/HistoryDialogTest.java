package net.smartlogic.unitconverter.fragment;

import android.app.Application;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.CalculationHistoryItem;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34)
public class HistoryDialogTest {
    @Test public void fullValuesRemainSelectableAndCopyActionKeepsRawDigits() {
        Preferences.getInstance(RuntimeEnvironment.getApplication());
        ThemeManager.init(RuntimeEnvironment.getApplication());
        var controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup();
        try {
            var activity = controller.get();
            String raw = "123456789012345678901234567890123456789012345678901234567890";
            var item = new CalculationHistoryItem(raw + "+1", raw, 123456L, CalculatorCatalog.ID_BASIC, 42);
            Bundle[] result = {null};
            activity.getSupportFragmentManager().setFragmentResultListener(HistoryDialogFragment.RESULT_KEY, activity, (key, args) -> result[0] = args);
            var fragment = HistoryDialogFragment.create(HistoryDialogFragment.DETAILS, item);
            fragment.showNow(activity.getSupportFragmentManager(), "details");
            AlertDialog dialog = (AlertDialog) fragment.requireDialog();
            TextView value = dialog.findViewById(R.id.tv_history_result);
            assertEquals(raw, value.getText().toString());
            assertTrue(value.isTextSelectable());
            assertEquals(Integer.MAX_VALUE, value.getMaxLines());
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertNotNull(result[0]);
            assertEquals(raw, HistoryDialogFragment.item(result[0]).result());
            assertEquals(42, HistoryDialogFragment.item(result[0]).id());
        } finally { controller.pause().stop().destroy(); }
    }
}
