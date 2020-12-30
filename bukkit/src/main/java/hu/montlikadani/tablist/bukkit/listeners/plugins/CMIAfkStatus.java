package hu.montlikadani.tablist.bukkit.listeners.plugins;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.Zrips.CMI.events.CMIAfkEnterEvent;
import com.Zrips.CMI.events.CMIAfkLeaveEvent;

public class CMIAfkStatus extends AfkPlayers implements Listener {

	@EventHandler
	public void onAfkChange(CMIAfkEnterEvent e) {
		goAfk(e.getPlayer(), true);
	}

	@EventHandler
	public void onAfkChange(CMIAfkLeaveEvent e) {
		goAfk(e.getPlayer(), false);
	}
}
