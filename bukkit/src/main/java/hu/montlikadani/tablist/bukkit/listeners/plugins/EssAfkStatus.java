package hu.montlikadani.tablist.bukkit.listeners.plugins;

import net.ess3.api.events.AfkStatusChangeEvent;

public final class EssAfkStatus extends AfkPlayers implements org.bukkit.event.Listener {

	@org.bukkit.event.EventHandler
	public void onAfkChange(AfkStatusChangeEvent event) {
		goAfk(event.getAffected().getBase(), event.getValue());
	}
}