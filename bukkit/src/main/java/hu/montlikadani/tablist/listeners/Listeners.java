package hu.montlikadani.tablist.listeners;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.user.TabListPlayer;

public final class Listeners implements org.bukkit.event.Listener {

	private final TabList plugin;

	public Listeners(TabList plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlJoin(PlayerJoinEvent event) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> plugin.updateAll(event.getPlayer()));
	}

	@EventHandler
	public void onGamemodeChange(PlayerGameModeChangeEvent e) {
		boolean isSpectator = e.getNewGameMode() == GameMode.SPECTATOR;

		// Checks if the new game mode is spectator or the player's old game mode was
		// spectator
		if (isSpectator || e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			Player player = e.getPlayer();

			plugin.getUser(player.getUniqueId()).filter(user -> user.isRemovedFromPlayerList())
					.map(user -> (TabListPlayer) user).ifPresent(user -> {
						if (isSpectator) {
							user.getHidePlayers().addPlayerToTab(player, player);
						} else {
							user.getHidePlayers().removePlayerFromTab(player, player);
						}
					});
		}
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent eve) {
		plugin.updateAll(eve.getPlayer());
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent ev) {
		plugin.onPlayerQuit(ev.getPlayer());
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent e) {
		plugin.onPlayerQuit(e.getPlayer());
	}
}