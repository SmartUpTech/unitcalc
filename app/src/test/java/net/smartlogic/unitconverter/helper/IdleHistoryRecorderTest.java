package net.smartlogic.unitconverter.helper;

import android.app.Application;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.fragment.ConverterFragment;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34)
@LooperMode(LooperMode.Mode.PAUSED)
public class IdleHistoryRecorderTest {
    public static class HostFragment extends Fragment {
        @Override public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
            return new FrameLayout(requireContext());
        }
    }
    private ActivityController<FragmentActivity> activity;
    private HostFragment fragment;
    private IdleHistoryRecorder recorder;

    @Before public void setup() {
        RuntimeEnvironment.getApplication().deleteDatabase("UnitCalc.db");
        activity = Robolectric.buildActivity(FragmentActivity.class);
        activity.get().setTheme(R.style.AppTheme);
        activity.setup();
        FrameLayout container = new FrameLayout(activity.get());
        container.setId(12345);
        activity.get().setContentView(container);
        fragment = new HostFragment();
        activity.get().getSupportFragmentManager().beginTransaction().add(12345, fragment).commitNow();
        recorder = new IdleHistoryRecorder(fragment, null);
    }

    @After public void cleanup() throws Exception {
        activity.pause().stop().destroy();
        drainWrites();
    }

    private void waitMs(long millis) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis)); }
    private void drainWrites() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        DatabaseHelper.executeHistoryWork(done::countDown);
        assertTrue(done.await(10, TimeUnit.SECONDS));
    }
    private int count() throws Exception {
        drainWrites();
        try (DatabaseHelper db = new DatabaseHelper(RuntimeEnvironment.getApplication())) {
            return db.getAllHistory().size();
        }
    }

    @Test public void typingRestartsDelayAndSavesOnlyLatestSnapshot() throws Exception {
        recorder.update("1+1", "2", CalculatorCatalog.ID_BASIC);
        waitMs(2_999);
        assertEquals(0, count());
        recorder.update("1+12", "13", CalculatorCatalog.ID_BASIC);
        waitMs(2_999);
        assertEquals(0, count());
        waitMs(1);
        assertEquals(1, count());
        try (DatabaseHelper db = new DatabaseHelper(RuntimeEnvironment.getApplication())) {
            assertEquals("1+12", db.getAllHistory().get(0).expression());
            assertEquals("13", db.getAllHistory().get(0).result());
        }
    }

    @Test public void equalsAndUnchangedResultDoNotDuplicateIdleSave() throws Exception {
        recorder.update("2*3", "6", CalculatorCatalog.ID_BASIC);
        recorder.saveNow();
        waitMs(3_000);
        recorder.update("2*3", "6", CalculatorCatalog.ID_BASIC);
        recorder.saveNow();
        assertEquals(1, count());
        recorder.update("3*4", "12", CalculatorCatalog.ID_BASIC);
        waitMs(3_000);
        recorder.saveNow();
        assertEquals(2, count());
    }

    @Test public void invalidInputCancelsPreviousValidResult() throws Exception {
        recorder.update("1/2", "0.5", CalculatorCatalog.ID_BASIC);
        waitMs(2_000);
        recorder.invalidate();
        waitMs(3_000);
        assertEquals(0, count());
    }

    @Test public void restoredHistoryIsNotSavedUntilUserChangesIt() throws Exception {
        recorder.update("4+5", "9", CalculatorCatalog.ID_BASIC);
        recorder.markCurrentAsSaved();
        waitMs(3_000);
        assertEquals(0, count());
        recorder.update("4+6", "10", CalculatorCatalog.ID_BASIC);
        waitMs(3_000);
        assertEquals(1, count());
    }

    @Test public void backgroundAndHiddenToolsCannotSave() throws Exception {
        recorder.update("2 USD → INR", "160 INR", CalculatorCatalog.ID_CURRENCY);
        waitMs(2_000);
        activity.pause();
        waitMs(3_000);
        assertEquals(0, count());
        activity.resume();
        activity.get().getSupportFragmentManager().beginTransaction().hide(fragment).commitNow();
        waitMs(3_000);
        assertEquals(0, count());
        activity.get().getSupportFragmentManager().beginTransaction().show(fragment).commitNow();
        recorder.visibilityChanged();
        waitMs(3_000);
        assertEquals(1, count());
    }

    @Test public void recreatedRecorderRetainsDuplicateProtection() throws Exception {
        recorder.update("0 m → cm", "0 cm", CalculatorCatalog.unitId(0));
        waitMs(3_000);
        Bundle state = new Bundle();
        recorder.saveState(state);
        IdleHistoryRecorder restored = new IdleHistoryRecorder(fragment, state);
        restored.update("0 m → cm", "0 cm", CalculatorCatalog.unitId(0));
        waitMs(3_000);
        assertEquals(1, count());
    }

    @Test public void bothConverterToolsSaveWithoutEquals() throws Exception {
        Preferences prefs = Preferences.getInstance(RuntimeEnvironment.getApplication());
        prefs.getPreferences().edit().clear().commit();
        long now = System.currentTimeMillis();
        prefs.setCurrencySnapshot(CurrencyRateStore.parseResponse(
                "[{\"date\":\"2026-10-20\",\"base\":\"USD\",\"quote\":\"INR\",\"rate\":80}]", now).encode(), now);
        prefs.setFromCurrencyIndex(31);
        prefs.setToCurrencyIndex(11);
        net.smartlogic.unitconverter.app.AppConst.CONVERTER_TAB = ConverterFragment.TAB_UNIT;
        ConverterFragment converters = new ConverterFragment();
        activity.get().getSupportFragmentManager().beginTransaction().replace(12345, converters).commitNow();
        waitMs(100);
        Fragment unit = converters.getChildFragmentManager().findFragmentByTag("converter_unit");
        assertTrue("Converter must be resumed", converters.isResumed());
        assertTrue("Unit tool must be resumed", unit.isResumed());
        assertFalse("Unit tool must be visible", unit.isHidden());
        ((EditText) unit.requireView().findViewById(R.id.input)).setText("12");
        String unitResult = ((EditText) unit.requireView().findViewById(R.id.output)).getText().toString();
        assertFalse(unitResult.isEmpty());
        waitMs(3_000);
        assertEquals(1, count());
        try (DatabaseHelper db = new DatabaseHelper(RuntimeEnvironment.getApplication())) {
            assertTrue(db.getAllHistory().get(0).result().startsWith(unitResult));
        }
        converters.openCurrencyMode();
        converters.getChildFragmentManager().executePendingTransactions();
        Fragment currency = converters.getChildFragmentManager().findFragmentByTag("converter_currency");
        ((EditText) currency.requireView().findViewById(R.id.input)).setText("2");
        waitMs(2_999);
        assertEquals(1, count());
        waitMs(1);
        assertEquals(2, count());
        try (DatabaseHelper db = new DatabaseHelper(RuntimeEnvironment.getApplication())) {
            var entry = db.getAllHistory().get(0);
            assertEquals(CalculatorCatalog.ID_CURRENCY, entry.calculatorId());
            assertEquals("2 USD → INR", entry.expression());
            assertEquals("160 INR", entry.result());
        }
    }
}
