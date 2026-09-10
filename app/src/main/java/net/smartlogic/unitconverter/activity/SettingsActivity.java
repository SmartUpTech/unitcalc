package net.smartlogic.unitconverter.activity;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.smartlogic.unitconverter.BuildConfig;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.theme.CalculatorTheme;
import net.smartlogic.unitconverter.theme.CalculatorThemes;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeChipAdapter;
import net.smartlogic.unitconverter.theme.ThemeManager;
import net.smartlogic.unitconverter.theme.WindowChrome;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.title_activity_settings);
            actionBar.setElevation(0f);
        }
        WindowChrome.apply(this);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commitNow();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        WindowChrome.apply(this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends Fragment {

        private Preferences preferences;
        private ThemeChipAdapter themeChipAdapter;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_settings, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            preferences = Preferences.getInstance(requireContext());

            bindThemeRow(view.findViewById(R.id.row_theme));

            bindChoiceRow(
                    view.findViewById(R.id.row_number_decimals),
                    getString(R.string.prefs_title_number_decimals),
                    getString(R.string.prefs_summary_number_decimals),
                    preferences.getPreferences().getString(
                            Preferences.PREFS_NUMBER_OF_DECIMALS,
                            getString(R.string.default_number_decimals)),
                    R.array.number_decimals,
                    Preferences.PREFS_NUMBER_OF_DECIMALS);

            bindChoiceRow(
                    view.findViewById(R.id.row_group_separator),
                    getString(R.string.prefs_title_group_separator),
                    getString(R.string.prefs_summary_group_separator),
                    displayGroupSeparator(preferences.getGroupSeparator()),
                    R.array.group_separators,
                    Preferences.PREFS_GROUP_SEPARATOR);

            bindChoiceRow(
                    view.findViewById(R.id.row_decimal_separator),
                    getString(R.string.prefs_title_decimal_separator),
                    getString(R.string.prefs_summary_decimal_separator),
                    preferences.getDecimalSeparator(),
                    R.array.decimal_separators,
                    Preferences.PREFS_DECIMAL_SEPARATOR);

            bindActionRow(
                    view.findViewById(R.id.row_rate),
                    getString(R.string.pref_rate_title),
                    getString(R.string.pref_rate_summary),
                    () -> startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(getString(R.string.url_app_link)))));

            bindActionRow(
                    view.findViewById(R.id.row_share),
                    getString(R.string.pref_share_title),
                    getString(R.string.pref_share_summary),
                    this::shareApp);

            bindActionRow(
                    view.findViewById(R.id.row_website),
                    getString(R.string.pref_website_title),
                    getString(R.string.pref_website_summary),
                    () -> startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(getString(R.string.url_smartup_website)))));

            bindActionRow(
                    view.findViewById(R.id.row_privacy),
                    getString(R.string.pref_privacy_title),
                    getString(R.string.pref_privacy_summary),
                    () -> startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(getString(R.string.url_privacy_policy)))));

            View versionRow = view.findViewById(R.id.row_version);
            TextView versionTitle = versionRow.findViewById(R.id.tv_info_title);
            TextView versionValue = versionRow.findViewById(R.id.tv_info_value);
            versionTitle.setText(R.string.pref_version_title);
            versionValue.setText(BuildConfig.VERSION_NAME);

            applySettingsTheme(view);
        }

        private void bindThemeRow(@NonNull View row) {
            TextView title = row.findViewById(R.id.tv_theme_title);
            title.setText(R.string.prefs_title_theme);
            RecyclerView chips = row.findViewById(R.id.rv_theme_chips);
            chips.setLayoutManager(new LinearLayoutManager(
                    requireContext(), LinearLayoutManager.HORIZONTAL, false));
            chips.setHasFixedSize(false);
            themeChipAdapter = new ThemeChipAdapter(
                    CalculatorThemes.all(),
                    ThemeManager.get().id(),
                    this::onThemeSelected);
            chips.setAdapter(themeChipAdapter);
        }

        private void onThemeSelected(@NonNull CalculatorTheme theme) {
            if (theme.id().equals(ThemeManager.get().id())) {
                return;
            }
            ThemeManager.select(requireContext(), theme.id());
            if (getActivity() != null) {
                getActivity().recreate();
            }
        }

        private void applySettingsTheme(@NonNull View view) {
            CalculatorTheme theme = ThemeManager.get();
            view.setBackgroundColor(theme.background());
            ThemeApplier.apply(view);
            int[] cardIds = {R.id.card_display, R.id.card_feedback, R.id.card_information};
            float radius = 16f * getResources().getDisplayMetrics().density;
            int stroke = Math.round(getResources().getDisplayMetrics().density);
            for (int id : cardIds) {
                View card = view.findViewById(id);
                if (card != null) {
                    GradientDrawable drawable = ThemeApplier.roundedFill(theme.background(), radius);
                    drawable.setStroke(stroke, theme.functions());
                    card.setBackground(drawable);
                }
            }
            int[] sectionIds = {
                    R.id.tv_section_display, R.id.tv_section_feedback, R.id.tv_section_information
            };
            for (int id : sectionIds) {
                TextView section = view.findViewById(id);
                if (section != null) {
                    section.setTextColor(theme.mainText());
                }
            }
            TextView themeTitle = view.findViewById(R.id.tv_theme_title);
            if (themeTitle != null) {
                themeTitle.setTextColor(theme.mainText());
            }
        }

        private void bindChoiceRow(@NonNull View row, @NonNull String title, @NonNull String summary,
                                   @NonNull String currentValue, int entriesArrayRes,
                                   @NonNull String preferenceKey) {
            TextView titleView = row.findViewById(R.id.tv_choice_title);
            TextView summaryView = row.findViewById(R.id.tv_choice_summary);
            TextView valueView = row.findViewById(R.id.tv_choice_value);
            titleView.setText(title);
            summaryView.setText(summary);
            summaryView.setVisibility(View.VISIBLE);
            valueView.setText(currentValue);

            String[] entries = getResources().getStringArray(entriesArrayRes);
            String[] values = getResources().getStringArray(entriesArrayRes);
            row.setOnClickListener(v -> showSingleChoiceDialog(
                    title, entries, values, preferenceKey, valueView));
        }

        private void bindActionRow(@NonNull View row, @NonNull String title, @NonNull String summary,
                                   @NonNull Runnable action) {
            TextView titleView = row.findViewById(R.id.tv_action_title);
            TextView summaryView = row.findViewById(R.id.tv_action_summary);
            titleView.setText(title);
            summaryView.setText(summary);
            row.setOnClickListener(v -> action.run());
        }

        private void showSingleChoiceDialog(@NonNull String title, @NonNull String[] entries,
                                            @NonNull String[] values, @NonNull String preferenceKey,
                                            @NonNull TextView valueView) {
            String current = preferences.getPreferences().getString(preferenceKey, values[0]);
            int checked = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(current)) {
                    checked = i;
                    break;
                }
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle(title)
                    .setSingleChoiceItems(entries, checked, (dialog, which) -> {
                        String selected = values[which];
                        preferences.getPreferences().edit()
                                .putString(preferenceKey, selected)
                                .apply();
                        if (Preferences.PREFS_GROUP_SEPARATOR.equals(preferenceKey)) {
                            valueView.setText(displayGroupSeparator(selected));
                        } else {
                            valueView.setText(selected);
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        @NonNull
        private String displayGroupSeparator(@NonNull String stored) {
            if ("None".equals(stored) || stored.isEmpty()) {
                return getString(R.string.group_separator_none);
            }
            return stored;
        }

        private void shareApp() {
            String shareBody = getString(R.string.note_share_body)
                    + getString(R.string.url_app_short_link);
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.brand_name));
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.pref_share_title)));
        }
    }
}
