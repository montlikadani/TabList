package hu.montlikadani.tablist.bukkit.listeners;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import hu.montlikadani.tablist.bukkit.ConfigValues;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.utils.UpdateDownloader;

public class Listeners implements Listener {

	private TabList plugin;

	public Listeners(TabList plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();

		plugin.updateAll(p);
		plugin.getGroups().loadGroupForPlayer(p);

		if (ConfigValues.isFakePlayers()) {
			plugin.getConf().createFakePlayersFile();
			plugin.getFakePlayerHandler().load();
		}

		if (p.isOp()) {
			UpdateDownloader.checkFromGithub(p);
		}
	}

	@EventHandler
	public void onGamemodeChange(PlayerGameModeChangeEvent e) {
		Player p = e.getPlayer();

		if (plugin.getHidePlayers().containsKey(p)) {
			if (e.getNewGameMode() == GameMode.SPECTATOR) {
				plugin.getHidePlayers().get(p).addPlayerToTab(p);
			} else {
				plugin.getHidePlayers().get(p).removePlayerFromTab(p, p);
			}
		}
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent eve) {
		Player pla = eve.getPlayer();

		if (plugin.getTabManager().isPlayerInTab(pla)) {
			plugin.getTabManager().removePlayer(pla);
		}

		plugin.updateAll(pla);
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