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
    private int lastValidPosition = 0;
    private boolean suppressPageCallback;

    public WorkspaceTabController(@NonNull ViewPager2 pager,
                                  @NonNull TabLayout tabLayout,
                                  @NonNull @StringRes int[] tabTitleResIds,
                                  @NonNull Callback callback) {
        this.pager = pager;
        this.tabLayout = tabLayout;
        this.callback = callback;

        pager.setOffscreenPageLimit(tabTitleResIds.length - 1);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (suppressPageCallback) {
                    return;
                }
                if (!callback.isTabEnabled(position)) {
                    int skipTarget = findSkipTarget(position, lastValidPosition);
                    if (skipTarget >= 0) {
                        suppressPageCallback = true;
                        pager.setCurrentItem(skipTarget, false);
                        suppressPageCallback = false;
                        lastValidPosition = skipTarget;
                        callback.onTabSelected(skipTarget);
                    } else {
                        revertToLastValidTab();
                    }
                    return;
                }
                lastValidPosition = position;
                callback.onTabSelected(position);
            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (!callback.isTabEnabled(position)) {
                    revertTabSelection();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        mediator = new TabLayoutMediator(tabLayout, pager, (tab, position) ->
                tab.setText(tabTitleResIds[position]));
        mediator.attach();
    }

    public void selectTab(int position) {
        if (!callback.isTabEnabled(position)) {
            revertTabSelection();
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
        mediator.detach();
    }

    private void revertToLastValidTab() {
        suppressPageCallback = true;
        pager.setCurrentItem(lastValidPosition, false);
        suppressPageCallback = false;
        revertTabSelection();
    }

    private void revertTabSelection() {
        TabLayout.Tab tab = tabLayout.getTabAt(lastValidPosition);
        if (tab != null) {
            tab.select();
        }
    }

    private int findSkipTarget(int disabledPosition, int originPosition) {
        int pageCount = pager.getAdapter() != null ? pager.getAdapter().getItemCount() : 0;
        int forward = disabledPosition + 1;
        if (disabledPosition > originPosition
                && forward < pageCount
                && callback.isTabEnabled(forward)) {
            return forward;
        }
        int backward = disabledPosition - 1;
        if (disabledPosition < originPosition
                && backward >= 0
                && callback.isTabEnabled(backward)) {
            return backward;
        }
        return -1;
    }
}
