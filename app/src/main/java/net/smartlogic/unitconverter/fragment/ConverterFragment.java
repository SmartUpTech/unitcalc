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

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.tabs.TabLayout;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.app.AppConst;
import net.smartlogic.unitconverter.graphy.compose.GraphyPanelController;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.graphy.renderer.FlowchartRenderer;
import net.smartlogic.unitconverter.graphy.renderer.GraphyRenderer;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;

/**
 * Hosts unit and currency converters behind a single Converter tab.
 */
public class ConverterFragment extends Fragment {

    public static final int TAB_UNIT = 0;
    public static final int TAB_CURRENCY = 1;

    private static final String TAG_UNIT = "converter_unit";
    private static final String TAG_CURRENCY = "converter_currency";

    private MaterialButtonToggleGroup modeToggle;
    private UnitConverterFragment unitFragment;
    private CurrencyConverterFragment currencyFragment;
    private Fragment activeChild;
    private boolean childFragmentsInitialized;

    private TabLayout workspaceTabs;
    private View convertPane;
    private View graphyPane;
    private View historyPane;
    private TextView graphyContext;
    private TextView graphyEmpty;
    private ComposeView graphyPanel;
    private GraphyPanelController graphyPanelController;
    private GraphyViewTheme graphyTheme;
    private final GraphyRenderer graphyRenderer = new FlowchartRenderer();
    private GraphyOutput latestGraphyOutput;
    private int lastWorkspaceTab;

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
        modeToggle = view.findViewById(R.id.converter_mode_toggle);
        workspaceTabs = view.findViewById(R.id.workspace_tabs);
        convertPane = view.findViewById(R.id.convert_pane);
        graphyPane = view.findViewById(R.id.graphy_pane);
        historyPane = view.findViewById(R.id.history_pane);
        graphyContext = view.findViewById(R.id.graphy_context);
        graphyEmpty = view.findViewById(R.id.graphy_empty);
        graphyPanel = view.findViewById(R.id.graphy_panel);
        graphyTheme = new GraphyViewTheme(requireContext());
        graphyPanelController = new GraphyPanelController(graphyPanel, this, graphyTheme);

        if (workspaceTabs.getTabCount() == 0) {
            workspaceTabs.addTab(workspaceTabs.newTab().setText(R.string.tab_convert));
            workspaceTabs.addTab(workspaceTabs.newTab().setText(R.string.graphy));
            workspaceTabs.addTab(workspaceTabs.newTab().setText(R.string.nav_history));
        }
        workspaceTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1 && !hasGraphyContent()) {
                    selectWorkspaceTab(lastWorkspaceTab);
                    return;
                }
                lastWorkspaceTab = tab.getPosition();
                applyWorkspaceTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        updateGraphyTabState();

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
    }

    public void updateConversionGraphy(@NonNull GraphyOutput output, @NonNull String contextLine) {
        latestGraphyOutput = output;
        updateGraphyTabState();
        if (graphyPane.getVisibility() == View.VISIBLE && hasGraphyContent()) {
            showGraphy(output, contextLine);
        }
    }

    public void clearConversionGraphy() {
        latestGraphyOutput = null;
        updateGraphyTabState();
        graphyPanelController.update("", null);
        graphyContext.setText("");
    }

    private boolean hasGraphyContent() {
        return latestGraphyOutput != null && graphyRenderer.supports(latestGraphyOutput);
    }

    private void updateGraphyTabState() {
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
        TabLayout.Tab tab = workspaceTabs.getTabAt(index);
        if (tab != null) {
            tab.select();
        }
    }

    private void applyWorkspaceTab(int position) {
        convertPane.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        graphyPane.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        historyPane.setVisibility(position == 2 ? View.VISIBLE : View.GONE);

        if (position == 1) {
            if (hasGraphyContent()) {
                String context = latestGraphyOutput.getExpression() + " = " + latestGraphyOutput.getResult();
                showGraphy(latestGraphyOutput, context);
            } else {
                graphyEmpty.setVisibility(View.VISIBLE);
                graphyPanel.setVisibility(View.GONE);
                graphyContext.setText("");
            }
        }
    }

    private void showGraphy(@NonNull GraphyOutput output, @NonNull String contextLine) {
        graphyEmpty.setVisibility(View.GONE);
        graphyPanel.setVisibility(View.VISIBLE);
        graphyContext.setText(contextLine);
        String explanation = output.getExplanationTemplate();
        if (explanation == null) {
            explanation = "";
        }
        graphyPanelController.update(explanation, output);
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
