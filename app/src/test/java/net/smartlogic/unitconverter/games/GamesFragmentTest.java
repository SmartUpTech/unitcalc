package net.smartlogic.unitconverter.games;

import android.app.Application;
import android.os.Looper;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;

import net.smartlogic.unitconverter.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowWebView;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35)
public class GamesFragmentTest {
    @Test public void firstVisitResumesTheWebViewAfterFragmentResumeIsDispatched() {
        var controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup();
        FragmentActivity activity = controller.get();
        FrameLayout host = new FrameLayout(activity);
        host.setId(12345);
        activity.setContentView(host);
        GamesFragment fragment = new GamesFragment();
        activity.getSupportFragmentManager().beginTransaction().add(12345, fragment).commitNow();
        FrameLayout container = fragment.requireView().findViewById(R.id.games_web_container);
        GamesWebView webView = (GamesWebView) container.getChildAt(0);
        ShadowWebView shadow = Shadows.shadowOf(webView);
        assertTrue("created before RESUMED, so initially paused", shadow.wasOnPauseCalled());
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertTrue("first visit must resume without a retry", shadow.wasOnResumeCalled());
        ReflectionHelpers.setField(shadow, "onResumeCalled", false);
        controller.pause().stop().start().resume();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertTrue("foreground return must resume the existing WebView", shadow.wasOnResumeCalled());
        assertSame(webView, container.getChildAt(0));

        ReflectionHelpers.setField(shadow, "onPauseCalled", false);
        ReflectionHelpers.setField(shadow, "onResumeCalled", false);
        activity.getSupportFragmentManager().beginTransaction().hide(fragment).commitNow();
        assertTrue(shadow.wasOnPauseCalled());
        assertFalse(shadow.wasOnResumeCalled());
        activity.getSupportFragmentManager().beginTransaction().show(fragment).commitNow();
        assertTrue("tab return must resume without recreating", shadow.wasOnResumeCalled());
        assertSame(webView, container.getChildAt(0));
        controller.pause().stop().destroy();
        assertEquals(0, container.getChildCount());
    }
}
