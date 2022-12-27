package hu.montlikadani.tablist.listeners.resources;

import org.bukkit.event.Listener;

import net.ess3.api.events.AfkStatusChangeEvent;

public final class EssAfkStatus extends AfkPlayers {

	public EssAfkStatus(final hu.montlikadani.tablist.TabList tl) {
		tl.getServer().getPluginManager().registerEvent(AfkStatusChangeEvent.class, new Listener() {
		}, org.bukkit.event.EventPriority.NORMAL, (listener, e) -> {
			AfkStatusChangeEvent event = (AfkStatusChangeEvent) e;

			goAfk(tl, event.getAffected().getBase(), event.getValue());
		}, tl);
	}
}
