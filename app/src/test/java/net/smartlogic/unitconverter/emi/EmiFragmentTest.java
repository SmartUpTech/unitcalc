package net.smartlogic.unitconverter.emi;

import android.app.Application;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class EmiFragmentTest {
    public static class Host extends AppCompatActivity {
        @Override public void onCreate(Bundle state) {
            setTheme(R.style.AppTheme); super.onCreate(state);
            FrameLayout frame = new FrameLayout(this); frame.setId(12345); setContentView(frame);
        }
    }
    private EmiCalculatorFragment launch(Host activity) throws Exception {
        ThemeManager.init(activity);
        Preferences.getInstance(activity).getPreferences().edit().remove("emi_workspace_v1").apply();
        EmiCalculatorFragment f = new EmiCalculatorFragment(); activity.getSupportFragmentManager().beginTransaction().add(12345, f).commitNow();
        layout(activity); drain(f);
        set(f, R.id.emi_principal, "1000000"); set(f, R.id.emi_rate, "8.5"); set(f, R.id.emi_tenure, "20"); drain(f); return f;
    }
    private void layout(Host a) {
        View root = a.getWindow().getDecorView();
        root.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 1080, 1920); org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
    private void drain(EmiCalculatorFragment f) throws Exception {
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200));
        Future<?> work = ReflectionHelpers.getField(f, "calculation"); if (work != null) work.get(10, TimeUnit.SECONDS);
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
    private View form(EmiCalculatorFragment f) { return ReflectionHelpers.getField(f, "formPage"); }
    private void set(EmiCalculatorFragment f, int id, String value) { ((EditText) form(f).findViewById(id)).setText(value); }
    private void menu(EmiCalculatorFragment f, int index) {
        EmiDialogFragment menu = (EmiDialogFragment) f.getChildFragmentManager().findFragmentByTag(EmiDialogFragment.TAG);
        assertNotNull(menu); AlertDialog dialog = (AlertDialog) menu.requireDialog(); dialog.getListView().performItemClick(null, index, index);
    }
    private View text(View view, String value) {
        if (view instanceof TextView t && t.getText().toString().equals(value)) return view;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) { View found = text(group.getChildAt(i), value); if (found != null) return found; }
        return null;
    }
    @Test public void liveResultInvalidationPrepaymentsAndNestedExportMenus() throws Exception {
        var controller = Robolectric.buildActivity(Host.class).setup().visible();
        try {
            EmiCalculatorFragment f = launch(controller.get()); assertNotNull(ReflectionHelpers.getField(f, "resultA"));
            ((EditText) form(f).findViewById(R.id.emi_principal)).performClick();
            assertEquals(View.VISIBLE, form(f).findViewById(R.id.emi_keys).getVisibility());
            assertTrue(form(f).findViewById(R.id.emi_value).getVisibility() == View.VISIBLE);
            f.handleBack();
            EditText rate = form(f).findViewById(R.id.emi_rate);
            rate.getText().replace(0, rate.length(), "101"); assertEquals("8.5", rate.getText().toString());
            rate.getText().replace(0, rate.length(), "8.50001"); assertEquals("8.5", rate.getText().toString());
            EditText principal = form(f).findViewById(R.id.emi_principal);
            principal.getText().replace(0, principal.length(), "1000000000000"); assertEquals("1000000", principal.getText().toString());
            principal.getText().replace(1, 4, "NaN"); assertEquals("1000000", principal.getText().toString());
            set(f, R.id.emi_rate, ""); assertNull(ReflectionHelpers.getField(f, "resultA")); drain(f);
            assertEquals("—", ((TextView) form(f).findViewById(R.id.emi_value)).getText().toString());
            set(f, R.id.emi_rate, "8.5"); drain(f);
            principal.performClick(); set(f, R.id.emi_principal, "1"); drain(f);
            View root = f.requireView();
            root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(528, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, 360, 528);
            assertTrue("Rounding guidance must leave the editor usable", ((EmiLayout) form(f).findViewById(R.id.emi_layout)).fitsViewport());
            set(f, R.id.emi_principal, "1000000"); drain(f); f.handleBack();
            form(f).findViewById(R.id.emi_more).performClick(); menu(f, 0);
            assertEquals("rules", ReflectionHelpers.getField(f, "screen"));
            text(f.requireView().findViewById(R.id.emi_secondary), f.getString(R.string.emi_add_rule)).performClick();
            set(f, R.id.emi_principal, "10000"); set(f, R.id.emi_rate, "12"); form(f).findViewById(R.id.emi_insights).performClick(); drain(f);
            EmiResult result = ReflectionHelpers.getField(f, "resultA"); assertTrue(result.interestSaved().signum() > 0);
            f.handleBack(); form(f).findViewById(R.id.emi_more).performClick(); menu(f, 2);
            // Selecting Save report opens a second dialog after the first menu is removed.
            EmiDialogFragment export = (EmiDialogFragment) f.getChildFragmentManager().findFragmentByTag(EmiDialogFragment.TAG);
            assertNotNull(export); assertEquals(2, ((AlertDialog) export.requireDialog()).getListView().getAdapter().getCount());
        } finally { controller.pause().stop().destroy(); }
    }
    @Test public void comparisonEditsAndRecreationKeepIndependentScenariosAndSelection() throws Exception {
        var controller = Robolectric.buildActivity(Host.class).setup().visible();
        try {
            EmiCalculatorFragment f = launch(controller.get());
            form(f).findViewById(R.id.emi_more).performClick(); menu(f, 1); drain(f);
            text(f.requireView().findViewById(R.id.emi_secondary), f.getString(R.string.emi_edit_b)).performClick();
            set(f, R.id.emi_rate, "8"); drain(f); form(f).findViewById(R.id.emi_insights).performClick(); drain(f);
            EmiDraft a = ReflectionHelpers.getField(f, "a"), b = ReflectionHelpers.getField(f, "b"); assertEquals("8.5", a.rate); assertEquals("8", b.rate);
            f.handleBack(); EditText amount = form(f).findViewById(R.id.emi_principal); amount.setSelection(1, 4);
            Bundle saved = new Bundle(); f.onSaveInstanceState(saved); assertEquals(1, saved.getIntArray("caret_a")[0]); assertEquals(4, saved.getIntArray("caret_a")[1]);
            controller.recreate(); layout(controller.get());
            EmiCalculatorFragment restored = (EmiCalculatorFragment) controller.get().getSupportFragmentManager().findFragmentById(12345); drain(restored);
            assertEquals("8.5", ((EmiDraft) ReflectionHelpers.getField(restored, "a")).rate);
            assertEquals("8", ((EmiDraft) ReflectionHelpers.getField(restored, "b")).rate);
            assertEquals(1, ((EditText) form(restored).findViewById(R.id.emi_principal)).getSelectionStart());
        } finally { controller.pause().stop().destroy(); }
    }
}
