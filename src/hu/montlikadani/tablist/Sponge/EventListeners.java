package hu.montlikadani.tablist.Sponge;

import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

public class EventListeners {

	@Listener
	public void onJoin(ClientConnectionEvent.Join event) {
		TabList.get().getTManager().loadTab(event.getTargetEntity());
	}

	@Listener
	public void onQuit(ClientConnectionEvent.Disconnect e) {
		TabList.get().getTManager().cancelTab(e.getTargetEntity());
	}
}
