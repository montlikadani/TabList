package hu.montlikadani.tablist.Sponge.src;

import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;

public class EventListeners {

	@Listener
	public void onJoin(ClientConnectionEvent.Join event) {
		Task.builder().delayTicks(15L).execute(t -> TabList.get().updateAll(event.getTargetEntity()))
				.submit(TabList.get());
	}

	@Listener
	public void onQuit(ClientConnectionEvent.Disconnect e) {
		TabList.get().onQuit(e.getTargetEntity());
	}
}
