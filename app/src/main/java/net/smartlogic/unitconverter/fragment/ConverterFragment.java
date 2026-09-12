package net.smartlogic.unitconverter.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.tabs.TabLayout;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.app.AppConst;
import net.smartlogic.unitconverter.graphy.compose.GraphyPanelController;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.graphy.renderer.FlowchartRenderer;
import net.smartlogic.unitconverter.graphy.renderer.GraphyRenderer;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.helper.WorkspacePagerAdapter;
import net.smartlogic.unitconverter.helper.WorkspaceTabController;
import net.smartlogic.unitconverter.theme.ThemeApplier;

import java.util.ArrayList;
import java.util.List;

/**
 * Hosts unit and currency converters behind a single Converter tab.
 */
public class ConverterFragment extends Fragment {

    public static final int TAB_UNIT = 0;
    public static final int TAB_CURRENCY = 1;

    private static final String TAG_UNIT = "converter_unit";
    private static final String TAG_CURRENCY = "converter_currency";

    private static final int[] WORKSPACE_LAYOUTS = {
            R.layout.pane_converter_convert,
            R.layout.pane_converter_graphy
    };

    private static final int[] WORKSPACE_TAB_TITLES = {
            R.string.tab_convert,
            R.string.graphy
    };

    private MaterialButtonToggleGroup modeToggle;
    private UnitConverterFragment unitFragment;
    private CurrencyConverterFragment currencyFragment;
    private Fragment activeChild;
    private boolean childFragmentsInitialized;

    private TabLayout workspaceTabs;
    private ViewPager2 workspacePager;
    private WorkspaceTabController workspaceTabController;
    private TextView graphyContext;
    private TextView graphyEmpty;
    private ComposeView graphyPanel;
    private GraphyPanelController graphyPanelController;
    private GraphyViewTheme graphyTheme;
    private final GraphyRenderer graphyRenderer = new FlowchartRenderer();
    private GraphyOutput latestGraphyOutput;
    private int lastWorkspaceTab;
    private boolean convertPageBound;
    private boolean graphyPageBound;
    private boolean pendingCurrencyMode;
    private final List<Runnable> pendingReadyActions = new ArrayList<>();

    public interface ReadyAction {
        void run(@NonNull ConverterFragment fragment);
    }

    public ConverterFragment() {
    }

    public static ConverterFragment newInstance() {
        return new ConverterFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_converter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        workspaceTabs = view.findViewById(R.id.workspace_tabs);
        workspacePager = view.findViewById(R.id.workspace_pager);
        graphyTheme = new GraphyViewTheme(requireContext());

        WorkspacePagerAdapter pagerAdapter = new WorkspacePagerAdapter(
                WORKSPACE_LAYOUTS,
                this::bindWorkspacePage
        );
        workspacePager.setAdapter(pagerAdapter);

        workspaceTabController = new WorkspaceTabController(
                workspacePager,
                workspaceTabs,
                WORKSPACE_TAB_TITLES,
                new WorkspaceTabController.Callback() {
                    @Override
                    public void onTabSelected(int position) {
                        lastWorkspaceTab = position;
                        applyWorkspaceTab(position);
                    }

                    @Override
                    public boolean isTabEnabled(int position) {
                        return position != 1 || hasGraphyContent();
                    }
                }
        );
        updateGraphyTabState();
        ThemeApplier.apply(view);
        ensureChildFragments();
        if (pendingOpenConvertWorkspace) {
            openConvertWorkspace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            ThemeApplier.apply(getView());
        }
    }

    @Override
    public void onDestroyView() {
        if (workspaceTabController != null) {
            workspaceTabController.detach();
            workspaceTabController = null;
        }
        convertPageBound = false;
        graphyPageBound = false;
        modeToggle = null;
        graphyPanelController = null;
        graphyPanel = null;
        graphyEmpty = null;
        graphyContext = null;
        childFragmentsInitialized = false;
        pendingReadyActions.clear();
        super.onDestroyView();
    }

    private void bindWorkspacePage(int position, @NonNull View pageView) {
        ThemeApplier.apply(pageView);
        if (position == 0) {
            bindConvertPage(pageView);
        } else if (position == 1) {
            bindGraphyPage(pageView);
        }
    }

