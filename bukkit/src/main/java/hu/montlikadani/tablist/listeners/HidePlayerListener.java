package hu.montlikadani.tablist.listeners;

import org.bukkit.GameMode;

import hu.montlikadani.tablist.TabList;

public final class HidePlayerListener implements org.bukkit.event.Listener {

	private final TabList plugin;

	public HidePlayerListener(TabList plugin) {
		this.plugin = plugin;
	}

	@org.bukkit.event.EventHandler
	public void onGamemodeChange(org.bukkit.event.player.PlayerGameModeChangeEvent e) {
		boolean isSpectator = e.getNewGameMode() == GameMode.SPECTATOR;

		// Checks if the new game mode is spectator or the player's old game mode was
		// spectator
		if (isSpectator || e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			org.bukkit.entity.Player player = e.getPlayer();

			plugin.getUser(player.getUniqueId()).filter(user -> user.isRemovedFromPlayerList())
					.map(user -> (hu.montlikadani.tablist.user.TabListPlayer) user).ifPresent(user -> {
						if (isSpectator) {
							user.getHidePlayers().addPlayerToTab(player, player);
						} else {
							user.getHidePlayers().removePlayerFromTab(player, player);
						}
					});
		}
	}
}
