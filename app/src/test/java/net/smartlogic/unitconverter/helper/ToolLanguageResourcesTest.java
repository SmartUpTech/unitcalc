package net.smartlogic.unitconverter.helper;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import net.smartlogic.unitconverter.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Real Android resource selection across tools, reports and regional language variants. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class ToolLanguageResourcesTest {
    private Context language(String tag) {
        Context app = RuntimeEnvironment.getApplication();
        Configuration config = new Configuration(app.getResources().getConfiguration());
        config.setLocales(LocaleList.forLanguageTags(tag));
        return app.createConfigurationContext(config);
    }

    @Test public void toolExplanationsNeverFallBackToEnglishForOfferedTranslations() {
        Context english = language("en-US");
        int[] resources = {R.string.graphy_empty, R.string.history_clear_question,
                R.string.games_load_error, R.string.age_calendar_rule,
                R.string.bmi_child_guidance, R.string.emi_assumptions,
                R.string.investment_fd_assumptions, R.string.store_listing_full_description};
        for (SupportedLanguage choice : SupportedLanguage.ALL) {
            Context selected = language(choice.tag);
            for (int id : resources) {
                String value = selected.getString(id);
                assertFalse(choice.tag, value.isEmpty());
                if (!choice.language.equals("en"))
                    assertNotEquals(choice.tag + ": English fallback for " + id,
                            english.getString(id), value);
            }
            String loan = selected.getString(R.string.emi_share_summary,
                    "AMOUNT", "RATE", 12, "MONTHLY", "INTEREST", "TOTAL",
                    "PAYOFF", "SAVED", 3, "FINAL");
            for (String token : new String[]{"AMOUNT", "RATE%", "12", "MONTHLY",
                    "INTEREST", "TOTAL", "PAYOFF", "SAVED", "3", "FINAL"})
                assertTrue(choice.tag + ": missing report field " + token, loan.contains(token));
            assertFalse(choice.tag, loan.contains("%10$s"));
            String investment = selected.getString(R.string.investment_history, "VALUE", "RATE", 24);
            assertTrue(choice.tag, investment.contains("VALUE"));
            assertTrue(choice.tag, investment.contains("RATE%"));
            assertTrue(choice.tag, investment.contains("24"));
        }
    }

    @Test public void russianAndPolishSelectOneFewAndManyWithWesternValues() {
        Context russian = language("ru-RU");
        Context polish = language("pl-PL");
        assertEquals("1 год", russian.getResources().getQuantityString(R.plurals.age_years, 1, "1"));
        assertEquals("2 года", russian.getResources().getQuantityString(R.plurals.age_years, 2, "2"));
        assertEquals("5 лет", russian.getResources().getQuantityString(R.plurals.age_years, 5, "5"));
        assertEquals("21 год", russian.getResources().getQuantityString(R.plurals.age_years, 21, "21"));
        assertEquals("1 rok", polish.getResources().getQuantityString(R.plurals.age_years, 1, "1"));
        assertEquals("2 lata", polish.getResources().getQuantityString(R.plurals.age_years, 2, "2"));
        assertEquals("5 lat", polish.getResources().getQuantityString(R.plurals.age_years, 5, "5"));
        assertEquals("22 lata", polish.getResources().getQuantityString(R.plurals.age_years, 22, "22"));
    }
}
