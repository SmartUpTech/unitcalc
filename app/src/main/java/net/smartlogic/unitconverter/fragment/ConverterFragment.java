package net.smartlogic.unitconverter.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.app.AppConst;

/**
 * Hosts unit and currency converters behind a single Converter tab.
 */
public class ConverterFragment extends Fragment {

    public static final int TAB_UNIT = 0;
    public static final int TAB_CURRENCY = 1;

    private static final String TAG_UNIT = "converter_unit";
    private static final String TAG_CURRENCY = "converter_currency";

    private TabLayout tabLayout;
    private UnitConverterFragment unitFragment;
    private CurrencyConverterFragment currencyFragment;
    private Fragment activeChild;
    private boolean childFragmentsInitialized;

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
        tabLayout = view.findViewById(R.id.converter_tabs);
        TabLayout workspaceTabs = view.findViewById(R.id.workspace_tabs);
        View convertPane = view.findViewById(R.id.convert_pane);
        View graphyPane = view.findViewById(R.id.graphy_pane);
        View historyPane = view.findViewById(R.id.history_pane);

        if (workspaceTabs.getTabCount() == 0) {
            workspaceTabs.addTab(workspaceTabs.newTab().setText(R.string.tab_convert));
            workspaceTabs.addTab(workspaceTabs.newTab().setText(R.string.graphy));
            workspaceTabs.addTab(workspaceTabs.newTab().setText(R.string.nav_history));
        }
        workspaceTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                convertPane.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                graphyPane.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                historyPane.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        if (tabLayout.getTabCount() == 0) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.unit_converter));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.currency_converter));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showConverterTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        ensureChildFragments();
        int selectedTab = AppConst.CONVERTER_TAB;
        tabLayout.getTabAt(selectedTab).select();
        showConverterTab(selectedTab);
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
