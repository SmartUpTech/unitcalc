package net.smartlogic.unitconverter.games;

import java.util.HashSet;
import java.util.Set;

/** Process-session memory only. Thresholds are supplied by the existing Ad Manager. */
public final class GamesSession {
    private final int firstThreshold;
    private final int secondThreshold;
    private final int maxAds;
    private final Set<String> completions = new HashSet<>();
    private int adsReserved;

    public GamesSession(int firstThreshold, int secondThreshold, int maxAds) {
        this.firstThreshold = firstThreshold;
        this.secondThreshold = secondThreshold;
        this.maxAds = maxAds;
    }

    /** Called only after a new completion is durably recorded. Never queues a late ad. */
    public synchronized boolean recordCompletion(String gameId, String date) {
        if (!GamesConfig.isGameId(gameId) || !GamesConfig.isDate(date)
                || !completions.add(date + ":" + gameId)) return false;
        int count = completions.size();
        return adsReserved < maxAds && (count == firstThreshold || count == secondThreshold);
    }

    public synchronized boolean reserveAd() {
        if (adsReserved >= maxAds) return false;
        adsReserved++;
        return true;
    }

    public synchronized int completionCount() { return completions.size(); }
    public synchronized int adCount() { return adsReserved; }
}
