package hu.montlikadani.tablist.utils.datafetcher.impl;

import com.google.gson.JsonElement;
import hu.montlikadani.tablist.utils.PlayerSkinProperties;
import hu.montlikadani.tablist.utils.datafetcher.UrlDataReader;

public final class AshconProfile implements hu.montlikadani.tablist.utils.datafetcher.RequestType {

    @Override
    public PlayerSkinProperties get(String playerIdOrName) {
        com.google.gson.JsonObject json = UrlDataReader.readJsonObject("https://api.ashcon.app/mojang/v2/user/" + playerIdOrName);

        if (json == null) {
            return null;
        }

        JsonElement playerIdElement = json.get("uuid");

        if (playerIdElement == null) {
            return null;
        }

        JsonElement texturesElement = json.get("textures");

        if (texturesElement == null) {
            return null;
        }

        JsonElement rawTextureObject = texturesElement.getAsJsonObject().get("raw");

        if (rawTextureObject == null) {
            return null;
        }

        String value = rawTextureObject.getAsJsonObject().get("value").getAsString();
        json = UrlDataReader.decodeSkinValue(value);

        java.util.UUID id = hu.montlikadani.tablist.utils.Util.tryParseId(playerIdElement.getAsString()).orElse(null);

        return new PlayerSkinProperties(id == null ? playerIdOrName : null, id, value,
                json.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").getAsString());
    }
}
