package net.smartlogic.unitconverter.calculator.finance.emi;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.DatabaseHelper;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.helper.PreferredCurrency;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34)
public class EmiIntegrationTest {
    @Test public void financeIsThirdDrawerCategoryAndEmiIsFavoriteEligible() {
        var section = CalculatorCatalog.getSections().get(2);
        assertEquals(R.string.drawer_section_finance, section.titleRes);
        assertTrue(section.entries.stream().anyMatch(e -> CalculatorCatalog.ID_EMI.equals(e.id)));
        assertTrue(CalculatorCatalog.getById(CalculatorCatalog.ID_EMI).isFavoriteEligible());
        assertFalse(CalculatorCatalog.getSections().stream().filter(s -> s.titleRes == R.string.drawer_section_tools)
                .flatMap(s -> s.entries.stream()).anyMatch(e -> CalculatorCatalog.ID_EMI.equals(e.id)));
    }
    private EmiDraft draft() {
        EmiDraft d = new EmiDraft("INR"); d.principal = "1000000"; d.rate = "8.5000"; d.tenure = "20.00";
        d.firstEmi = LocalDate.of(2026, 12, 2); return d;
    }
    @Test public void structuredStatePreservesRawInputsAndRules() {
        EmiDraft d = draft(); d.rules.add(new PrepaymentRule(new BigDecimal("5000.00"), 12, 240, 12));
        EmiDraft restored = EmiDraft.read(d.json().toString());
        assertEquals("8.5000", restored.rate); assertEquals("20.00", restored.tenure); assertEquals(d.inputs(), restored.inputs());
        assertThrows(IllegalArgumentException.class, () -> EmiDraft.read("{\"version\":2}"));
    }
    @Test public void preferredCurrencyDoesNotMutateConverterSelections() {
        var c = RuntimeEnvironment.getApplication(); var p = Preferences.getInstance(c);
        p.setFromCurrencyIndex(7); p.setToCurrencyIndex(3); p.setAdditionalCurrencyIndex(4); p.setPreferredCurrency("JPY");
        assertEquals("JPY", p.getPreferredCurrency()); assertEquals(7, p.getFromCurrencyIndex()); assertEquals(3, p.getToCurrencyIndex()); assertEquals(4, p.getAdditionalCurrencyIndex());
        assertEquals("JPY", PreferredCurrency.regionDefault(Locale.JAPAN, List.of("INR", "JPY")));
        assertEquals("INR", PreferredCurrency.regionDefault(Locale.ROOT, List.of("INR", "JPY")));
        assertEquals("INR", PreferredCurrency.regionDefault(new Locale("en", "AQ"), List.of("INR", "JPY")));
        EmiFormatter f = new EmiFormatter(c); assertEquals("¥123", f.money(new BigDecimal("123"), "JPY"));
        String value = f.money(new BigDecimal("123"), "INR"); assertTrue(value.startsWith("₹")); assertFalse(value.contains("INR")); assertTrue(value.endsWith(".00"));
    }
    @Test public void migrationAndUndoRetainStructuredPayloadAndLegacyRows() {
        var c = RuntimeEnvironment.getApplication(); c.deleteDatabase("UnitCalc.db");
        try (SQLiteDatabase legacy = SQLiteDatabase.openOrCreateDatabase(c.getDatabasePath("UnitCalc.db"), null)) {
            legacy.execSQL("CREATE TABLE calculation_history(id INTEGER PRIMARY KEY AUTOINCREMENT, expression TEXT, result TEXT, created_at INTEGER, calculator_id TEXT)");
            legacy.execSQL("INSERT INTO calculation_history VALUES(1,'1+1','2',1,'basic_calculator')"); legacy.setVersion(3);
        }
        try (DatabaseHelper database = new DatabaseHelper(c)) {
            assertEquals("1+1", database.getAllHistory().get(0).expression()); assertNull(database.getAllHistory().get(0).payload());
            String payload = draft().json().toString(); database.addHistory("Loan", "EMI", CalculatorCatalog.ID_EMI, payload);
            var saved = database.getAllHistory().get(0); assertEquals(payload, saved.payload());
            var bundle = net.smartlogic.unitconverter.fragment.HistoryDialogFragment.bundle(saved);
            assertEquals(saved, net.smartlogic.unitconverter.fragment.HistoryDialogFragment.item(bundle));
            assertTrue(database.deleteHistoryItem(saved.id())); assertTrue(database.restoreHistoryItem(saved)); assertEquals(saved, database.getAllHistory().get(0));
        }
    }
    @Test public void yearlyTotalsCsvAndGraphyUseAuthoritativeSnapshot() throws Exception {
        var c = RuntimeEnvironment.getApplication(); EmiResult r = EmiCalculator.calculate(draft().inputs());
        var annual = EmiTableRows.build(r, true);
        assertEquals(1, annual.get(0).count()); assertEquals(11, annual.get(annual.size() - 1).count());
        assertEquals(0, annual.stream().map(EmiTableRows.Row::interest).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(r.totalInterest()));
        ByteArrayOutputStream csv = new ByteArrayOutputStream(); EmiReportRenderer.csv(c, csv, r, null, false);
        String text = csv.toString(java.nio.charset.StandardCharsets.UTF_8); assertEquals(241, text.split("\r\n").length);
        assertTrue(text.contains("\"2026-12-02\"")); assertTrue(text.contains("\"8678.23\"")); assertTrue(text.contains("₹")); assertFalse(text.contains("INR"));
        var graph = EmiGraphBuilder.build(c, r, false);
        assertEquals(new EmiFormatter(c).money(r.regularEmi(), r), graph.getResult());
        assertEquals(1, graph.getNodes().stream().filter(n -> n.type() == net.smartlogic.unitconverter.graphy.model.GraphyNodeType.RESULT).count());
        assertFalse(net.smartlogic.unitconverter.graphy.model.GraphyTopology.orderedNodes(graph).isEmpty());
        EmiDraft prepaid = draft(); prepaid.rules.add(new PrepaymentRule(new BigDecimal("10000"), 12, 240, 12));
        EmiResult revised = EmiCalculator.calculate(prepaid.inputs());
        var impact = EmiGraphBuilder.build(c, revised, true, true);
        assertEquals(new EmiFormatter(c).money(revised.interestSaved(), revised), impact.getResult());
        assertFalse(net.smartlogic.unitconverter.graphy.model.GraphyTopology.orderedNodes(impact).isEmpty());
    }
}
