package hu.montlikadani.tablist.utils;

import hu.montlikadani.tablist.utils.datafetcher.URLDataFetcher;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerSkinProperties {

    public static final java.util.Set<PlayerSkinProperties> CACHED = new java.util.HashSet<>();
    public static final int MAX_REQUESTS = 100;

    public final UUID playerId;
    public String playerName, textureRawValue, decodedTextureValue;

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
