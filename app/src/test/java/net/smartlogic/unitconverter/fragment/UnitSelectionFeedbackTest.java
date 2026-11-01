package net.smartlogic.unitconverter.fragment;

import android.app.Application;
import android.os.Looper;
import android.view.View;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.Conversion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34,
        instrumentedPackages = {"net.smartlogic.unitconverter.helper"},
        shadows = UnitSelectionFeedbackTest.FeedbackRequests.class)
public class UnitSelectionFeedbackTest {
    @Implements(value = net.smartlogic.unitconverter.helper.InteractionFeedbackManager.class, isInAndroidSdk = false)
    public static class FeedbackRequests {
        static int categoryPulses;
        static int pickerPulses;
        @Implementation public static void perform(View view,
                net.smartlogic.unitconverter.helper.InteractionFeedbackManager.Type type) {
            if (view.getId() == R.id.category_selector) categoryPulses++;
            else pickerPulses++;
        }
    }

    public static class CountingUnitFragment extends UnitConverterFragment {
        int selections;
        Runnable duringSelection;
        @Override public void selectCategory(int id) {
            selections++;
            Runnable callback = duringSelection;
            duringSelection = null;
            if (callback != null) callback.run();
            super.selectCategory(id);
        }
    }

    @Test public void repeatedPickerCallbacksOpenAndCommitOnlyOnce() {
        var controller = Robolectric.buildActivity(AppCompatActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup().visible();
        try {
            Preferences prefs = Preferences.getInstance(controller.get());
            prefs.setLastConversion(Conversion.LENGTH);
            UnitConverterFragment fragment = new UnitConverterFragment();
            controller.get().getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, fragment).commitNow();
            shadowOf(Looper.getMainLooper()).idle();
            FeedbackRequests.pickerPulses = 0;
            View from = fragment.requireView().findViewById(R.id.fromUnit);
            from.performClick();
            from.performClick();
            from.performClick();
            assertEquals("feedback only when the picker opens", 1, FeedbackRequests.pickerPulses);
            var picker = (ConverterPickerDialogFragment) fragment.getChildFragmentManager()
                    .findFragmentByTag(ConverterPickerDialogFragment.TAG);
            assertNotNull(picker);
            int[] results = {0};
            fragment.getChildFragmentManager().setFragmentResultListener(
                    ConverterPickerDialogFragment.RESULT_KEY, fragment.getViewLifecycleOwner(),
                    (key, result) -> results[0]++);
            ListView list = picker.requireDialog().findViewById(R.id.converter_picker_list);
            View row = list.getAdapter().getView(1, null, list);
            FeedbackRequests.pickerPulses = 0;
            list.performItemClick(row, 1, list.getAdapter().getItemId(1));
            list.performItemClick(row, 1, list.getAdapter().getItemId(1));
            list.performItemClick(row, 1, list.getAdapter().getItemId(1));
            assertEquals("one selection result", 1, results[0]);
            assertEquals("one selection haptic", 1, FeedbackRequests.pickerPulses);
            shadowOf(Looper.getMainLooper()).idle();
        } finally {
            controller.pause().stop().destroy();
        }
    }

    @Test public void tappingCategoryChangesItOnceAndProducesOneHaptic() {
        verifyCategoryTap(false);
    }

    @Test public void categoryTransitionIgnoresReentrantSelectionCallbacks() {
        verifyCategoryTap(true);
    }

    private void verifyCategoryTap(boolean reentrant) {
        var controller = Robolectric.buildActivity(AppCompatActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup().visible();
        try {
            Preferences prefs = Preferences.getInstance(controller.get());
            prefs.setKeyVibrationEnabled(true);
            prefs.setLastConversion(Conversion.LENGTH);
            CountingUnitFragment fragment = new CountingUnitFragment();
            controller.get().getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, fragment).commitNow();
            shadowOf(Looper.getMainLooper()).idle();
            FeedbackRequests.categoryPulses = 0;
            fragment.selections = 0;
            TabLayout tabs = fragment.requireView().findViewById(R.id.category_selector);
            TabLayout.Tab target = null;
            for (int i = 0; i < tabs.getTabCount(); i++) {
                if ((Integer) tabs.getTabAt(i).getTag() == Conversion.MASS) target = tabs.getTabAt(i);
            }
            assertNotNull(target);
            if (reentrant) {
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    TabLayout.Tab other = tabs.getTabAt(i);
                    if ((Integer) other.getTag() == Conversion.TEMPERATURE) fragment.duringSelection = other::select;
                }
            }
            target.view.performClick();
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals(Conversion.MASS, fragment.getSelectedCategoryId());
            assertEquals("one category transition per click", 1, fragment.selections);
            assertEquals("one haptic per click", 1, FeedbackRequests.categoryPulses);
            target.view.performClick();
            target.select();
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals("reselection must not repeat category work", 1, fragment.selections);
            assertEquals("reselection must not repeat feedback", 1, FeedbackRequests.categoryPulses);
            fragment.selectCategory(Conversion.TEMPERATURE);
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals("programmatic category synchronization is silent", 1, FeedbackRequests.categoryPulses);
        } finally {
            controller.pause().stop().destroy();
        }
    }
}
