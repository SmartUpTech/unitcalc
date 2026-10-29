package net.smartlogic.unitconverter.age;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.DialogFragment;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.fragment.ConverterPickerDialogFragment;
import net.smartlogic.unitconverter.theme.CalculatorThemes;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.LooperMode;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class AgeThemeDialogTest {
    @Test public void dateCalendarAndValuePopupsRemainReadableInEveryTheme() throws Exception {
        var app = RuntimeEnvironment.getApplication();
        ThemeManager.setProAccessProvider(() -> true);
        try {
            for (var theme : CalculatorThemes.all()) {
                ThemeManager.select(app, theme.id());
                assertEquals(theme.id(), ThemeManager.get().id());
                var controller = Robolectric.buildActivity(AppCompatActivity.class);
                controller.get().setTheme(R.style.AppTheme);
                controller.setup();
                try {
                    var activity = controller.get();
                    for (String mode : new String[]{AgeDialogFragment.BIRTH, AgeDialogFragment.AS_OF, AgeDialogFragment.CALENDAR}) {
                        var popup = AgeDialogFragment.create(mode, LocalDate.of(2000, 2, 29));
                        popup.showNow(activity.getSupportFragmentManager(), AgeDialogFragment.TAG);
                        capture(popup, theme.id() + "-" + mode, theme.background());
                        popup.dismissNow();
                    }
                    var picker = ConverterPickerDialogFragment.forValues(activity.getString(R.string.age_year),
                            0, 1, new ArrayList<>(List.of("1999", "2000", "2001")), R.string.age_search_date_values);
                    picker.showNow(activity.getSupportFragmentManager(), ConverterPickerDialogFragment.TAG);
                    capture(picker, theme.id() + "-year-values", theme.background());
                    TextView search = picker.requireDialog().findViewById(R.id.converter_picker_search);
                    search.requestFocus();
                    search.setText("2000");
                    capture(picker, theme.id() + "-year-search", theme.background());
                    assertCursorContrast(search, theme.id(), theme.background());
                    picker.dismissNow();
                } finally { controller.pause().stop().destroy(); }
            }
        } finally { ThemeManager.setProAccessProvider(() -> false); }
    }

    private void capture(DialogFragment popup, String name, int background) throws Exception {
        shadowOf(Looper.getMainLooper()).idle();
        var window = popup.requireDialog().getWindow();
        assertNotNull(window);
        View decor = window.getDecorView();
        int width = window.getAttributes().width > 0 ? window.getAttributes().width : 328;
        int height = window.getAttributes().height;
        decor.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height > 0 ? height : 700,
                        height > 0 ? View.MeasureSpec.EXACTLY : View.MeasureSpec.AT_MOST));
        decor.layout(0, 0, decor.getMeasuredWidth(), decor.getMeasuredHeight());
        assertTrue(name + " has visible popup content", decor.getHeight() > 0);
        Bitmap bitmap = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(background);
        decor.draw(new Canvas(bitmap));
        File file = new File("build/reports/age/dialogs/" + name + ".png");
        file.getParentFile().mkdirs();
        try (var stream = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); }
        bitmap.recycle();
        inspect(decor, name, background);
    }

    private void inspect(View view, String name, int background) {
        if (view.getVisibility() != View.VISIBLE) return;
        if (view instanceof TextView text && text.getText().length() > 0) {
            assertTrue(name + " readable text: " + text.getText(),
                    ColorUtils.calculateContrast(text.getCurrentTextColor(), background) >= 4.5);
            assertFalse(name + " uses regular text: " + text.getText(), text.getTypeface().isBold());
            if (text.getLayout() != null && text.getHeight() > 0) {
                assertTrue(name + " unclipped text: " + text.getText(),
                        text.getLayout().getLineBottom(text.getLayout().getLineCount() - 1)
                                <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
            }
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) inspect(group.getChildAt(i), name, background);
        }
    }

    private void assertCursorContrast(TextView search, String name, int background) {
        var cursor = search.getTextCursorDrawable();
        assertNotNull(name + " has a search cursor", cursor);
        int width = Math.max(1, cursor.getIntrinsicWidth());
        int height = Math.max(search.getLineHeight(), cursor.getIntrinsicHeight());
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        cursor.setBounds(0, 0, width, height);
        cursor.draw(new Canvas(image));
        int mostOpaque = 0;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int color = image.getPixel(x, y);
            if (android.graphics.Color.alpha(color) > android.graphics.Color.alpha(mostOpaque)) mostOpaque = color;
        }
        image.recycle();
        assertTrue(name + " visible search cursor", android.graphics.Color.alpha(mostOpaque) > 0);
        assertTrue(name + " search cursor contrasts with popup", ColorUtils.calculateContrast(
                ColorUtils.compositeColors(mostOpaque, background), background) >= 3.0);
    }
}
