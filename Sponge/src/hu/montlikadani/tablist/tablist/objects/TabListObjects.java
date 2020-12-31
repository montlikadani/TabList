package hu.montlikadani.tablist.tablist.objects;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scoreboard.Score;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.criteria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.scoreboard.objective.displaymode.ObjectiveDisplayModes;

import hu.montlikadani.tablist.ConfigValues;
import hu.montlikadani.tablist.Debug;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.player.ITabPlayer;
import hu.montlikadani.tablist.utils.SchedulerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;

public class TabListObjects {

	private TabList plugin;

	private ScheduledTask task;

	private ObjectType type = ObjectType.NONE;

	public TabListObjects(TabList plugin) {
		this.plugin = plugin;
	}

	public Optional<ScheduledTask> getTask() {
		return Optional.ofNullable(task);
	}

	public ObjectType getObjectType() {
		return type;
	}

	public void cancelTask() {
		if (!isCancelled()) {
			task.cancel();
			task = null;
		}
	}

	public boolean isCancelled() {
		return task == null;
	}

	public void unregisterAllObjective() {
		for (ObjectType types : ObjectType.values()) {
			unregisterObjective(types.getName());
		}
	}

	public void unregisterAllObjective(ITabPlayer player) {
		for (ObjectType types : ObjectType.values()) {
			unregisterObjective(player, types.getName());
		}
	}

	public void unregisterObjective(String objectName) {
		TabList.board.clearSlot(DisplaySlots.LIST);
		getObjective(objectName).ifPresent(TabList.board::removeObjective);
	}

	public void unregisterObjective(final ITabPlayer player, final String objectName) {
		player.asServerPlayer().ifPresent(sp -> {
			Scoreboard b = sp.getScoreboard();
			synchronized (b) { // Avoiding concurrentModify
				b.clearSlot(DisplaySlots.LIST);
			}

			b.getObjective(objectName).ifPresent(b::removeObjective);
		});
	}

	public Optional<Objective> getObjective(String name) {
		return TabList.board.getObjective(name);
	}

	public void loadObjects() {
		cancelTask();

		if (plugin.getTabPlayers().isEmpty()) {
			return;
		}

		type = ObjectType.getByName(ConfigValues.getTablistObjectsType());
		if (type == ObjectType.NONE) {
			return;
		}

		if (type == ObjectType.HEARTH) {
			plugin.getTabPlayers().forEach(this::loadHealthObject);
			return;
		}

		int interval = ConfigValues.getObjectsRefreshInterval();
		if (interval < 1) {
			return;
		}

		task = SchedulerUtil.submitScheduleAsyncTask(interval, TimeUnit.SECONDS, t -> {
			if (plugin.getTabPlayers().isEmpty()) {
				cancelTask();
				return;
			}

			plugin.getTabPlayers().forEach(all -> all.asServerPlayer().ifPresent(sp -> {
				int score = 0;
				if (type == ObjectType.PING) {
					score = sp.getConnection().getLatency();
				} else if (type == ObjectType.CUSTOM) {
					String result = PlainComponentSerializer.plain()
							.serialize(plugin.getVariables().replaceVariables(sp, ConfigValues.getCustomObject()));

					result = result.replaceAll("[^\\d]", "");

					try {
						score = Integer.parseInt(result);
					} catch (NumberFormatException e) {
						Debug.warn("Not correct custom objective: " + ConfigValues.getCustomObject());
					}
				}

				final String objName = type.getName();
				final Objective object = getObjective(objName)
						.orElse(Objective.builder().displayName(Component.text("tabObjects")).name(objName)
								.objectiveDisplayMode(ObjectiveDisplayModes.INTEGER).criterion(Criteria.DUMMY).build());
				final Scoreboard board = TabList.board;

				if (!board.getObjective(objName).isPresent()) {
					board.addObjective(object);
				}

				board.updateDisplaySlot(object, DisplaySlots.LIST);

				final int fScore = score;
				Optional<Score> s = object.getScore(Component.text(sp.getName()));
				if (!s.isPresent() || s.get().getScore() != fScore) {
					plugin.getTabPlayers().forEach(tabP -> tabP.asServerPlayer()
							.ifPresent(serverPlayer -> getObjective(objName).ifPresent(obj -> {
								obj.getOrCreateScore(Component.text(sp.getName())).setScore(fScore);
								serverPlayer.setScoreboard(board);
							})));
				}
			}));
		});
	}

	public void loadHealthObject(ITabPlayer p) {
		if (!p.asServerPlayer().isPresent()) {
			return;
		}

		String objName = ObjectType.HEARTH.getName();
		Objective object = getObjective(objName)
				.orElse(Objective.builder().displayName(Component.text("\u2665", NamedTextColor.RED)).name(objName)
						.objectiveDisplayMode(ObjectiveDisplayModes.HEARTS).criterion(Criteria.HEALTH).build());

		if (!TabList.board.getObjective(objName).isPresent()) {
			TabList.board.addObjective(object);
		}

		TabList.board.updateDisplaySlot(object, DisplaySlots.LIST);
		p.asServerPlayer().get().setScoreboard(TabList.board);
	}
}
