package hu.montlikadani.tablist.listeners.resources;

import org.bukkit.event.Listener;
import org.purpurmc.purpur.event.PlayerAFKEvent;

public final class PurpurAfkStatus extends AfkPlayers {

	public PurpurAfkStatus(hu.montlikadani.tablist.TabList tl) {
		tl.getServer().getPluginManager().registerEvent(PlayerAFKEvent.class, new Listener() {
		}, org.bukkit.event.EventPriority.NORMAL, new org.bukkit.plugin.EventExecutor() {

			@Override
			public void execute(Listener listener, org.bukkit.event.Event e) {
				PlayerAFKEvent event = (PlayerAFKEvent) e;

				goAfk(event.getPlayer(), event.isGoingAfk());
			}
		}, tl);
	}
}
