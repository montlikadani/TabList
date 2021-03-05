package hu.montlikadani.tablist.bukkit;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.PluginUtils;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.task.Tasks;

@SuppressWarnings("deprecation")
public class Objects {

	private final TabList plugin;
	private final AtomicInteger objectScore = new AtomicInteger();

	private ObjectTypes currentObjectType = ObjectTypes.PING;
	private BukkitTask task;

	public Objects(TabList plugin) {
		this.plugin = plugin;
	}

	public ObjectTypes getCurrentObjectType() {
		return currentObjectType;
	}

	void registerHealthTab(Player pl) {
		currentObjectType = ObjectTypes.HEALTH;

		if (ConfigValues.getObjectsDisabledWorlds().contains(pl.getWorld().getName())
				|| ConfigValues.getHealthObjectRestricted().contains(pl.getName())) {
			unregisterObjective(getObject(pl.getScoreboard(), currentObjectType));
			return;
		}

		// TODO Fix not show correctly the health after reload

		Scoreboard board = pl.getScoreboard();
		Objective objective = getObject(board, currentObjectType).orElse(null);
		if (objective == null) {
			String dName = ChatColor.RED + "\u2665";
			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
				objective = plugin.getComplement().registerNewObjective(board, currentObjectType.getObjectName(),
						"health", dName, RenderType.HEARTS);
			} else {
				objective = board.registerNewObjective(currentObjectType.getObjectName(), "health");
				plugin.getComplement().setDisplayName(objective, dName);
			}

			objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		}
	}

	void startTask() {
		cancelTask();

		if (Bukkit.getOnlinePlayers().isEmpty()) {
			return;
		}

		final int timer = 20 * ConfigValues.getObjectRefreshInterval();

		try {
			currentObjectType = ObjectTypes.valueOf(ConfigValues.getObjectType().toUpperCase());
		} catch (IllegalArgumentException e) {
			currentObjectType = ObjectTypes.PING;
		}

		task = Tasks.submitAsync(() -> {
			if (Bukkit.getOnlinePlayers().isEmpty()) {
				cancelTask();
				return;
			}

			for (Player player : Bukkit.getOnlinePlayers()) {
				if (ConfigValues.getObjectsDisabledWorlds().contains(player.getWorld().getName())
						|| PluginUtils.isInGame(player)) {
					continue;
				}

				Objective object = getObject(player.getScoreboard(), currentObjectType).orElse(null);
				if (object == null) {
					object = player.getScoreboard().registerNewObjective(currentObjectType.getObjectName(), "dummy");
				}

				if (currentObjectType == ObjectTypes.PING) {
					plugin.getComplement().setDisplayName(object, "ms");
					objectScore.set(TabListAPI.getPing(player));
				} else if (currentObjectType == ObjectTypes.CUSTOM) {
					String result = plugin.getPlaceholders().replaceVariables(player,
							ConfigValues.getCustomObjectSetting());
					result = result.replaceAll("[^\\d]", "");

					try {
						objectScore.set(Integer.parseInt(result));
					} catch (NumberFormatException e) {
						Util.logConsole(
								"Invalid custom objective with " + ConfigValues.getCustomObjectSetting() + " value.");
						continue;
					}
				}

				object.setDisplaySlot(DisplaySlot.PLAYER_LIST);

				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
					object.setRenderType(RenderType.INTEGER);
				}

				if (object.getScore(player.getUniqueId().toString()).getScore() != objectScore.get()) {
					Bukkit.getOnlinePlayers().forEach(all -> getObject(all.getScoreboard(), currentObjectType)
							.ifPresent(o -> o.getScore(player.getUniqueId().toString()).setScore(objectScore.get())));
				}
			}
		}, timer, timer);
	}

	public void cancelTask() {
		if (!isCancelled()) {
			task.cancel();
			task = null;
		}
	}

	public boolean isCancelled() {
		// Do NOT use #isCancelled method, version-dependent
		return task == null;
	}

	public void unregisterObjective(final Optional<Objective> obj) {
		obj.ifPresent(Objective::unregister);
	}

	public void unregisterObjectiveForEveryone(final ObjectTypes type) {
		Bukkit.getOnlinePlayers().forEach(p -> unregisterObjective(getObject(p.getScoreboard(), type)));
		cancelTask();
	}

	public Optional<Objective> getObject(Scoreboard board, ObjectTypes type) {
		return Optional.ofNullable(board.getObjective(type.getObjectName()));
	}

	public enum ObjectTypes {
		HEALTH("showhealth"), PING("PingTab"), CUSTOM("customObj");

		private String objectName;

		ObjectTypes(String objectName) {
			this.objectName = objectName;
		}

		public String getObjectName() {
			return objectName;
		}
	}
}