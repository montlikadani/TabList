package hu.montlikadani.tablist.Sponge;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

public class EventListeners {

	@Listener
	public void onJoin(ClientConnectionEvent.Join event) {
		Player p = event.getTargetEntity();

		TabList.get().getTManager().cancelTab(p);
		TabList.get().getTManager().loadTab(p);
	}

	@Listener
	public void onQuit(ClientConnectionEvent.Disconnect e) {
		TabList.get().getTManager().cancelTab(e.getTargetEntity());
	}
}
