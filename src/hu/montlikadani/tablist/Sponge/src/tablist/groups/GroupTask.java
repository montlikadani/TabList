package hu.montlikadani.tablist.Sponge.src.tablist.groups;

import java.util.function.Consumer;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import hu.montlikadani.tablist.Sponge.src.TabList;

public class GroupTask implements Consumer<Task> {

	private Task task;

	public GroupTask() {
		task = Task.builder().async().intervalTicks(4).execute(this::accept).submit(TabList.get());
	}

	public Task getTask() {
		return task;
	}

	public void cancel() {
		if (!isRunning()) {
			return;
		}

		task.cancel();
		task = null;
	}

	public boolean isRunning() {
		return task != null;
	}

	@Override
	public void accept(Task t) {
		if (Sponge.getServer().getOnlinePlayers().isEmpty()) {
			cancel();
			return;
		}

		for (TabGroup g : TabList.get().getGroupsList()) {
			for (Player p : Sponge.getServer().getOnlinePlayers()) {
				g.setGroup(p);
			}
		}
	}
}
