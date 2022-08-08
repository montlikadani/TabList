package hu.montlikadani.tablist.listeners.resources;

public final class EssAfkStatus extends AfkPlayers {

	public EssAfkStatus(hu.montlikadani.tablist.TabList tl) {
		tl.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {

			@org.bukkit.event.EventHandler
			public void onAfkChange(net.ess3.api.events.AfkStatusChangeEvent event) {
				goAfk(event.getAffected().getBase(), event.getValue());
			}
		}, tl);
	}
}
