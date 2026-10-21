package net.smartlogic.unitconverter.helper;

import android.app.Application;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import net.smartlogic.unitconverter.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35, qualifiers = "mdpi")
public class WorkspaceTabControllerTest {
    @Test public void disabledGraphyRejectsSwipesAndSelectionWithoutReentrantDispatch() {
        var activity = Robolectric.buildActivity(FragmentActivity.class);
        activity.get().setTheme(R.style.AppTheme);
        activity.setup();
        LinearLayout root = new LinearLayout(activity.get());
        root.setOrientation(LinearLayout.VERTICAL);
        TabLayout tabs = new TabLayout(activity.get());
        ViewPager2 pager = new ViewPager2(activity.get());
        root.addView(tabs);
        root.addView(pager, new LinearLayout.LayoutParams(-1, 0, 1));
        activity.get().setContentView(root);
        pager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
                TextView page = new TextView(parent.getContext());
                page.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
                return new RecyclerView.ViewHolder(page) {};
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {}
            @Override public int getItemCount() { return 2; }
        });
        AtomicBoolean enabled = new AtomicBoolean(false);
        List<Integer> selected = new ArrayList<>();
        WorkspaceTabController controller = new WorkspaceTabController(pager, tabs,
                new int[]{R.string.tab_calculate, R.string.graphy}, new WorkspaceTabController.Callback() {
                    @Override public void onTabSelected(int position) { selected.add(position); }
                    @Override public boolean isTabEnabled(int position) { return position == 0 || enabled.get(); }
                });
        root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 360, 600);
        for (int attempt = 0; attempt < 5; attempt++) {
            pager.setCurrentItem(1, false); // Same callback path as swiping to empty Graphy.
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals(0, pager.getCurrentItem());
            assertEquals(0, tabs.getSelectedTabPosition());
            tabs.getTabAt(1).select(); // Also exercise mediator-driven selection.
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals(0, pager.getCurrentItem());
            assertEquals(0, tabs.getSelectedTabPosition());
        }
        assertTrue(pager.beginFakeDrag());
        pager.fakeDragBy(-360);
        pager.endFakeDrag();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));
        assertEquals("disabled swipe must return to the valid page", 0, pager.getCurrentItem());
        assertEquals("tab highlight must match the page after settling", 0, tabs.getSelectedTabPosition());
        assertTrue(tabs.getTabAt(0).view.isSelected());
        assertFalse(tabs.getTabAt(1).view.isSelected());
        assertFalse(selected.contains(1));
        enabled.set(true);
        controller.selectTab(1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, pager.getCurrentItem());
        assertEquals(1, tabs.getSelectedTabPosition());
        assertTrue(selected.contains(1));
        enabled.set(false);
        tabs.getTabAt(0).select();
        tabs.getTabAt(1).select();
        controller.detach(); // Pending restoration must not navigate a destroyed workspace.
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        activity.pause().stop().destroy();
    }
}
