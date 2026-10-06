package net.smartlogic.unitconverter.games;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import net.smartlogic.unitconverter.R;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.function.Consumer;

/** Origin-scoped message bridge, additionally restricted to the main Games document. */
public final class GamesWebView extends WebView {
    public interface Listener {
        void onDocumentStarted();
        void onRouteChanged();
        void onMessage(String message);
        void onFailure(int messageRes);
        void onRendererGone();
    }
    private final Listener listener;
    private boolean bridgeInstalled;
    private volatile boolean disposed;

    @SuppressLint("SetJavaScriptEnabled")
    public GamesWebView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false); // Embedded completion state is exclusively native.
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setVisibility(INVISIBLE); // Reveal only after native palette/configuration reaches a frame.
        setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, Bitmap icon) {
                if (disposed) return;
                listener.onDocumentStarted();
                if (!GamesConfig.isTrustedUrl(url)) {
                    stopLoading();
                    listener.onFailure(R.string.games_load_error);
                }
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // No browser, intent:, file:, cross-origin links or secondary frames in this feature.
                return !request.isForMainFrame() || !GamesConfig.isTrustedUrl(request.getUrl().toString());
            }

            @Override public void doUpdateVisitedHistory(WebView view, String url, boolean reload) {
                if (!disposed) listener.onRouteChanged();
            }

            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (GamesConfig.isTrustedUrl(request.getUrl().toString())) return null;
                return new WebResourceResponse("text/plain", "UTF-8", 403, "Blocked",
                        Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (!disposed && request.isForMainFrame()) listener.onFailure(R.string.games_load_error);
            }

            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (!disposed && request.isForMainFrame()) listener.onFailure(R.string.games_load_error);
            }

            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                if (!disposed) listener.onFailure(R.string.games_load_error);
            }

            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (!disposed) listener.onRendererGone();
                return true;
            }
        });
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(this, GamesConfig.BRIDGE_NAME,
                    Collections.singleton(GamesConfig.TRUSTED_ORIGIN), (view, message, origin, mainFrame, reply) -> {
                        if (!disposed && mainFrame && GamesConfig.isTrustedOrigin(origin.toString())
                                && GamesConfig.isTrustedUrl(view.getUrl())
                                && message.getType() == androidx.webkit.WebMessageCompat.TYPE_STRING) {
                            listener.onMessage(message.getData());
                        }
                    });
            bridgeInstalled = true;
        }
    }

    public void loadGames(String url) {
        if (!bridgeInstalled) {
            listener.onFailure(R.string.games_update_webview);
            return;
        }
        loadUrl(GamesConfig.isTrustedUrl(url) ? url : GamesConfig.embeddedUrl());
    }

    public void configure(JSONObject config, Consumer<Boolean> callback) {
        if (!GamesConfig.isTrustedUrl(getUrl())) { callback.accept(false); return; }
        evaluateJavascript("Boolean(window.SmartUpGames && window.SmartUpGames.configure(" + config + "))", result -> {
            if (!"true".equals(result)) { callback.accept(false); return; }
            postVisualStateCallback(0, new VisualStateCallback() {
                @Override public void onComplete(long requestId) { callback.accept(true); }
            });
        });
    }

    public void back(Consumer<Boolean> callback) {
        if (!GamesConfig.isTrustedUrl(getUrl())) { callback.accept(false); return; }
        evaluateJavascript("(function(){if(location.hash && window.SmartUpGames){window.SmartUpGames.back();return true;}return false;})()",
                result -> callback.accept("true".equals(result)));
    }

    public void dispose() {
        disposed = true;
        stopLoading();
        if (bridgeInstalled) {
            WebViewCompat.removeWebMessageListener(this, GamesConfig.BRIDGE_NAME);
            bridgeInstalled = false;
        }
        setWebViewClient(new WebViewClient());
        removeAllViews();
        destroy();
    }
}