    private void bindConvertPage(@NonNull View pageView) {
        if (convertPageBound) {
            return;
        }
        convertPageBound = true;

        View container = getView() != null ? getView().findViewById(R.id.converter_container) : null;
        ViewGroup placeholder = pageView.findViewById(R.id.converter_placeholder);
        if (container != null && placeholder != null) {
            ViewGroup parent = (ViewGroup) container.getParent();
            if (parent != null && parent != placeholder) {
                parent.removeView(container);
            }
            if (container.getParent() == null) {
                container.setVisibility(View.VISIBLE);
                ViewGroup.LayoutParams lp = container.getLayoutParams();
                if (lp == null) {
                    lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                } else {
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                }
                placeholder.addView(container, lp);
            }
        }

        modeToggle = pageView.findViewById(R.id.converter_mode_toggle);
        modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btn_currency_mode) {
                showConverterTab(TAB_CURRENCY);
            } else {
                showConverterTab(TAB_UNIT);
            }
        });

        ensureChildFragments();
        int selectedTab = AppConst.CONVERTER_TAB;
        if (selectedTab == TAB_CURRENCY) {
            modeToggle.check(R.id.btn_currency_mode);
        } else {
            modeToggle.check(R.id.btn_unit_mode);
        }
        showConverterTab(selectedTab);
        applyPendingNavigation();
        flushPendingReadyActions();
    }

    public void runWhenReady(@NonNull ReadyAction action) {
        if (convertPageBound && modeToggle != null) {
            action.run(this);
            return;
        }
        pendingReadyActions.add(() -> action.run(this));
        openConvertWorkspace();
    }

    private boolean pendingOpenConvertWorkspace;

    public void openConvertWorkspace() {
        if (workspaceTabController != null) {
            selectWorkspaceTab(0);
            pendingOpenConvertWorkspace = false;
        } else {
            pendingOpenConvertWorkspace = true;
        }
    }

    public void openUnitCategory(int categoryId) {
        pendingUnitCategory = categoryId;
        pendingCurrencyMode = false;
        openConvertWorkspace();
        if (modeToggle != null) {
            applyPendingNavigation();
        }
    }

    public void openCurrencyMode() {
        pendingCurrencyMode = true;
        pendingUnitCategory = -1;
        openConvertWorkspace();
        if (modeToggle != null) {
            applyPendingNavigation();
        }
    }

    public void openUnitMode() {
        pendingCurrencyMode = false;
        pendingUnitCategory = -1;
        openConvertWorkspace();
        if (modeToggle != null) {
            modeToggle.check(R.id.btn_unit_mode);
            showConverterTab(TAB_UNIT);
        }
    }

    private void applyPendingNavigation() {
        if (modeToggle == null) {
            return;
        }
        if (pendingCurrencyMode) {
            modeToggle.check(R.id.btn_currency_mode);
            showConverterTab(TAB_CURRENCY);
            pendingCurrencyMode = false;
            return;
        }
        if (pendingUnitCategory >= 0) {
            modeToggle.check(R.id.btn_unit_mode);
            showConverterTab(TAB_UNIT);
            applyPendingUnitCategory();
        }
    }

    private void flushPendingReadyActions() {
        if (!convertPageBound || modeToggle == null || pendingReadyActions.isEmpty()) {
            return;
        }
        List<Runnable> actions = new ArrayList<>(pendingReadyActions);
        pendingReadyActions.clear();
        for (Runnable action : actions) {
            action.run();
        }
    }

    private void bindGraphyPage(@NonNull View pageView) {
        if (graphyPageBound) {
            return;
        }
        graphyPageBound = true;

        graphyContext = pageView.findViewById(R.id.graphy_context);
        graphyEmpty = pageView.findViewById(R.id.graphy_empty);
        graphyPanel = pageView.findViewById(R.id.graphy_panel);
        pageView.post(() -> {
            if (!isAdded() || graphyPanel == null || graphyPanelController != null) {
                return;
            }
            graphyPanelController = new GraphyPanelController(graphyPanel, this, graphyTheme);
        });
    }

    private int pendingUnitCategory = -1;

    @NonNull
    public String getActiveCatalogId() {
        if (activeChild == currencyFragment) {
            return net.smartlogic.unitconverter.model.CalculatorCatalog.ID_CURRENCY;
        }
        if (unitFragment != null) {
            return net.smartlogic.unitconverter.model.CalculatorCatalog.unitId(unitFragment.getSelectedCategoryId());
        }
        return net.smartlogic.unitconverter.model.CalculatorCatalog.unitId(
                Preferences.getInstance(requireContext()).getLastConversion());
    }

    private void applyPendingUnitCategory() {
        if (pendingUnitCategory >= 0 && unitFragment != null) {
            unitFragment.selectCategory(pendingUnitCategory);
            pendingUnitCategory = -1;
        }
    }

    public void updateConversionGraphy(@NonNull GraphyOutput output, @NonNull String contextLine) {
        latestGraphyOutput = output;
        updateGraphyTabState();
        if (workspaceTabController != null
                && workspaceTabController.getCurrentTab() == 1
                && hasGraphyContent()) {
            showGraphy(output, contextLine);
        }
    }

    public void clearConversionGraphy() {
        latestGraphyOutput = null;
        updateGraphyTabState();
        if (graphyPanelController != null) {
            graphyPanelController.update("", null);
        }
        if (graphyContext != null) {
            graphyContext.setText("");
        }
    }

    private boolean hasGraphyContent() {
        return latestGraphyOutput != null && graphyRenderer.supports(latestGraphyOutput);
    }

    private void updateGraphyTabState() {
        if (workspaceTabs == null) {
            return;
        }
        TabLayout.Tab graphyTab = workspaceTabs.getTabAt(1);
        if (graphyTab == null) {
            return;
        }
        boolean enabled = hasGraphyContent();
        View tabView = graphyTab.view;
        tabView.setEnabled(enabled);
        tabView.setAlpha(enabled ? 1f : 0.4f);
    }

    private void selectWorkspaceTab(int index) {
        if (workspaceTabController != null) {
            workspaceTabController.selectTab(index);
        }
    }

    private void applyWorkspaceTab(int position) {
        if (position == 1) {
            if (!hasGraphyContent()) {
                selectWorkspaceTab(0);
                return;
            }
            String contextLine = latestGraphyOutput.getExpression();
            if (contextLine == null) {
                contextLine = "";
            }
            showGraphy(latestGraphyOutput, contextLine);
        }
    }

    private void showGraphy(@NonNull GraphyOutput output, @NonNull String contextLine) {
        if (graphyPanelController == null) {
            return;
        }
        if (graphyEmpty != null) {
            graphyEmpty.setVisibility(View.GONE);
        }
        if (graphyPanel != null) {
            graphyPanel.setVisibility(View.VISIBLE);
        }
        if (graphyContext != null) {
            graphyContext.setVisibility(View.GONE);
        }
        graphyPanelController.update(contextLine, output);
    }

    private void ensureChildFragments() {
        if (childFragmentsInitialized) {
            return;
        }

        unitFragment = (UnitConverterFragment) getChildFragmentManager().findFragmentByTag(TAG_UNIT);
        currencyFragment = (CurrencyConverterFragment) getChildFragmentManager().findFragmentByTag(TAG_CURRENCY);

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (unitFragment == null) {
            unitFragment = UnitConverterFragment.newInstance();
            transaction.add(R.id.converter_container, unitFragment, TAG_UNIT);
        }
        if (currencyFragment == null) {
            currencyFragment = CurrencyConverterFragment.newInstance();
            transaction.add(R.id.converter_container, currencyFragment, TAG_CURRENCY);
        }
        transaction.commitNow();
        childFragmentsInitialized = true;
    }

    private void showConverterTab(int tabIndex) {
        ensureChildFragments();
        AppConst.CONVERTER_TAB = tabIndex;

        Fragment target = tabIndex == TAB_CURRENCY ? currencyFragment : unitFragment;
        if (target == activeChild) {
            return;
        }

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (activeChild != null) {
            transaction.hide(activeChild);
        }
        transaction.show(target);
        transaction.commit();
        activeChild = target;
    }
}
