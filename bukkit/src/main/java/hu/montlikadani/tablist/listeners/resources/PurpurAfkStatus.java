package hu.montlikadani.tablist.listeners.resources;

public final class PurpurAfkStatus extends AfkPlayers {

	public PurpurAfkStatus(hu.montlikadani.tablist.TabList tl) {
		tl.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {

			@org.bukkit.event.EventHandler
			public void onAfkChange(org.purpurmc.purpur.event.PlayerAFKEvent event) {
				goAfk(event.getPlayer(), event.isGoingAfk());
			}
		}, tl);
	}
}
