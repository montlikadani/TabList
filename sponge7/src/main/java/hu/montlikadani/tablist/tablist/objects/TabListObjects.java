package hu.montlikadani.tablist.tablist.objects;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

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

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.ConfigValues;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.Util;

public class TabListObjects {

	private final TabList plugin;

	private Task task;

	private final AtomicInteger value = new AtomicInteger();
	private final Scoreboard board = Util.GLOBAL_SCORE_BOARD;

	private Pattern customPatternReplacer;

	private final Text tabObjectText = Text.of("tabObjects");
	private final Text healthObjectText = Text.of(TextColors.RED, "\u2665");

	public TabListObjects(TabList plugin) {
		this.plugin = plugin;
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
		for (ObjectType types : ObjectType.VALUES) {
			unregisterObjective(types.getName());
		}
	}

	public void unregisterObjective(String objectName) {
		board.clearSlot(DisplaySlots.LIST);
		getObjective(objectName).ifPresent(board::removeObjective);
	}

	public Optional<Objective> getObjective(String name) {
		return board.getObjective(name);
	}

	public void loadObjects() {
		cancelTask();

		if (plugin.getTabUsers().isEmpty()) {
			return;
		}

		final ObjectType type = ConfigValues.getTablistObjectsType();
		if (type == ObjectType.NONE) {
			return;
		}

		if (type == ObjectType.HEARTH) {
			for (TabListUser user : plugin.getTabUsers()) {
				user.getPlayer().ifPresent(player -> loadHealthObject(player));
			}

			return;
		}

		int interval = ConfigValues.getObjectsRefreshInterval();
		if (interval < 1) {
			return;
		}

		task = Task.builder().async().interval(interval, TimeUnit.SECONDS).execute(() -> {
			if (plugin.getTabUsers().isEmpty()) {
				cancelTask();
				return;
			}

			plugin.getTabUsers().forEach(user -> user.getPlayer().ifPresent(player -> {
				if (type == ObjectType.PING) {
					value.set(player.getConnection().getLatency());
				} else if (type == ObjectType.CUSTOM) {
					String result = plugin.getVariables().replaceIntegerVariables(player, ConfigValues.getCustomObject());

					if (customPatternReplacer == null) {
						customPatternReplacer = Pattern.compile("[^\\d]");
					}

					result = customPatternReplacer.matcher(result).replaceAll("");

					try {
						value.set(Integer.parseInt(result));
					} catch (NumberFormatException e) {
						TabList.LOGGER.warn("Not correct custom objective: " + ConfigValues.getCustomObject());
					}
				}

				Optional<Objective> opt = getObjective(type.getName());
				Objective object = opt.orElseGet(
						() -> Objective.builder().displayName(tabObjectText).objectiveDisplayMode(ObjectiveDisplayModes.INTEGER)
								.criterion(Criteria.DUMMY).name(type.getName()).build());

				if (!opt.isPresent()) {
					board.addObjective(object);
					board.updateDisplaySlot(object, DisplaySlots.LIST);
				}

				Optional<Score> score = object.getScore(user.getName());

				if (!score.isPresent() || score.get().getScore() != value.get()) {
					plugin.getTabUsers().forEach(us -> us.getPlayer().ifPresent(pl -> {
						object.getOrCreateScore(us.getName()).setScore(value.get());
						pl.setScoreboard(board);
					}));
				}
			}));
		}).submit(plugin);
	}

	public void loadHealthObject(Player player) {
		String objName = ObjectType.HEARTH.getName();
		Optional<Objective> opt = getObjective(objName);
		Objective object = opt.orElseGet(() -> Objective.builder().displayName(healthObjectText)
				.objectiveDisplayMode(ObjectiveDisplayModes.HEARTS).criterion(Criteria.HEALTH).name(objName).build());

		if (!opt.isPresent()) {
			board.addObjective(object);
		}

		board.updateDisplaySlot(object, DisplaySlots.LIST);
		player.setScoreboard(board);
	}
}
