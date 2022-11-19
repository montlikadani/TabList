package hu.montlikadani.tablist.listeners.resources;

import org.bukkit.event.EventHandler;

import com.Zrips.CMI.events.CMIAfkEnterEvent;
import com.Zrips.CMI.events.CMIAfkLeaveEvent;

import hu.montlikadani.tablist.TabList;

public final class CMIAfkStatus extends AfkPlayers implements org.bukkit.event.Listener {

	private final TabList tl;

	public CMIAfkStatus(TabList tl) {
		this.tl = tl;
	}

	@EventHandler
	public void onAfkChange(CMIAfkEnterEvent e) {
		goAfk(tl, e.getPlayer(), true);
	}

	@EventHandler
	public void onAfkChange(CMIAfkLeaveEvent e) {
		goAfk(tl, e.getPlayer(), false);
	}
}
