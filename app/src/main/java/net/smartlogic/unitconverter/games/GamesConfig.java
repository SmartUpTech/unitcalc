package net.smartlogic.unitconverter.games;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** The host owns transport configuration; the website owns all games and routes. */
public final class GamesConfig {
    public static final String ENTRY_URL = "https://smartuptech.in/games/index.html";
    public static final String TAG = "games";
    public static final String BRIDGE_NAME = "AndroidGames";
    public static final int BRIDGE_VERSION = 1;
    public static final int MAX_MESSAGE_LENGTH = 16_384;
    public static final long LOAD_TIMEOUT_MS = 30_000;
    public static final long DATE_CHECK_INTERVAL_MS = 30_000;
    private static final URI ENTRY = URI.create(ENTRY_URL);

    /** Wake at local midnight, while retaining periodic checks for manual clock changes. */
    public static long nextDateCheckDelayMillis(Instant now, ZoneId zone) {
        Instant midnight = now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant();
        long remaining = Duration.between(now, midnight).toMillis();
        return Math.max(1L, Math.min(DATE_CHECK_INTERVAL_MS, remaining));
    }
    public static final String TRUSTED_ORIGIN = ENTRY.getScheme() + "://" + ENTRY.getHost();

    private GamesConfig() {}

    public static String embeddedUrl() {
        try {
            String query = ENTRY.getQuery();
            return new URI(ENTRY.getScheme(), ENTRY.getAuthority(), ENTRY.getPath(),
                    query == null ? "embedded=1" : query + "&embedded=1", ENTRY.getFragment()).toString();
        } catch (URISyntaxException impossible) {
            throw new IllegalStateException("Invalid Games entry URL", impossible);
        }
    }

    public static boolean isTrustedOrigin(String url) {
        try {
            URI uri = URI.create(url);
            return ENTRY.getScheme().equals(uri.getScheme()) && ENTRY.getHost().equals(uri.getHost())
                    && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443);
        } catch (IllegalArgumentException | NullPointerException invalid) {
            return false;
        }
    }

    public static boolean isTrustedUrl(String url) {
        if (!isTrustedOrigin(url)) return false;
        URI uri = URI.create(url);
        String path = uri.getRawPath();
        return path != null && path.startsWith("/games/") && !path.contains("%")
                && !path.contains("\\") && uri.normalize().getRawPath().equals(path);
    }

    public static boolean isGameId(String value) {
        return value != null && value.matches("[a-z][a-z0-9_]{0,47}");
    }

    public static boolean isDate(String value) {
        try {
            return value != null && value.length() == 10 && LocalDate.parse(value).toString().equals(value);
        } catch (java.time.format.DateTimeParseException invalid) {
            return false;
        }
    }
}
