package net.smartlogic.unitconverter.helper;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowNetworkInfo;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class CurrencyAutoRefreshTest {
    private ConnectivityManager connectivity;
    private ShadowConnectivityManager networks;
    private final AtomicLong now = new AtomicLong(1_000_000_000L);
    private final AtomicInteger attempts = new AtomicInteger();
    private final AtomicReference<CurrencyRateStore.Snapshot> rates = new AtomicReference<>();
    private CurrencyAutoRefresh monitor;

    @Before public void setUp() throws Exception {
        connectivity = (ConnectivityManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        networks = shadowOf(connectivity);
        networks.setActiveNetworkInfo(null);
        rates.set(snapshot(now.get() - CurrencyRateStore.REFRESH_INTERVAL));
        monitor = new CurrencyAutoRefresh(connectivity, new Handler(Looper.getMainLooper()),
                rates::get, attempts::incrementAndGet, now::get);
    }

    private CurrencyRateStore.Snapshot snapshot(long fetchedAt) throws Exception {
        return CurrencyRateStore.parseResponse(
                "[{\"date\":\"2026-10-20\",\"base\":\"USD\",\"quote\":\"INR\",\"rate\":80}]", fetchedAt);
    }

    private Network connect(boolean validated) {
        networks.setActiveNetworkInfo(ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.CONNECTED,
                ConnectivityManager.TYPE_WIFI, 0, true, true));
        Network network = connectivity.getActiveNetwork();
        assertNotNull(network);
        networks.setNetworkCapabilities(network, capabilities(validated));
        return network;
    }

    private NetworkCapabilities capabilities(boolean validated) {
        NetworkCapabilities capabilities = new NetworkCapabilities()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        if (validated) capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        return capabilities;
    }

    private ConnectivityManager.NetworkCallback callback() {
        assertEquals(1, networks.getNetworkCallbacks().size());
        return networks.getNetworkCallbacks().iterator().next();
    }

    @Test public void staleRatesWaitForValidatedInternetAndRetryAfterReconnection() {
        monitor.setActive(true);
        assertEquals(0, attempts.get());
        Network network = connect(false);
        ConnectivityManager.NetworkCallback callback = callback();
        callback.onCapabilitiesChanged(network, capabilities(false));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, attempts.get());
        networks.setNetworkCapabilities(network, capabilities(true));
        callback.onCapabilitiesChanged(network, capabilities(true));
        callback.onAvailable(network);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, attempts.get());
        callback.onCapabilitiesChanged(network, capabilities(true));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("Repeated capability events must not retry a failed request", 1, attempts.get());
        networks.setNetworkCapabilities(network, capabilities(false));
        callback.onCapabilitiesChanged(network, capabilities(false));
        shadowOf(Looper.getMainLooper()).idle();
        networks.setNetworkCapabilities(network, capabilities(true));
        callback.onCapabilitiesChanged(network, capabilities(true));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(2, attempts.get());
        monitor.setActive(false);
    }

    @Test public void freshRatesRefreshAtExpiryOnlyWhileOpen() throws Exception {
        connect(true);
        rates.set(snapshot(now.get()));
        monitor.setActive(true);
        assertEquals(0, attempts.get());
        now.addAndGet(CurrencyRateStore.REFRESH_INTERVAL);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(CurrencyRateStore.REFRESH_INTERVAL));
        assertEquals(1, attempts.get());
        rates.set(snapshot(now.get()));
        monitor.ratesChanged();
        monitor.setActive(false);
        now.addAndGet(CurrencyRateStore.REFRESH_INTERVAL);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(CurrencyRateStore.REFRESH_INTERVAL));
        assertEquals(1, attempts.get());
        assertTrue(networks.getNetworkCallbacks().isEmpty());
    }

    @Test public void reopeningRetriesStaleRatesAndIgnoresOldQueuedCallbacks() {
        Network network = connect(true);
        monitor.setActive(true);
        assertEquals(1, attempts.get());
        ConnectivityManager.NetworkCallback old = callback();
        old.onAvailable(network);
        monitor.setActive(false);
        assertTrue(networks.getNetworkCallbacks().isEmpty());
        monitor.setActive(true);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(2, attempts.get());
        monitor.setActive(false);
        old.onAvailable(network);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(2, attempts.get());
    }
}
