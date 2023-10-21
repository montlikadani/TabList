package hu.montlikadani.tablist.utils;

import hu.montlikadani.tablist.utils.datafetcher.URLDataFetcher;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerSkinProperties {

    private static final java.util.Set<PlayerSkinProperties> CACHED = new java.util.HashSet<>();
    public static final int MAX_REQUESTS = 100;

    public final UUID playerId;
    public String playerName, textureRawValue, decodedTextureValue;

    public static PlayerSkinProperties findPlayerProperty(String playerName, UUID playerId) {
        for (PlayerSkinProperties one : CACHED) {
            if ((playerId != null && playerId.equals(one.playerId)) || (playerName != null && playerName.equals(one.playerName))) {
                return one;
            }
        }

        return null;
    }

    public static int cachedSize() {
        return CACHED.size();
    }

    public PlayerSkinProperties(String playerName, UUID playerId) {
        this(playerName, playerId, null, null);
    }

    public PlayerSkinProperties(String playerName, UUID playerId, String textureRawValue, String decodedTextureValue) {
        this.playerName = playerName;
        this.playerId = playerId;

        if (textureRawValue != null && decodedTextureValue != null) {
            this.textureRawValue = textureRawValue;
            this.decodedTextureValue = decodedTextureValue;

            CACHED.add(this);
        }
    }

    public CompletableFuture<Void> retrieveTextureData() {
        if (textureRawValue != null && decodedTextureValue != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.complete(null);

            return future;
        }

        if (playerName != null) {
            return URLDataFetcher.fetchProfile(playerName).thenAccept(properties -> {
                this.textureRawValue = properties.textureRawValue;
                this.decodedTextureValue = properties.decodedTextureValue;
            });
        }

        if (playerId == null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.complete(null);

            return future;
        }

        return URLDataFetcher.fetchProfile(playerId.toString()).thenAccept(properties -> {
            this.textureRawValue = properties.textureRawValue;
            this.decodedTextureValue = properties.decodedTextureValue;
        });
    }
}
