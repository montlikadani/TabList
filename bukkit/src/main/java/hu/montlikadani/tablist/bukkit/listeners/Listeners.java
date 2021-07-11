package hu.montlikadani.tablist.bukkit.listeners;

import java.util.concurrent.CompletableFuture;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.tablist.TabTitle;
import hu.montlikadani.tablist.bukkit.user.TabListPlayer;
import hu.montlikadani.tablist.bukkit.utils.UpdateDownloader;

public final class Listeners implements org.bukkit.event.Listener {

	private final TabList plugin;

	public Listeners(TabList plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();

		CompletableFuture.supplyAsync(() -> {
			plugin.updateAll(player);
			return true;
		});

		if (player.isOp()) {
			UpdateDownloader.checkFromGithub(player);
		}
	}

	@EventHandler
	public void onGamemodeChange(PlayerGameModeChangeEvent e) {
		Player player = e.getPlayer();

		plugin.getUser(player).map(user -> (TabListPlayer) user).filter(user -> user.getHidePlayers() != null)
				.ifPresent(user -> {
					if (e.getNewGameMode() == GameMode.SPECTATOR) {
						user.getHidePlayers().addPlayerToTab(player, player);
					} else {
						user.getHidePlayers().removePlayerFromTab(player, player);
					}
				});
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent eve) {
		TabTitle.sendTabTitle(eve.getPlayer(), "", "");
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