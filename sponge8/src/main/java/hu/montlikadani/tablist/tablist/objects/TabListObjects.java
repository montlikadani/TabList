package hu.montlikadani.tablist.tablist.objects;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scoreboard.Score;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.criteria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.scoreboard.objective.displaymode.ObjectiveDisplayModes;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.ConfigValues;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.SchedulerUtil;
import hu.montlikadani.tablist.utils.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TabListObjects {

	private final TabList plugin;

	private ScheduledTask task;

	private final AtomicInteger value = new AtomicInteger();
	private Scoreboard board;

	private Pattern customPatternReplacer;

	private Component tabObjectText;
	private Component healthObjectText;

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
		if (board != null) {
			board.clearSlot(DisplaySlots.LIST);
			getObjective(objectName).ifPresent(board::removeObjective);
		}
	}

	public Optional<Objective> getObjective(String name) {
		return board.objective(name);
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

		if (board == null) {
			board = Util.GLOBAL_SCORE_BOARD;
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

		SchedulerUtil.submitScheduleAsyncTask(interval, task -> {
			if (plugin.getTabUsers().isEmpty()) {
				cancelTask();
				return;
			}

			plugin.getTabUsers().forEach(user -> user.getPlayer().ifPresent(player -> {
				if (type == ObjectType.PING) {
					value.set(player.connection().latency());
				} else if (type == ObjectType.CUSTOM) {
					String result = plugin.getVariables().replaceIntegerVariables(player, ConfigValues.getCustomObject());

					if (customPatternReplacer == null) {
						customPatternReplacer = Pattern.compile("[^\\d]");
					}

					result = customPatternReplacer.matcher(result).replaceAll("");

					try {
						value.set(Integer.parseInt(result));
					} catch (NumberFormatException e) {
						System.err.println("Not correct custom objective: " + ConfigValues.getCustomObject());
					}
				}

				Optional<Objective> opt = getObjective(type.getName());

				Objective object = opt.orElseGet(() -> {
					if (tabObjectText == null) {
						tabObjectText = Component.text("tabObjects");
					}

					return Objective.builder().displayName(tabObjectText).objectiveDisplayMode(ObjectiveDisplayModes.INTEGER)
							.criterion(Criteria.DUMMY).name(type.getName()).build();
				});

				if (!opt.isPresent()) {
					board.addObjective(object);
					board.updateDisplaySlot(object, DisplaySlots.LIST);
				}

				Optional<Score> score = object.findScore(user.getName());

				if (!score.isPresent() || score.get().score() != value.get()) {
					plugin.getTabUsers().forEach(us -> us.getPlayer().ifPresent(pl -> {
						object.findOrCreateScore(us.getName()).setScore(value.get());
						pl.setScoreboard(board);
					}));
				}
			}));
		});
	}

	public void loadHealthObject(ServerPlayer player) {
		String objName = ObjectType.HEARTH.getName();
		Optional<Objective> opt = getObjective(objName);

		Objective object = opt.orElseGet(() -> {
			if (healthObjectText == null) {
				healthObjectText = Component.text("\u2665", NamedTextColor.RED);
			}

			return Objective.builder().displayName(healthObjectText).objectiveDisplayMode(ObjectiveDisplayModes.HEARTS)
					.criterion(Criteria.HEALTH).name(objName).build();
		});

		if (!opt.isPresent()) {
			board.addObjective(object);
		}

		board.updateDisplaySlot(object, DisplaySlots.LIST);
		player.setScoreboard(board);
	}
}
