package hu.montlikadani.tablist;

import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

import hu.montlikadani.tablist.api.TabListAPI;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.StrUtil;
import hu.montlikadani.tablist.utils.task.Tasks;

@SuppressWarnings("deprecation")
public final class Objects {

	private final TabList plugin;
	private final AtomicInteger objectScore = new AtomicInteger();

	private BukkitTask task;

	public Objects(TabList plugin) {
		this.plugin = plugin;
	}

	void registerHealthTab(Player pl) {
		if (ConfigValues.getObjectsDisabledWorlds().contains(pl.getWorld().getName())
				|| ConfigValues.getHealthObjectRestricted().contains(pl.getName())) {
			unregisterObjective(pl.getScoreboard().getObjective(ConfigValues.getObjectType().objectName));
			return;
		}

		// TODO Fix not show correctly the health after reload

		final Scoreboard board = pl.getScoreboard();
		final String objectName = ConfigValues.getObjectType().objectName;

		if (board.getObjective(objectName) != null) {
			return;
		}

		Tasks.submitSync(() -> {
			Objective objective;

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
				objective = plugin.getComplement().registerNewObjective(board, objectName, "health", objectName,
						RenderType.HEARTS);
			} else {
				objective = board.registerNewObjective(objectName, "health");
				plugin.getComplement().setDisplayName(objective, org.bukkit.ChatColor.RED + "\u2665");
			}

			objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
			return 1;
		});
	}

	void startTask() {
		cancelTask();

		task = Tasks.submitAsync(() -> {
			if (plugin.getUsers().isEmpty()) {
				cancelTask();
				return;
			}

			for (TabListUser user : plugin.getUsers()) {
				Player player = user.getPlayer();

				if (player == null || ConfigValues.getObjectsDisabledWorlds().contains(player.getWorld().getName())) {
					continue;
				}

				ObjectTypes type = ConfigValues.getObjectType();
				Scoreboard board = player.getScoreboard();
				Objective object = board.getObjective(type.objectName);

				if (object == null) {
					if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
						object = plugin.getComplement().registerNewObjective(board, type.objectName, "dummy",
								type.objectName, RenderType.INTEGER);
					} else {
						object = board.registerNewObjective(type.objectName, "dummy");
					}

					object.setDisplaySlot(DisplaySlot.PLAYER_LIST);

					if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
						object.setRenderType(RenderType.INTEGER);
					}

					if (type == ObjectTypes.PING) {
						plugin.getComplement().setDisplayName(object, "ms");
					}
				}

				if (type == ObjectTypes.PING) {
					objectScore.set(TabListAPI.getPing(player));
				} else if (type == ObjectTypes.CUSTOM) {
					String result = plugin.getPlaceholders().replaceVariables(player,
							ConfigValues.getCustomObjectSetting());
					result = StrUtil.getNumberEscapeSequence().matcher(result).replaceAll("");

					try {
						objectScore.set(Integer.parseInt(result));
					} catch (NumberFormatException e) {
						hu.montlikadani.tablist.utils.Util.logConsole(
								"Invalid custom objective with " + ConfigValues.getCustomObjectSetting() + " value.");
						return;
					}
				}

				String entry = player.getName();

				if (ServerVersion.isCurrentLower(ServerVersion.v1_18_R1)) {
					if (entry.length() > 40) {
						entry = entry.substring(0, 40);
					}
				} else if (entry.length() > Short.MAX_VALUE) {
					entry = entry.substring(0, Short.MAX_VALUE);
				}

				if (object.getScore(entry).getScore() != objectScore.get()) {
					for (TabListUser us : plugin.getUsers()) {
						Player pl = us.getPlayer();

						if (pl != null) {
							Objective objective = pl.getScoreboard().getObjective(type.objectName);

							if (objective != null) {
								objective.getScore(entry).setScore(objectScore.get());
							}
						}
					}
				}
			}
		}, ConfigValues.getObjectRefreshInterval(), ConfigValues.getObjectRefreshInterval());
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

	public void unregisterObjective(Objective obj) {
		if (obj != null) {
			obj.unregister();
		}
	}

	public enum ObjectTypes {
		HEALTH("showhealth"), PING("PingTab"), CUSTOM("customObj");

		public final String loweredName = toString().toLowerCase(java.util.Locale.ENGLISH);

		private final String objectName;

		ObjectTypes(String objectName) {
			this.objectName = objectName;
		}

		public String getObjectName() {
			return objectName;
		}
	}
}