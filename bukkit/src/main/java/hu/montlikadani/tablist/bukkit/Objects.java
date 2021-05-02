package hu.montlikadani.tablist.bukkit;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

import hu.montlikadani.tablist.bukkit.api.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.PluginUtils;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.task.Tasks;

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
			unregisterObjective(getObject(pl.getScoreboard()));
			return;
		}

		// TODO Fix not show correctly the health after reload

		Scoreboard board = pl.getScoreboard();
		Objective objective = getObject(board).orElse(null);
		if (objective == null) {
			String dName = ChatColor.RED + "\u2665";

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
				objective = plugin.getComplement().registerNewObjective(board, ConfigValues.getObjectType().objectName,
						"health", dName, RenderType.HEARTS);
			} else {
				objective = board.registerNewObjective(ConfigValues.getObjectType().objectName, "health");
				plugin.getComplement().setDisplayName(objective, dName);
			}

			objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		}
	}

	void startTask() {
		cancelTask();

		if (plugin.getUsers().isEmpty()) {
			return;
		}

		task = Tasks.submitAsync(() -> {
			if (plugin.getUsers().isEmpty()) {
				cancelTask();
				return;
			}

			for (TabListUser user : plugin.getUsers()) {
				Player player = user.getPlayer();

				if (ConfigValues.getObjectsDisabledWorlds().contains(player.getWorld().getName())
						|| PluginUtils.isInGame(player)) {
					continue;
				}

				Objective object = getObject(player.getScoreboard()).orElse(null);
				if (object == null) {
					object = player.getScoreboard().registerNewObjective(ConfigValues.getObjectType().objectName,
							"dummy");

					object.setDisplaySlot(DisplaySlot.PLAYER_LIST);

					if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
						object.setRenderType(RenderType.INTEGER);
					}
				}

				if (ConfigValues.getObjectType() == ObjectTypes.PING) {
					plugin.getComplement().setDisplayName(object, "ms");
					objectScore.set(TabListAPI.getPing(player));
				} else if (ConfigValues.getObjectType() == ObjectTypes.CUSTOM) {
					String result = plugin.getPlaceholders().replaceVariables(player,
							ConfigValues.getCustomObjectSetting());
					result = result.replaceAll("[^\\d]", "");

					try {
						objectScore.set(Integer.parseInt(result));
					} catch (NumberFormatException e) {
						Util.logConsole(
								"Invalid custom objective with " + ConfigValues.getCustomObjectSetting() + " value.");
						return;
					}
				}

				//final String uId = player.getUniqueId().toString();
				String pName = player.getName();
				if (pName.length() > 40) {
					pName = pName.substring(0, 40);
				}

				final String n = pName;

				if (object.getScore(n).getScore() != objectScore.get()) {
					plugin.getUsers().forEach(all -> getObject(all.getPlayer().getScoreboard())
							.ifPresent(o -> o.getScore(n).setScore(objectScore.get())));
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

	public void unregisterObjective(final Optional<Objective> obj) {
		obj.ifPresent(Objective::unregister);
	}

	public void unregisterObjectivesForEveryone() {
		if (!plugin.getUsers().isEmpty()) {
			for (ObjectTypes t : ObjectTypes.values()) {
				for (TabListUser user : plugin.getUsers()) {
					unregisterObjective(getObject(user.getPlayer().getScoreboard(), t));
				}
			}
		}
	}

	public Optional<Objective> getObject(Scoreboard board) {
		return getObject(board, ConfigValues.getObjectType());
	}

	public Optional<Objective> getObject(Scoreboard board, ObjectTypes type) {
		return Optional.ofNullable(board.getObjective(type.objectName));
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