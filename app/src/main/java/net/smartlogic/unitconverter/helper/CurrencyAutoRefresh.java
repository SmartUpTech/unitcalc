package net.smartlogic.unitconverter.helper;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Foreground-only rate refresh, once per validated connection while rates are stale. */
public final class CurrencyAutoRefresh {
    private final ConnectivityManager connectivity;
    private final Handler handler;
    private final Supplier<CurrencyRateStore.Snapshot> rates;
    private final Runnable refresh;
    private final LongSupplier clock;
    private ConnectivityManager.NetworkCallback callback;
    private Network attemptedNetwork;
    private final Runnable expiry = () -> {
        attemptedNetwork = null;
        ratesChanged();
    };

    public CurrencyAutoRefresh(ConnectivityManager connectivity, Handler handler,
                              Supplier<CurrencyRateStore.Snapshot> rates, Runnable refresh) {
        this(connectivity, handler, rates, refresh, System::currentTimeMillis);
    }

    CurrencyAutoRefresh(ConnectivityManager connectivity, Handler handler,
                        Supplier<CurrencyRateStore.Snapshot> rates, Runnable refresh, LongSupplier clock) {
        this.connectivity = connectivity;
        this.handler = handler;
        this.rates = rates;
        this.refresh = refresh;
        this.clock = clock;
    }

    public void setActive(boolean active) {
        if (!active) {
            handler.removeCallbacks(expiry);
            ConnectivityManager.NetworkCallback previous = callback;
            callback = null;
            attemptedNetwork = null;
            if (previous != null) connectivity.unregisterNetworkCallback(previous);
            return;
        }
        if (callback == null) {
            callback = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network network) {
                    postConnection(this, network, true);
                }

                @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                    postConnection(this, network, hasInternet(capabilities));
                }

                @Override public void onLost(Network network) {
                    postConnection(this, network, false);
                }
            };
            connectivity.registerDefaultNetworkCallback(callback);
        }
        ratesChanged();
    }

    private void postConnection(ConnectivityManager.NetworkCallback source, Network network, boolean connected) {
        handler.post(() -> {
            // Callbacks queued before leaving/reopening must not affect the new subscription.
            if (callback != source) return;
            if (!connected) {
                if (network.equals(attemptedNetwork)) attemptedNetwork = null;
                return;
            }
            ratesChanged();
        });
    }

    public void ratesChanged() {
        handler.removeCallbacks(expiry);
        if (callback == null) return;
        CurrencyRateStore.Snapshot snapshot = rates.get();
        long now = clock.getAsLong();
        if (snapshot != null && "Frankfurter".equals(snapshot.provider) && snapshot.isFresh(now)) {
            long remaining = CurrencyRateStore.REFRESH_INTERVAL - (now - snapshot.fetchedAt);
            handler.postDelayed(expiry, remaining);
            return;
        }
        Network network = connectivity.getActiveNetwork();
        if (network == null || !hasInternet(connectivity.getNetworkCapabilities(network))
                || network.equals(attemptedNetwork)) return;
        attemptedNetwork = network;
        refresh.run();
    }

    private static boolean hasInternet(NetworkCapabilities capabilities) {
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
