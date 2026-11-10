package net.smartlogic.unitconverter.calculator.finance.emi;

import android.app.Application;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.theme.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class EmiReportTest {
    @Test public void csvReportsContainCompleteMonthlyAndYearlySchedules() throws Exception {
        var c = RuntimeEnvironment.getApplication(); ThemeManager.init(c);
        Preferences.getInstance(c).getPreferences().edit().putString(Appearance.FONT, Appearance.Font.OSWALD.name()).apply();
        EmiInputs i = new EmiInputs(new BigDecimal("1000000"), new BigDecimal("8.5"), 240,
                LocalDate.of(2024, 1, 31), "INR", EmiInputs.Method.REDUCE_TENURE, List.of());
        EmiResult a = EmiCalculator.calculate(i);
        EmiResult b = EmiCalculator.calculate(new EmiInputs(i.principal(), i.annualRate(), i.months(), i.firstEmi(), i.currencyCode(),
                EmiInputs.Method.REDUCE_EMI, List.of(new PrepaymentRule(new BigDecimal("10000"), 12, 240, 12))));
        File directory = new File("build/reports/emi/documents"); assertTrue(directory.isDirectory() || directory.mkdirs());
        for (boolean yearly : new boolean[]{false, true}) {
            File csv = new File(directory, yearly ? "comparison-yearly.csv" : "schedule-monthly.csv");
            try (var out = new FileOutputStream(csv)) {
                EmiReportRenderer.csv(c, out, a, yearly ? b : null, yearly);
            }
            var lines = java.nio.file.Files.readAllLines(csv.toPath());
            int expected = EmiTableRows.build(a, yearly).size() + (yearly ? EmiTableRows.build(b, true).size() : 0);
            assertEquals(expected + 1, lines.size());
            assertTrue(lines.get(1).contains("2024-01-31"));
            assertTrue(lines.get(lines.size() - 1).endsWith("\"0.00\""));
        }
    }
}
