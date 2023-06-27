package hu.montlikadani.tablist.utils.datafetcher.impl;

import hu.montlikadani.tablist.utils.PlayerSkinProperties;
import hu.montlikadani.tablist.utils.datafetcher.UrlDataReader;

public final class MineToolsProfile implements hu.montlikadani.tablist.utils.datafetcher.RequestType {

    @Override
    public PlayerSkinProperties get(String playerId) {
        com.google.gson.JsonObject json = UrlDataReader.readJsonObject("https://api.minetools.eu/profile/" + playerId);

        if (json == null) {
            return null;
        }

        com.google.gson.JsonElement rawElement = json.get("raw");

        if (rawElement == null) {
            return null;
        }

        com.google.gson.JsonArray jsonArray = rawElement.getAsJsonObject().get("properties").getAsJsonArray();

        if (jsonArray.isEmpty()) {
            return null;
        }

        String userName = json.get("decoded").getAsJsonObject().get("profileName").getAsString();
        String value = jsonArray.get(0).getAsJsonObject().get("value").getAsString();

        json = UrlDataReader.decodeSkinValue(value);

        return new PlayerSkinProperties(userName,
                hu.montlikadani.tablist.utils.Util.tryParseId(playerId).orElse(null), value,
                json.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").getAsString());
    }
}
