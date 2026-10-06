package net.smartlogic.unitconverter.games;

import org.json.JSONException;
import org.json.JSONObject;

/** Validates the shared v1 envelope and rejects unstarted, stale and duplicate finishes. */
public final class GamesBridge {
    public record Event(String type, String gameId, String date, JSONObject result, String errorCode) {}
    private String date;
    private String activeId;
    private boolean completionReceived;

    public void configure(String localDate) { date = localDate; activeId = null; completionReceived = false; }
    public void reset() { date = null; activeId = null; completionReceived = false; }

    public Event accept(String message) {
        if (message == null || message.length() > GamesConfig.MAX_MESSAGE_LENGTH) return null;
        try {
            JSONObject value = new JSONObject(message);
            if (!(value.opt("bridgeVersion") instanceof Integer version)
                    || version != GamesConfig.BRIDGE_VERSION || !(value.opt("type") instanceof String type)) return null;
            if ("onReady".equals(type)) return new Event(type, null, null, null, null);
            if ("onError".equals(type)) {
                if (!(value.opt("errorCode") instanceof String code) || code.length() > 100) return null;
                Object id = value.opt("gameId");
                if (id != null && (!(id instanceof String) || !GamesConfig.isGameId((String) id))) return null;
                return new Event(type, (String) id, null, null, code);
            }
            if (!(value.opt("gameId") instanceof String id) || !GamesConfig.isGameId(id)
                    || !(value.opt("date") instanceof String eventDate)
                    || !GamesConfig.isDate(eventDate) || !eventDate.equals(date)) return null;
            switch (type) {
                case "onGameStarted":
                    if (!id.equals(activeId)) completionReceived = false;
                    activeId = id;
                    break;
                case "onGameCompleted":
                    if (!id.equals(activeId) || completionReceived || !(value.opt("result") instanceof JSONObject)) return null;
                    completionReceived = true;
                    break;
                case "onGameExited":
                    if (!id.equals(activeId)) return null;
                    activeId = null;
                    break;
                default: return null;
            }
            return new Event(type, id, eventDate, value.optJSONObject("result"), null);
        } catch (JSONException invalid) {
            return null;
        }
    }
}
