package hu.montlikadani.tablist.sponge.tablist.objects;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scoreboard.Score;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.critieria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.scoreboard.objective.displaymode.ObjectiveDisplayModes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import hu.montlikadani.tablist.sponge.ConfigValues;
import hu.montlikadani.tablist.sponge.Debug;
import hu.montlikadani.tablist.sponge.TabList;

public class TabListObjects {

	private TabList plugin;

	private Task task;

	private ObjectType type = ObjectType.NONE;

	public TabListObjects(TabList plugin) {
		this.plugin = plugin;
	}

	public Optional<Task> getTask() {
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

	public void unregisterAllObjective(Player player) {
		for (ObjectType types : ObjectType.values()) {
			unregisterObjective(player, types.getName());
		}
	}

	public void unregisterObjective(String objectName) {
		TabList.BOARD.clearSlot(DisplaySlots.LIST);
		getObjective(objectName).ifPresent(TabList.BOARD::removeObjective);
	}

	public void unregisterObjective(Player player, String objectName) {
		Scoreboard b = player.getScoreboard();
		b.clearSlot(DisplaySlots.LIST);
		b.getObjective(objectName).ifPresent(b::removeObjective);
	}

	public Optional<Objective> getObjective(String name) {
		return TabList.BOARD.getObjective(name);
	}

	public void loadObjects() {
		cancelTask();

		if (Sponge.getServer().getOnlinePlayers().isEmpty()) {
			return;
		}

		type = ObjectType.getByName(ConfigValues.getTablistObjectsType());
		if (type == ObjectType.NONE) {
			return;
		}

		if (type == ObjectType.HEARTH) {
			Sponge.getServer().getOnlinePlayers().forEach(this::loadHealthObject);
			return;
		}

		int interval = ConfigValues.getObjectsRefreshInterval();
		if (interval < 1) {
			return;
		}

		task = Task.builder().async().interval(interval, TimeUnit.SECONDS).execute(() -> {
			if (Sponge.getServer().getOnlinePlayers().isEmpty()) {
				cancelTask();
				return;
			}

			Sponge.getServer().getOnlinePlayers().forEach(all -> {
				int score = 0;
				if (type == ObjectType.PING) {
					score = all.getConnection().getLatency();
				} else if (type == ObjectType.CUSTOM) {
					String result = TextSerializers.PLAIN
							.serialize(plugin.getVariables().replaceVariables(all, ConfigValues.getCustomObject()));

					result = result.replaceAll("[^\\d]", "");

					try {
						score = Integer.parseInt(result);
					} catch (NumberFormatException e) {
						Debug.warn("Not correct custom objective: " + ConfigValues.getCustomObject());
					}
				}

				final String objName = type.getName();
				final Objective object = getObjective(objName)
						.orElse(Objective.builder().displayName(Text.of("tabObjects")).name(objName)
								.objectiveDisplayMode(ObjectiveDisplayModes.INTEGER).criterion(Criteria.DUMMY).build());
				final Scoreboard board = TabList.BOARD;

				if (!board.getObjective(objName).isPresent()) {
					board.addObjective(object);
				}

				board.updateDisplaySlot(object, DisplaySlots.LIST);

				final int fScore = score;
				Optional<Score> s = object.getScore(Text.of(all.getName()));
				if (!s.isPresent() || s.get().getScore() != fScore) {
					Sponge.getServer().getOnlinePlayers().forEach(p -> {
						getObjective(objName).ifPresent(obj -> {
							obj.getOrCreateScore(Text.of(all.getName())).setScore(fScore);
							p.setScoreboard(board);
						});
					});
				}
			});
		}).submit(plugin);
	}

	public void loadHealthObject(Player p) {
		String objName = ObjectType.HEARTH.getName();
		Objective object = getObjective(objName)
				.orElse(Objective.builder().displayName(Text.of(TextColors.RED, "\u2665")).name(objName)
						.objectiveDisplayMode(ObjectiveDisplayModes.HEARTS).criterion(Criteria.HEALTH).build());

		if (!TabList.BOARD.getObjective(objName).isPresent()) {
			TabList.BOARD.addObjective(object);
		}

		TabList.BOARD.updateDisplaySlot(object, DisplaySlots.LIST);
		p.setScoreboard(TabList.BOARD);
	}
}
