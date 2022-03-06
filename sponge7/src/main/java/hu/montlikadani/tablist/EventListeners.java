package hu.montlikadani.tablist;

import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

public final class EventListeners {

	private final TabList tl;

	public EventListeners(TabList tl) {
		this.tl = tl;
	}

	@Listener
	public void onJoin(ClientConnectionEvent.Join event) {
		org.spongepowered.api.scheduler.Task.builder().delayTicks(2L).execute(t -> tl.updateAll(event.getTargetEntity()))
				.submit(tl);
	}

	@Listener
	public void onQuit(ClientConnectionEvent.Disconnect e) {
		tl.onQuit(e.getTargetEntity());
	}

	@Listener
	public void onKick(org.spongepowered.api.event.entity.living.humanoid.player.KickPlayerEvent ev) {
		tl.onQuit(ev.getTargetEntity());
	}
}
