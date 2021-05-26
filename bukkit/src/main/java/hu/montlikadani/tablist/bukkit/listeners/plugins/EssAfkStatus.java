package hu.montlikadani.tablist.bukkit.listeners.plugins;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import net.ess3.api.events.AfkStatusChangeEvent;

public final class EssAfkStatus extends AfkPlayers implements Listener {

	@EventHandler
	public void onAfkChange(AfkStatusChangeEvent event) {
		goAfk(event.getAffected().getBase(), event.getValue());
	}
}