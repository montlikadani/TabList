package hu.montlikadani.tablist.listeners.resources;

import org.bukkit.event.Listener;

import net.ess3.api.events.AfkStatusChangeEvent;

public final class EssAfkStatus extends AfkPlayers {

	public EssAfkStatus(hu.montlikadani.tablist.TabList tl) {
		tl.getServer().getPluginManager().registerEvent(AfkStatusChangeEvent.class, new Listener() {
		}, org.bukkit.event.EventPriority.NORMAL, new org.bukkit.plugin.EventExecutor() {

			@Override
			public void execute(Listener listener, org.bukkit.event.Event e) {
				AfkStatusChangeEvent event = (AfkStatusChangeEvent) e;

				goAfk(event.getAffected().getBase(), event.getValue());
			}
		}, tl);
	}
}
