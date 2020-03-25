package hu.montlikadani.tablist.bukkit.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import hu.montlikadani.tablist.bukkit.PlayerList;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.utils.UpdateDownloader;
import hu.montlikadani.tablist.bukkit.utils.Util;

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

		if (plugin.getC().getBoolean("enable-fake-players")) {
			plugin.getConf().createFakePlayersFile();
			plugin.loadFakePlayers();
		}

		if (plugin.getC().getBoolean("per-world-player-list")) {
			PlayerList.hideShow(p);
		}

		if (p.isOp()) {
			Util.sendMsg(p, UpdateDownloader.checkFromGithub("player"));
		}
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent eve) {
		Player pla = eve.getPlayer();

		plugin.getTabHandler().unregisterTab(pla);
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