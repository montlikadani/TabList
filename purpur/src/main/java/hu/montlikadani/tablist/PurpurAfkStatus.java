package hu.montlikadani.tablist;

import org.purpurmc.purpur.event.PlayerAFKEvent;

public final class PurpurAfkStatus extends hu.montlikadani.tablist.listeners.resources.AfkPlayers {

	public PurpurAfkStatus(final hu.montlikadani.tablist.TabList tl) {
		tl.getServer().getPluginManager().registerEvent(PlayerAFKEvent.class, new org.bukkit.event.Listener() {
		}, org.bukkit.event.EventPriority.NORMAL, (listener, e) -> {
			PlayerAFKEvent event = (PlayerAFKEvent) e;

			goAfk(tl, event.getPlayer(), event.isGoingAfk());
		}, tl);
	}
}
