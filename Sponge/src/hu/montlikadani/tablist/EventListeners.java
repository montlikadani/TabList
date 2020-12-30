package hu.montlikadani.tablist;

import java.util.concurrent.TimeUnit;

import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.living.player.KickPlayerEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import hu.montlikadani.tablist.utils.SchedulerUtil;

public class EventListeners {

	@Listener
	public void onJoin(ServerSideConnectionEvent.Join event) {
		SchedulerUtil.submitScheduleSyncTask(2L, TimeUnit.MILLISECONDS, t -> TabList.get().updateAll(event.getPlayer()));
	}

	@Listener
	public void onQuit(ServerSideConnectionEvent.Disconnect e) {
		TabList.get().onQuit(e.getPlayer());
	}

	@Listener
	public void onKick(KickPlayerEvent ev) {
		TabList.get().onQuit(ev.getPlayer());
	}
}
