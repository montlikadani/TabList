package hu.montlikadani.tablist.bukkit;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;

import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.ConfigValues;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.PluginUtils;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public class Objects {

	private final TabList plugin = TabListAPI.getPlugin();

	private ObjectTypes currentObjectType = ObjectTypes.PING;
	private BukkitTask task;

	public ObjectTypes getCurrentObjectType() {
		return currentObjectType;
	}

	@SuppressWarnings("deprecation")
	void registerHealthTab(Player pl) {
		currentObjectType = ObjectTypes.HEALTH;

		String path = "tablist-object-type.object-settings.health.";
		if (plugin.getConfig().getStringList(path + "disabled-worlds").contains(pl.getWorld().getName())
				|| plugin.getConfig().getStringList(path + "restricted-players").contains(pl.getName())) {
			unregisterObjective(getObject(pl, currentObjectType));
			return;
		}

		if (PluginUtils.isInGame(pl)) {
			return;
		}

		// TODO Fix not show correctly the health after reload

		org.bukkit.scoreboard.Scoreboard board = pl.getScoreboard();
		Objective objective = getObject(pl, currentObjectType).orElse(null);
		if (objective == null) {
			String dName = ChatColor.RED + "\u2665";
			if (Version.isCurrentEqualOrHigher(Version.v1_13_R2)) {
				objective = board.registerNewObjective(currentObjectType.getObjectName(), "health", dName,
						RenderType.HEARTS);
			} else {
				objective = board.registerNewObjective(currentObjectType.getObjectName(), "health");
				objective.setDisplayName(dName);
			}

			objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		}
	}

	@SuppressWarnings("deprecation")
	void startTask() {
		cancelTask();

		if (Bukkit.getOnlinePlayers().isEmpty()) {
			return;
		}

		final int timer = 20 * ConfigValues.getObjectRefreshInterval();

		currentObjectType = ObjectTypes.valueOf(ConfigValues.getObjectType().toUpperCase());
		if (currentObjectType == null) {
			currentObjectType = ObjectTypes.PING;
		}

		task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			if (Bukkit.getOnlinePlayers().isEmpty()) {
				cancelTask();
				return;
			}

			for (Player player : Bukkit.getOnlinePlayers()) {
				if (plugin.getConfig()
						.getStringList("tablist-object-type.object-settings." + currentObjectType.name().toLowerCase()
								+ ".disabled-worlds")
						.contains(player.getWorld().getName()) || PluginUtils.isInGame(player)) {
					continue;
				}

				Optional<Objective> obj = Optional.empty();
				int score = 0;

				if (currentObjectType == ObjectTypes.PING) {
					obj = getObject(player, currentObjectType);
					if (!obj.isPresent()) {
						obj = Optional.of(player.getScoreboard().registerNewObjective(currentObjectType.getObjectName(),
								"dummy"));
					}

					if (!obj.isPresent()) {
						continue;
					}

					obj.get().setDisplayName("ms");

					score = TabListAPI.getPing(player);
				} else if (currentObjectType == ObjectTypes.CUSTOM) {
					obj = getObject(player, currentObjectType);
					if (!obj.isPresent()) {
						obj = Optional.of(player.getScoreboard().registerNewObjective(currentObjectType.getObjectName(),
								"dummy"));
					}

					if (!obj.isPresent()) {
						continue;
					}

					final String value = ConfigValues.getCustomObjectSetting();
					String result = plugin.getPlaceholders().replaceVariables(player, value);
					result = result.replaceAll("[^\\d]", "");

					try {
						score = Integer.parseInt(result);
					} catch (NumberFormatException e) {
						Util.logConsole("Not correct custom objective: " + value);
						continue;
					}
				}

				final int s = score;
				obj.ifPresent(object -> {
					object.setDisplaySlot(DisplaySlot.PLAYER_LIST);

					if (Version.isCurrentEqualOrHigher(Version.v1_13_R2)) {
						object.setRenderType(RenderType.INTEGER);
					}

					if (object.getScore(player.getName()).getScore() != s) {
						Bukkit.getOnlinePlayers().forEach(all -> getObject(all, currentObjectType)
								.ifPresent(o -> o.getScore(player.getName()).setScore(s)));
					}
				});
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
		Bukkit.getOnlinePlayers().forEach(p -> unregisterObjective(getObject(p, type)));
		cancelTask();
	}

	public Optional<Objective> getObject(Player p, ObjectTypes type) {
		return Optional.ofNullable(p.getScoreboard().getObjective(type.getObjectName()));
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