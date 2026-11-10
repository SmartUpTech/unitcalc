package net.smartlogic.unitconverter.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.converter.ConverterPickerDialogFragment;
import net.smartlogic.unitconverter.theme.WindowChrome;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        net.smartlogic.unitconverter.helper.AppLanguage.initializeAfterActivityCreated(this);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.settings_activity);
        WindowChrome.apply(this);
        getSupportFragmentManager().setFragmentResultListener(
                ConverterPickerDialogFragment.RESULT_KEY, this, (key, result) -> {
            if (result.getInt(ConverterPickerDialogFragment.GROUP) == 100) {
                int index = result.getInt(ConverterPickerDialogFragment.INDEX, -1);
                var regions = net.smartlogic.unitconverter.helper.RegionRegistry.ALL;
                if (index >= 0 && index < regions.size())
                    net.smartlogic.unitconverter.helper.Preferences.getInstance(this).applyRegion(regions.get(index).code);
                return;
            }
            var choices = net.smartlogic.unitconverter.helper.PreferredCurrency.choices(this);
            int index = result.getInt(ConverterPickerDialogFragment.INDEX, -1);
            if (index >= 0 && index < choices.size()) net.smartlogic.unitconverter.helper.Preferences.getInstance(this)
                    .setPreferredCurrency(choices.get(index).getCurrencyISOCode());
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeSettings();
            }
        });
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings, new SettingsFragment()).commitNow();
        }
    }

    void closeSettings() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        var preferences = net.smartlogic.unitconverter.helper.Preferences.getInstance(this);
        new net.smartlogic.unitconverter.helper.RegionSuggestion(preferences.getPreferences()).observe(
                net.smartlogic.unitconverter.helper.AppLanguage.current(this), preferences.getRegionalProfile().region);
        WindowChrome.apply(this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            closeSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                                 @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            ComposeView view = new ComposeView(requireContext());
            view.setId(R.id.settings_compose_content);
            SettingsUi.bind(view, this, () -> ((SettingsActivity) requireActivity()).closeSettings());
            return view;
        }
    }
}
