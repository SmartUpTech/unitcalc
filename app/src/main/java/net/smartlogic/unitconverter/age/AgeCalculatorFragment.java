package net.smartlogic.unitconverter.age;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.graphy.compose.GraphyPanelController;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import net.smartlogic.unitconverter.helper.InteractionFeedbackManager;
import net.smartlogic.unitconverter.helper.WorkspacePagerAdapter;
import net.smartlogic.unitconverter.helper.WorkspaceTabController;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public final class AgeCalculatorFragment extends Fragment {
    private LocalDate birth;
    private LocalDate asOf = LocalDate.now();
    private boolean followsToday = true;
    private int selectedTab;
    private View calculatePage;
    private ViewPager2 pager;
    private WorkspaceTabController tabs;
    private GraphyPanelController graphy;
    private AgeCalculator.Result result;
    private GraphyOutput output;
    private OnBackPressedCallback back;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable midnight = () -> { render(); scheduleMidnight(); };
    private final ThemeManager.Listener themeListener = theme -> {
        if (getView() != null) { ThemeApplier.apply(getView()); render(); }
    };

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (state != null) {
            String value = state.getString("birth");
            birth = value == null ? null : LocalDate.parse(value);
            asOf = LocalDate.parse(state.getString("asof", LocalDate.now().toString()));
            followsToday = state.getBoolean("today", true);
            selectedTab = state.getInt("tab", 0);
        }
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_calculator, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        back = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { tabs.selectTab(0); }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), back);
        getChildFragmentManager().setFragmentResultListener(AgeDialogFragment.RESULT, getViewLifecycleOwner(), (key, choice) -> {
            String mode = choice.getString("mode");
            LocalDate date = LocalDate.parse(choice.getString("date"));
            if (AgeDialogFragment.CALENDAR.equals(mode)) {
                try { startActivity(AgeCalendar.createIntent(requireContext(), date, LocalDate.now(), choice.getBoolean("yearly"))); }
                catch (ActivityNotFoundException e) { Toast.makeText(requireContext(), R.string.age_calendar_unavailable, Toast.LENGTH_LONG).show(); }
                return;
            }
            if (AgeDialogFragment.BIRTH.equals(mode)) birth = date;
            else { asOf = date; followsToday = false; }
            render();
            scheduleMidnight();
        });
        pager = view.findViewById(R.id.workspace_pager);
        // Dates use modal pickers; swiping the shared workspace still works normally.
        pager.setAdapter(new WorkspacePagerAdapter(new int[]{R.layout.pane_age_calculate, R.layout.pane_calculator_graphy}, (position, page) -> {
            ThemeApplier.apply(page);
            if (position == 0) {
                calculatePage = page;
                action(R.id.age_birth_button, () -> openDialog(AgeDialogFragment.BIRTH, birth == null ? asOf : birth));
                action(R.id.age_asof_button, () -> openDialog(AgeDialogFragment.AS_OF, asOf));
                action(R.id.age_today_button, () -> { followsToday = true; render(); scheduleMidnight(); });
                action(R.id.age_share, this::share);
                action(R.id.age_calendar, () -> { if (result != null) openDialog(AgeDialogFragment.CALENDAR, result.birth()); });
            } else {
                page.findViewById(R.id.graphy_context).setVisibility(View.GONE);
                graphy = new GraphyPanelController((ComposeView) page.findViewById(R.id.graphy_panel), this, new GraphyViewTheme(requireContext()));
            }
            render();
            scheduleMidnight();
        }));
        int restoreTab = selectedTab;
        tabs = new WorkspaceTabController(pager, view.findViewById(R.id.workspace_tabs),
                new int[]{R.string.tab_calculate, R.string.graphy}, new WorkspaceTabController.Callback() {
            @Override public void onTabSelected(int position) { selectedTab = position; updateBack(); }
            @Override public boolean isTabEnabled(int position) { return position == 0 || result != null; }
        });
        render();
        if (restoreTab == 1 && result != null) { selectedTab = 1; tabs.selectTab(1); }
        ThemeManager.addListener(themeListener);
        ThemeApplier.apply(view);
    }

    private void action(int id, Runnable action) {
        View view = calculatePage.findViewById(id);
        InteractionFeedbackManager.configure(view);
        view.setOnClickListener(v -> action.run());
    }

    private void openDialog(String mode, LocalDate date) {
        if (getChildFragmentManager().isStateSaved() || getChildFragmentManager().findFragmentByTag(AgeDialogFragment.TAG) != null) return;
        AgeDialogFragment.create(mode, date).showNow(getChildFragmentManager(), AgeDialogFragment.TAG);
    }

    private void render() {
        if (!isAdded()) return;
        if (followsToday) asOf = LocalDate.now();
        result = null;
        if (birth != null) {
            try { result = AgeCalculator.calculate(birth, asOf); }
            catch (IllegalArgumentException ignored) { /* Explicit inline invalid state below. */ }
        }
        AgeFormatter f = new AgeFormatter(requireContext());
        output = result == null ? null : AgeGraphBuilder.build(requireContext(), result);
        if (graphy != null) graphy.update(output);
        if (tabs != null && result == null && selectedTab != 0) tabs.selectTab(0);
        if (getView() != null) {
            TabLayout.Tab tab = ((TabLayout) getView().findViewById(R.id.workspace_tabs)).getTabAt(1);
            if (tab != null) { tab.view.setEnabled(result != null); tab.view.setAlpha(result == null ? 0.5f : 1f); }
        }
        if (calculatePage == null) return;
        text(R.id.age_birth_button, getString(R.string.age_date_button, getString(R.string.age_birth), birth == null ? getString(R.string.age_select_date) : f.date(birth)));
        text(R.id.age_asof_button, getString(R.string.age_date_button, getString(R.string.age_as_of), f.date(asOf)));
        text(R.id.age_note, getString(result != null ? R.string.age_totals_note : birth == null ? R.string.age_empty : R.string.age_invalid));
        calculatePage.findViewById(R.id.age_note).setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        int[] totals = {R.id.age_total_years, R.id.age_total_months, R.id.age_total_weeks, R.id.age_total_days, R.id.age_total_hours, R.id.age_total_minutes};
        long[] values = result == null ? new long[6] : new long[]{result.age().years(), result.totalMonths(), result.totalWeeks(), result.totalDays(), result.totalHours(), result.totalMinutes()};
        for (int i = 0; i < totals.length; i++) text(totals[i], result == null ? "" : f.number(values[i]));
        text(R.id.age_years, result == null ? "" : f.years(result.age().years()));
        text(R.id.age_remainder, result == null ? "" : getString(R.string.age_remaining, f.months(result.age().months()), f.days(result.age().days())));
        text(R.id.age_birthday_details, result == null ? "" : getString(R.string.age_birthday_details,
                f.date(result.nextBirthday()), f.weekday(result.nextBirthday()), f.countdown(result)));
        calculatePage.findViewById(R.id.age_calendar).setEnabled(result != null);
        calculatePage.findViewById(R.id.age_share).setEnabled(result != null);
        ThemeApplier.apply(calculatePage);
        updateBack();
    }
    private void text(int id, String value) { ((TextView) calculatePage.findViewById(id)).setText(value); }
    private void share() {
        if (result == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, new AgeFormatter(requireContext()).share(result));
        try { startActivity(Intent.createChooser(intent, getString(R.string.share))); }
        catch (ActivityNotFoundException e) { Toast.makeText(requireContext(), R.string.age_share_unavailable, Toast.LENGTH_LONG).show(); }
    }
    private void updateBack() { if (back != null) back.setEnabled(isResumed() && !isHidden() && selectedTab == 1); }
    private void scheduleMidnight() {
        handler.removeCallbacks(midnight);
        if (!isResumed() || isHidden() || !followsToday) return;
        ZonedDateTime now = ZonedDateTime.now();
        handler.postDelayed(midnight, Math.max(1, Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.getZone())).toMillis()));
    }
    @Override public void onResume() { super.onResume(); render(); updateBack(); scheduleMidnight(); }
    @Override public void onPause() { handler.removeCallbacks(midnight); super.onPause(); updateBack(); }
    @Override public void onHiddenChanged(boolean hidden) { super.onHiddenChanged(hidden); updateBack(); if (hidden) handler.removeCallbacks(midnight); else { render(); scheduleMidnight(); } }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (birth != null) out.putString("birth", birth.toString());
        out.putString("asof", asOf.toString()); out.putBoolean("today", followsToday); out.putInt("tab", selectedTab);
    }
    @Override public void onDestroyView() {
        handler.removeCallbacks(midnight); ThemeManager.removeListener(themeListener);
        if (tabs != null) tabs.detach();
        tabs = null; pager = null; graphy = null; calculatePage = null; back = null;
        super.onDestroyView();
    }
}
