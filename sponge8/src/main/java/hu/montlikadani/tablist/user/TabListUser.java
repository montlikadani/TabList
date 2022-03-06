package hu.montlikadani.tablist.user;

import hu.montlikadani.tablist.tablist.TabListManager;
import hu.montlikadani.tablist.tablist.groups.TabGroupPlayer;

public interface TabListUser {

	java.util.UUID getUniqueId();

	net.kyori.adventure.text.Component getName();

	java.util.Optional<org.spongepowered.api.entity.living.player.server.ServerPlayer> getPlayer();

	TabGroupPlayer getTabPlayer();

	TabListManager getTabListManager();

}
