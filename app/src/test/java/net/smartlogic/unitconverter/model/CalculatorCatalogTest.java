package net.smartlogic.unitconverter.model;

import net.smartlogic.unitconverter.R;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CalculatorCatalogTest {

    @Test
    public void drawerSections_groupConvertersUnderSingleCategory() {
        List<CalculatorCatalog.Section> sections = CalculatorCatalog.getSections();

        assertEquals(3, sections.size());
        assertEquals(R.string.drawer_section_calculators, sections.get(0).titleRes);
        assertEquals(R.string.drawer_section_converter, sections.get(1).titleRes);
        assertEquals(R.string.drawer_section_general, sections.get(2).titleRes);

        CalculatorCatalog.Section converterSection = sections.get(1);
        assertEquals(2, converterSection.entries.size());
        assertEquals(CalculatorCatalog.ID_UNIT, converterSection.entries.get(0).id);
        assertEquals(R.string.drawer_unit_converter, converterSection.entries.get(0).titleRes);
        assertEquals(CalculatorCatalog.Destination.UNIT_CONVERTER, converterSection.entries.get(0).destination);
        assertEquals(CalculatorCatalog.ID_CURRENCY, converterSection.entries.get(1).id);
        assertEquals(R.string.currency_converter, converterSection.entries.get(1).titleRes);
        assertEquals(CalculatorCatalog.Destination.CURRENCY, converterSection.entries.get(1).destination);
    }

    @Test
    public void unitCategoriesRemainAvailableForExploreAndFavorites() {
        assertNotNull(CalculatorCatalog.getById(CalculatorCatalog.unitId(Conversion.LENGTH)));

        boolean hasUnitCategory = false;
        for (CalculatorCatalog.Entry entry : CalculatorCatalog.getFavoriteEligibleEntries()) {
            if (entry.destination == CalculatorCatalog.Destination.UNIT_CATEGORY) {
                hasUnitCategory = true;
                break;
            }
        }
        assertTrue(hasUnitCategory);
    }

    @Test
    public void drawerUnitConverterEntry_isNotFavoriteEligible() {
        CalculatorCatalog.Entry entry = CalculatorCatalog.getById(CalculatorCatalog.ID_UNIT);
        assertNotNull(entry);
        assertFalse(entry.isFavoriteEligible());
    }
}
