package hu.montlikadani.tablist.listeners.resources;

import org.bukkit.event.EventHandler;

import com.Zrips.CMI.events.CMIAfkEnterEvent;
import com.Zrips.CMI.events.CMIAfkLeaveEvent;

public final class CMIAfkStatus extends AfkPlayers implements org.bukkit.event.Listener {

	@EventHandler
	public void onAfkChange(CMIAfkEnterEvent e) {
		goAfk(e.getPlayer(), true);
	}

	@EventHandler
	public void onAfkChange(CMIAfkLeaveEvent e) {
		goAfk(e.getPlayer(), false);
	}
}
