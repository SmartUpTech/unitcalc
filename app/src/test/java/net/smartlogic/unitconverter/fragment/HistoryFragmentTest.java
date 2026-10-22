package net.smartlogic.unitconverter.fragment;

import android.app.Application;
import android.os.Bundle;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import com.google.android.material.snackbar.Snackbar;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.DatabaseHelper;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.CalculationHistoryItem;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34)
@LooperMode(LooperMode.Mode.PAUSED)
public class HistoryFragmentTest {
    private void drain() throws Exception {
        for (int i = 0; i < 4; i++) {
            CountDownLatch idle = new CountDownLatch(1);
            DatabaseHelper.executeHistoryWork(idle::countDown);
            assertTrue(idle.await(5, TimeUnit.SECONDS));
            shadowOf(Looper.getMainLooper()).idle();
        }
    }

    @Test public void clearRequiresConfirmationAndIndividualDeleteCanBeUndone() throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        app.deleteDatabase("UnitCalc.db");
        Preferences.getInstance(app);
        ThemeManager.init(app);
        DatabaseHelper db = new DatabaseHelper(app);
        db.addHistory("11+22", "33", CalculatorCatalog.ID_BASIC);
        CalculationHistoryItem original = db.getAllHistory().get(0);
        var controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup().visible();
        try {
            HistoryFragment fragment = HistoryFragment.newInstance();
            controller.get().getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, fragment, "history").commitNow();
            drain();
            assertTrue(fragment.hasHistory());
            android.view.View page = fragment.requireView();
            page.measure(android.view.View.MeasureSpec.makeMeasureSpec(360, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(700, android.view.View.MeasureSpec.EXACTLY));
            page.layout(0, 0, 360, 700);
            shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(page.findViewById(R.id.history_item_more));
            page.findViewById(R.id.history_item_more).performClick();
            fragment.getChildFragmentManager().executePendingTransactions();
            HistoryDialogFragment options = (HistoryDialogFragment) fragment.getChildFragmentManager().findFragmentByTag("history_dialog");
            var choices = ((AlertDialog) options.requireDialog()).getListView();
            choices.performItemClick(choices.getChildAt(0), 0, 0);
            shadowOf(Looper.getMainLooper()).idle();
            fragment.getChildFragmentManager().executePendingTransactions();
            HistoryDialogFragment details = (HistoryDialogFragment) fragment.getChildFragmentManager().findFragmentByTag("history_dialog");
            assertNotNull(details);
            assertNotNull(details.requireDialog().findViewById(R.id.tv_history_result));
            details.dismissNow();
            Bundle copy = HistoryDialogFragment.bundle(original);
            copy.putString(HistoryDialogFragment.ACTION, HistoryDialogFragment.COPY_CALCULATION);
            fragment.getChildFragmentManager().setFragmentResult(HistoryDialogFragment.RESULT_KEY, copy);
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) app.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            assertEquals(app.getString(R.string.history_copy_content, original.expression(), original.result()),
                    clipboard.getPrimaryClip().getItemAt(0).getText().toString());
            fragment.showClearHistoryConfirmation();
            controller.get().getSupportFragmentManager().executePendingTransactions();
            fragment.getChildFragmentManager().executePendingTransactions();
            assertEquals(1, db.getAllHistory().size());
            HistoryDialogFragment confirmation = (HistoryDialogFragment) fragment.getChildFragmentManager().findFragmentByTag("history_dialog");
            ((AlertDialog) confirmation.requireDialog()).getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals(1, db.getAllHistory().size());

            Bundle action = HistoryDialogFragment.bundle(original);
            action.putString(HistoryDialogFragment.ACTION, HistoryDialogFragment.DELETE);
            fragment.getChildFragmentManager().setFragmentResult(HistoryDialogFragment.RESULT_KEY, action);
            drain();
            assertTrue(db.getAllHistory().isEmpty());
            android.view.View undo = fragment.requireView().findViewById(com.google.android.material.R.id.snackbar_action);
            assertNotNull("Delete must expose Undo", undo);
            undo.performClick();
            drain();
            assertEquals(original, db.getAllHistory().get(0));

            fragment.showClearHistoryConfirmation();
            fragment.getChildFragmentManager().executePendingTransactions();
            confirmation = (HistoryDialogFragment) fragment.getChildFragmentManager().findFragmentByTag("history_dialog");
            ((AlertDialog) confirmation.requireDialog()).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            drain();
            assertTrue(db.getAllHistory().isEmpty());
            assertEquals(android.view.View.VISIBLE, fragment.requireView().findViewById(R.id.history_empty_group).getVisibility());
        } finally { controller.pause().stop().destroy(); drain(); db.close(); }
    }
}
