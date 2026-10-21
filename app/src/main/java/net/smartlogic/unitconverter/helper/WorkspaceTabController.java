package net.smartlogic.unitconverter.helper;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Syncs workspace TabLayout with ViewPager2 swipe navigation.
 * Bottom navigation is unaffected — only in-page workspace tabs use this controller.
 *
 * Workspace tabs use {@link R.style#WorkspaceTabLayout}: condensed (scrollable, start-aligned)
 * and shared via {@link R.layout#workspace_tabs_pager} for all calculator hosts.
 */
public final class WorkspaceTabController {

    public interface Callback {
        void onTabSelected(int position);

        boolean isTabEnabled(int position);
    }

    private final ViewPager2 pager;
    private final TabLayout tabLayout;
    private final Callback callback;
    private final TabLayoutMediator mediator;
    private final ViewPager2.OnPageChangeCallback pageCallback;
    private final TabLayout.OnTabSelectedListener tabListener;
    private int lastValidPosition = 0;
    private boolean suppressPageCallback;
    private boolean restorationPosted;
    private boolean detached;
    private final Runnable restoreSelection = this::restoreLastValidSelection;

    private void restoreLastValidSelection() {
        restorationPosted = false;
        if (detached) return;
        int current = pager.getCurrentItem();
        if (callback.isTabEnabled(current) && tabLayout.getSelectedTabPosition() == current) {
            // onPageScrolled can move the visual highlight without selecting a
            // Tab object, even after a rejected swipe has restored the pager.
            tabLayout.setScrollPosition(current, 0f, true);
            return;
        }
        int fallback = lastValidPosition;
        if (!callback.isTabEnabled(fallback)) {
            fallback = -1;
            for (int position = 0; position < tabLayout.getTabCount(); position++) {
                if (callback.isTabEnabled(position)) { fallback = position; break; }
            }
        }
        if (fallback < 0) return;
        lastValidPosition = fallback;
        suppressPageCallback = true;
        try {
            pager.setCurrentItem(fallback, false);
            TabLayout.Tab tab = tabLayout.getTabAt(fallback);
            if (tab != null && tabLayout.getSelectedTabPosition() != fallback) tab.select();
            tabLayout.setScrollPosition(fallback, 0f, true);
        } finally {
            suppressPageCallback = false;
        }
        if (!detached) callback.onTabSelected(fallback);
    }

    public WorkspaceTabController(@NonNull ViewPager2 pager,
                                  @NonNull TabLayout tabLayout,
                                  @NonNull @StringRes int[] tabTitleResIds,
                                  @NonNull Callback callback) {
        this.pager = pager;
        this.tabLayout = tabLayout;
        this.callback = callback;

        pager.setOffscreenPageLimit(tabTitleResIds.length - 1);

        pageCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (suppressPageCallback) {
                    return;
                }
                if (!callback.isTabEnabled(position)) {
                    revertToLastValidTab();
                    return;
                }
                lastValidPosition = position;
                callback.onTabSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state != ViewPager2.SCROLL_STATE_IDLE || suppressPageCallback) {
                    return;
                }
                revertToLastValidTab();
            }
        };
        pager.registerOnPageChangeCallback(pageCallback);

        tabListener = new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (!callback.isTabEnabled(position)) {
                    revertToLastValidTab();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        };
        tabLayout.addOnTabSelectedListener(tabListener);

        mediator = new TabLayoutMediator(tabLayout, pager, (tab, position) ->
                tab.setText(tabTitleResIds[position]));
        mediator.attach();
    }

    public void selectTab(int position) {
        if (detached || position < 0 || position >= tabLayout.getTabCount()) return;
        if (!callback.isTabEnabled(position)) {
            revertToLastValidTab();
            return;
        }
        if (pager.getCurrentItem() == position) {
            callback.onTabSelected(position);
            return;
        }
        pager.setCurrentItem(position, true);
    }

    public int getCurrentTab() {
        return pager.getCurrentItem();
    }

    public void detach() {
        if (detached) return;
        detached = true;
        pager.removeCallbacks(restoreSelection);
        restorationPosted = false;
        mediator.detach();
        pager.unregisterOnPageChangeCallback(pageCallback);
        tabLayout.removeOnTabSelectedListener(tabListener);
    }

    private void revertToLastValidTab() {
        // Mediator selection is still dispatching here. Restoring synchronously
        // lets its remaining listeners select the rejected page again forever.
        if (detached || restorationPosted) return;
        restorationPosted = true;
        pager.post(restoreSelection);
    }
}
