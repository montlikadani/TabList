package hu.montlikadani.tablist.bukkit.listeners;

import java.util.concurrent.CompletableFuture;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.user.TabListPlayer;
import hu.montlikadani.tablist.bukkit.utils.UpdateDownloader;

public class Listeners implements Listener {

	private TabList plugin;

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
		Player p = e.getPlayer();

		plugin.getUser(p).filter(user -> ((TabListPlayer) user).getHidePlayers() != null).ifPresent(user -> {
			if (e.getNewGameMode() == GameMode.SPECTATOR) {
				((TabListPlayer) user).getHidePlayers().addPlayerToTab(p, p);
			} else {
				((TabListPlayer) user).getHidePlayers().removePlayerFromTab(p, p);
			}
		});
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent eve) {
		plugin.getUser(eve.getPlayer()).ifPresent(plugin.getTabManager()::removePlayer);
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