package net.smartlogic.unitconverter.helper;

import android.app.Application;
import net.smartlogic.unitconverter.model.CalculationHistoryItem;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34)
public class HistoryDatabaseTest {
    private DatabaseHelper database;
    @Before public void setup() {
        RuntimeEnvironment.getApplication().deleteDatabase("UnitCalc.db");
        database = new DatabaseHelper(RuntimeEnvironment.getApplication());
    }
    @After public void cleanup() { database.close(); }

    @Test public void deleteAndUndoPreserveIdentityEvenForDuplicateValues() {
        database.addHistory("1+1", "2", CalculatorCatalog.ID_BASIC);
        database.addHistory("1+1", "2", CalculatorCatalog.ID_BASIC);
        List<CalculationHistoryItem> original = database.getAllHistory();
        CalculationHistoryItem deleted = original.get(1);
        assertTrue(database.deleteHistoryItem(deleted.id()));
        assertEquals(List.of(original.get(0)), database.getAllHistory());
        assertTrue(database.restoreHistoryItem(deleted));
        assertEquals(original, database.getAllHistory());
        assertFalse(database.restoreHistoryItem(new CalculationHistoryItem("changed", "wrong", 1, CalculatorCatalog.ID_BASIC, deleted.id())));
        assertEquals(original, database.getAllHistory());
    }

    @Test public void keysetPagesRemainStableAcrossInsertAndDelete() {
        for (int i = 0; i < 123; i++) database.addHistory(Integer.toString(i), Integer.toString(i), CalculatorCatalog.ID_BASIC);
        List<CalculationHistoryItem> first = database.getHistoryPage(Long.MAX_VALUE, 50);
        assertEquals(50, first.size());
        database.addHistory("new", "new", CalculatorCatalog.ID_BASIC);
        database.deleteHistoryItem(first.get(0).id());
        List<CalculationHistoryItem> second = database.getHistoryPage(first.get(49).id(), 50);
        List<CalculationHistoryItem> third = database.getHistoryPage(second.get(49).id(), 50);
        Set<Long> ids = new HashSet<>();
        for (CalculationHistoryItem item : first) assertTrue(ids.add(item.id()));
        for (CalculationHistoryItem item : second) assertTrue(ids.add(item.id()));
        for (CalculationHistoryItem item : third) assertTrue(ids.add(item.id()));
        assertEquals(123, ids.size());
        assertEquals(23, third.size());
    }

    @Test public void queuedDeleteAndUndoKeepTheirOrder() throws Exception {
        database.addHistory("12345678901234567890", "12345678901234567890", CalculatorCatalog.ID_BASIC);
        CalculationHistoryItem item = database.getAllHistory().get(0);
        CountDownLatch done = new CountDownLatch(1);
        DatabaseHelper.executeHistoryWork(() -> database.deleteHistoryItem(item.id()));
        DatabaseHelper.executeHistoryWork(() -> { database.restoreHistoryItem(item); done.countDown(); });
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(List.of(item), database.getAllHistory());
        database.clearHistory();
        assertTrue(database.getAllHistory().isEmpty());
    }
}
