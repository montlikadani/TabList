package hu.montlikadani.tablist.utils.datafetcher;

import hu.montlikadani.tablist.utils.PlayerSkinProperties;

import java.util.concurrent.CompletableFuture;

public final class URLDataFetcher {

    private static final java.util.List<RequestType> DATA_FETCHERS = new java.util.ArrayList<>(2);

    static {
        DATA_FETCHERS.add(new hu.montlikadani.tablist.utils.datafetcher.impl.AshconProfile());
        DATA_FETCHERS.add(new hu.montlikadani.tablist.utils.datafetcher.impl.SessionServerProfile());
    }

    public static CompletableFuture<PlayerSkinProperties> fetchProfile(String playerNameOrId) {
        return CompletableFuture.supplyAsync(() -> fetchData(playerNameOrId));
    }

    private static PlayerSkinProperties fetchData(String playerNameOrId) {
        for (RequestType fetcher : DATA_FETCHERS) {
            try {
                return fetcher.get(playerNameOrId);
            } catch (java.io.IOException ex) {
                // If it fails then fetch using another host
            }
        }

        return null;
    }
}
