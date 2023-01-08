package hu.montlikadani.tablist.listeners;

import hu.montlikadani.tablist.user.TabListUser;
import org.bukkit.GameMode;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import hu.montlikadani.tablist.packets.PacketNM;

public final class HidePlayerListener {

	public HidePlayerListener(final hu.montlikadani.tablist.TabList tl) {
		tl.getServer().getPluginManager().registerEvent(PlayerGameModeChangeEvent.class, new Listener() {
		}, org.bukkit.event.EventPriority.NORMAL, (listener, e) -> {
			PlayerGameModeChangeEvent event = (PlayerGameModeChangeEvent) e;
			org.bukkit.entity.Player player = event.getPlayer();

			// Send hand animation packet when player changed game mode to fix the following issues:
			// - Player can not pass through blocks when in spectator
			// - Other players can not see this player who came back from spectator
			if (event.getNewGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.SPECTATOR) {
				tl.getUser(player.getUniqueId()).filter(TabListUser::isRemovedFromPlayerList).ifPresent(user -> PacketNM.NMS_PACKET.appendPlayerWithoutListed(player));
			}
		}, tl);
	}
}
