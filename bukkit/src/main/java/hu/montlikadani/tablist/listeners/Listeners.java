package hu.montlikadani.tablist.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import hu.montlikadani.tablist.TabList;

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