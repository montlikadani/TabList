package hu.montlikadani.tablist;

import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.living.player.KickPlayerEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import hu.montlikadani.tablist.utils.SchedulerUtil;

public final class EventListeners {

	private final TabList tl;

	public EventListeners(TabList tl) {
		this.tl = tl;
	}

	@Listener
	public void onJoin(ServerSideConnectionEvent.Join event) {
		SchedulerUtil.submitScheduleSyncTask(2L, java.time.temporal.ChronoUnit.MILLIS, t -> tl.updateAll(event.player()));
	}

	@Listener
	public void onQuit(ServerSideConnectionEvent.Disconnect e) {
		tl.onQuit(e.player());
	}

	@Listener
	public void onKick(KickPlayerEvent ev) {
		tl.onQuit(ev.player());
	}
}
