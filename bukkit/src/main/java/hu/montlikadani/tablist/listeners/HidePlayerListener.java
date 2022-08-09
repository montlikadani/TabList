package hu.montlikadani.tablist.listeners;

import org.bukkit.GameMode;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public final class HidePlayerListener {

	public HidePlayerListener(final hu.montlikadani.tablist.TabList tl) {
		tl.getServer().getPluginManager().registerEvent(PlayerGameModeChangeEvent.class, new Listener() {
		}, org.bukkit.event.EventPriority.NORMAL, new org.bukkit.plugin.EventExecutor() {

			@Override
			public void execute(Listener listener, org.bukkit.event.Event e) {
				PlayerGameModeChangeEvent event = (PlayerGameModeChangeEvent) e;
				boolean isSpectator = event.getNewGameMode() == GameMode.SPECTATOR;

				// Checks if the new game mode is spectator or the player's old game mode was
				// spectator
				if (isSpectator || event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
					org.bukkit.entity.Player player = event.getPlayer();

					tl.getUser(player.getUniqueId()).filter(user -> user.isRemovedFromPlayerList())
							.map(user -> (hu.montlikadani.tablist.user.TabListPlayer) user).ifPresent(user -> {
								if (isSpectator) {
									user.getHidePlayers().addPlayerToTab(player, player);
								} else {
									user.getHidePlayers().removePlayerFromTab(player, player);
								}
							});
				}
			}
		}, tl);
	}
}
