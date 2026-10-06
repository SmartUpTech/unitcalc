package net.smartlogic.unitconverter.games;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.button.MaterialButton;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.activity.MainActivity;
import net.smartlogic.unitconverter.helper.AdMobManager;
import net.smartlogic.unitconverter.theme.CalculatorTheme;
import net.smartlogic.unitconverter.theme.ThemeManager;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;

public final class GamesFragment extends Fragment implements GamesWebView.Listener {
    private static final String SAVED_URL = "games_url";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final GamesBridge bridge = new GamesBridge();
    private final ThemeManager.Listener themeListener = theme -> { applyTheme(); configureIfChanged(); };
    private final Runnable timeout = () -> showError(R.string.games_load_error);
    private final Runnable dateCheck = new Runnable() {
        @Override public void run() {
            if (isPlayingSurfaceVisible()) {
                configureIfChanged();
                main.postDelayed(this, GamesConfig.DATE_CHECK_INTERVAL_MS);
            }
        }
    };
    private GamesStateRepository repository;
    private GamesWebView webView;
    private View root;
    private View status;
    private TextView statusText;
    private ProgressBar progress;
    private MaterialButton retry;
    private OnBackPressedCallback backCallback;
    private int documentGeneration;
    private int configurationGeneration;
    private int navigationGeneration;
    private boolean ready;
    private boolean failed;
    private String configured;
    private String configuredDate;
    private String lastUrl = GamesConfig.embeddedUrl();

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_games, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedState) {
        root = view;
        status = view.findViewById(R.id.games_status);
        statusText = view.findViewById(R.id.games_status_text);
        progress = view.findViewById(R.id.games_progress);
        retry = view.findViewById(R.id.games_retry);
        repository = GamesStateRepository.getInstance(requireContext());
        if (savedState != null && GamesConfig.isTrustedUrl(savedState.getString(SAVED_URL))) {
            lastUrl = savedState.getString(SAVED_URL);
        }
        retry.setOnClickListener(v -> createWebView());
        backCallback = new OnBackPressedCallback(!isHidden()) {
            @Override public void handleOnBackPressed() {
                if (requireActivity() instanceof MainActivity activity && activity.closeNavigationDrawerIfOpen()) {
                    return;
                } else if (webView != null && ready && !failed) {
                    GamesWebView target = webView;
                    target.back(handled -> {
                        if (target == webView && isPlayingSurfaceVisible() && !handled) forwardBack();
                    });
                } else {
                    forwardBack();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
        ThemeManager.addListener(themeListener);
        createWebView();
    }

    private void forwardBack() {
        backCallback.setEnabled(false);
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
        if (root != null) backCallback.setEnabled(!isHidden());
    }

    private void createWebView() {
        if (webView != null) {
            if (GamesConfig.isTrustedUrl(webView.getUrl())) lastUrl = webView.getUrl();
            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.dispose();
        }
        documentGeneration++;
        ready = false;
        failed = false;
        configured = null;
        bridge.reset();
        webView = new GamesWebView(requireContext(), this);
        FrameLayout container = root.findViewById(R.id.games_web_container);
        container.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        applyTheme();
        showLoading();
        webView.loadGames(lastUrl);
        if (!isPlayingSurfaceVisible()) webView.onPause();
    }

    @Override public void onDocumentStarted() {
        documentGeneration++;
        ready = false;
        failed = false;
        configured = null;
        bridge.reset();
        showLoading();
    }

    @Override public void onMessage(String message) {
        GamesBridge.Event event = bridge.accept(message);
        if (event == null || failed) return;
        switch (event.type()) {
            case "onReady":
                if (!ready) { ready = true; configureIfChanged(); }
                break;
            case "onGameStarted":
            case "onGameExited":
                navigationGeneration++;
                break;
            case "onGameCompleted":
                if (!isPlayingSurfaceVisible() || !event.date().equals(LocalDate.now().toString())) {
                    configureIfChanged();
                    return;
                }
                saveCompletion(event);
                break;
            case "onError":
                if ("INVALID_HOST".equals(event.errorCode())) showError(R.string.games_load_error);
                // Other web errors use the shared themed web retry UI.
                break;
            default: break;
        }
    }

    private void configureIfChanged() {
        if (root == null || webView == null || !ready || failed || !isPlayingSurfaceVisible()) return;
        String date = LocalDate.now().toString();
        String zone = ZoneId.systemDefault().getId();
        String identity = GamesHostConfiguration.create(requireContext(), date, zone,
                java.util.Collections.emptyMap()).toString();
        // The web session already owns its immediate completion badges. Resending only because
        // a badge changed would restart the next unfinished game in the shared v1 platform.
        if (identity.equals(configured)) return;
        showLoading();
        int page = documentGeneration;
        int request = ++configurationGeneration;
        GamesWebView target = webView;
        repository.execute(() -> {
            Map<String, String> completions = repository.completionsFor(date);
            main.post(() -> {
                if (root == null || target != webView || page != documentGeneration
                        || request != configurationGeneration || !isPlayingSurfaceVisible() || failed) return;
                JSONObject config = GamesHostConfiguration.create(requireContext(), date, zone, completions);
                if (!date.equals(LocalDate.now().toString()) || !zone.equals(ZoneId.systemDefault().getId())) {
                    configureIfChanged();
                    return;
                }
                bridge.configure(date);
                configuredDate = date;
                target.configure(config, accepted -> {
                    if (root == null || target != webView || page != documentGeneration || request != configurationGeneration) return;
                    if (!accepted) { showError(R.string.games_load_error); return; }
                    configured = identity;
                    main.removeCallbacks(timeout);
                    target.setVisibility(View.VISIBLE);
                    status.setVisibility(View.GONE);
                });
            });
        });
    }

    private void saveCompletion(GamesBridge.Event event) {
        int page = documentGeneration;
        int navigation = navigationGeneration;
        GamesWebView target = webView;
        String completionUrl = target.getUrl();
        repository.execute(() -> {
            GamesStateRepository.WriteResult result = repository.recordCompletion(event.gameId(), event.date());
            main.post(() -> {
                if (result == GamesStateRepository.WriteResult.NEW) {
                    // Count the genuine finish even if the user navigated away while disk writing.
                    // An unavailable/stale break is skipped, never shown later.
                    AdMobManager ads = AdMobManager.getInstance(applicationContext);
                    ads.onGamesCompletion(getActivity(), event.gameId(), event.date(),
                            () -> root != null && target == webView && page == documentGeneration
                                    && isPlayingSurfaceVisible() && !failed
                                    && navigation == navigationGeneration
                                    && event.date().equals(configuredDate)
                                    && event.date().equals(LocalDate.now().toString())
                                    && Objects.equals(completionUrl, target.getUrl()));
                } else if (result == GamesStateRepository.WriteResult.FAILED
                        && root != null && target == webView && page == documentGeneration) {
                    showError(R.string.games_save_error);
                }
            });
        });
    }

    private android.content.Context applicationContext;

    @Override public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        applicationContext = context.getApplicationContext();
    }

    private boolean isPlayingSurfaceVisible() {
        return root != null && isAdded() && !isHidden()
                && getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
    }

    private void applyTheme() {
        if (root == null) return;
        CalculatorTheme theme = ThemeManager.get();
        root.setBackgroundColor(theme.background());
        status.setBackgroundColor(theme.background());
        statusText.setTextColor(theme.mainText());
        progress.setIndeterminateTintList(ColorStateList.valueOf(theme.equal()));
        retry.setBackgroundTintList(ColorStateList.valueOf(theme.background()));
        retry.setTextColor(theme.equal());
        if (webView != null) webView.setBackgroundColor(theme.background());
    }

    private void showLoading() {
        if (root == null) return;
        status.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
        retry.setVisibility(View.GONE);
        statusText.setText(R.string.games_loading);
        if (webView != null) webView.setVisibility(View.INVISIBLE);
        main.removeCallbacks(timeout);
        if (isPlayingSurfaceVisible()) main.postDelayed(timeout, GamesConfig.LOAD_TIMEOUT_MS);
    }

    private void showError(int messageRes) {
        if (root == null) return;
        failed = true;
        main.removeCallbacks(timeout);
        if (webView != null) { webView.stopLoading(); webView.setVisibility(View.INVISIBLE); }
        status.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
        retry.setVisibility(View.VISIBLE);
        statusText.setText(messageRes);
    }

    @Override public void onFailure(int messageRes) { showError(messageRes); }

    @Override public void onRouteChanged() { navigationGeneration++; }

    @Override public void onRendererGone() {
        if (webView != null) {
            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.dispose();
            webView = null;
        }
        documentGeneration++;
        showError(R.string.games_load_error);
    }

    @Override public void onResume() {
        super.onResume();
        updateVisibility();
    }

    @Override public void onPause() {
        main.removeCallbacks(dateCheck);
        main.removeCallbacks(timeout);
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        updateVisibility();
    }

    private void updateVisibility() {
        if (root == null) return;
        backCallback.setEnabled(!isHidden());
        main.removeCallbacks(dateCheck);
        main.removeCallbacks(timeout);
        if (isPlayingSurfaceVisible()) {
            if (configured == null && !failed) main.postDelayed(timeout, GamesConfig.LOAD_TIMEOUT_MS);
            if (webView != null) webView.onResume();
            configureIfChanged();
            main.postDelayed(dateCheck, GamesConfig.DATE_CHECK_INTERVAL_MS);
            AdMobManager.getInstance(requireContext()).prepareGamesAds(requireActivity(), this::isPlayingSurfaceVisible,
                    () -> { if (isAdded()) requireActivity().invalidateOptionsMenu(); });
        } else if (webView != null) webView.onPause();
    }

    public boolean isPrivacyOptionsRequired() {
        return AdMobManager.getInstance(requireContext()).isGamesPrivacyOptionsRequired();
    }

    public void showPrivacyOptions() {
        AdMobManager.getInstance(requireContext()).showGamesPrivacyOptions(requireActivity());
    }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        if (webView != null && GamesConfig.isTrustedUrl(webView.getUrl())) lastUrl = webView.getUrl();
        state.putString(SAVED_URL, lastUrl);
    }

    @Override public void onDestroyView() {
        documentGeneration++;
        main.removeCallbacks(timeout);
        main.removeCallbacks(dateCheck);
        ThemeManager.removeListener(themeListener);
        if (webView != null) {
            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.dispose();
            webView = null;
        }
        root = null;
        status = null;
        statusText = null;
        progress = null;
        retry = null;
        super.onDestroyView();
    }
}
