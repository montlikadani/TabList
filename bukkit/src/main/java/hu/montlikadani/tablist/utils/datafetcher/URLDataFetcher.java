package hu.montlikadani.tablist.utils.datafetcher;

import hu.montlikadani.tablist.utils.PlayerSkinProperties;

import java.util.concurrent.CompletableFuture;

public final class URLDataFetcher {

    private static final RequestType[] DATA_FETCHERS = new RequestType[3];

    static {
        DATA_FETCHERS[0] = new hu.montlikadani.tablist.utils.datafetcher.impl.AshconProfile();
        DATA_FETCHERS[1] = new hu.montlikadani.tablist.utils.datafetcher.impl.MineToolsProfile();
        DATA_FETCHERS[2] = new hu.montlikadani.tablist.utils.datafetcher.impl.SessionServerProfile();
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
