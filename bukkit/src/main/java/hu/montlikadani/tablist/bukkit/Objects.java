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

	private BukkitTask task;

	@SuppressWarnings("deprecation")
	void registerHealthTab(Player pl) {
		String path = "tablist-object-type.object-settings.health.";
		if (plugin.getConfig().getStringList(path + "disabled-worlds").contains(pl.getWorld().getName())
				|| plugin.getConfig().getStringList(path + "restricted-players").contains(pl.getName())) {
			unregisterObjective(getObject(pl, ObjectTypes.HEALTH));
			return;
		}

		if (PluginUtils.isInGame(pl)) {
			return;
		}

		// TODO Fix not show correctly the health after reload

		org.bukkit.scoreboard.Scoreboard board = pl.getScoreboard();
		Objective objective = getObject(pl, ObjectTypes.HEALTH).orElse(null);
		if (objective == null) {
			String dName = ChatColor.RED + "\u2665";
			if (Version.isCurrentEqualOrHigher(Version.v1_13_R2)) {
				objective = board.registerNewObjective(ObjectTypes.HEALTH.getObjectName(), "health", dName,
						RenderType.HEARTS);
			} else {
				objective = board.registerNewObjective(ObjectTypes.HEALTH.getObjectName(), "health");
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

		task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			if (Bukkit.getOnlinePlayers().isEmpty()) {
				cancelTask();
				return;
			}

			final String type = ConfigValues.getObjectType().toLowerCase();

			for (Player player : Bukkit.getOnlinePlayers()) {
				if (plugin.getConfig().getStringList("tablist-object-type.object-settings." + type + ".disabled-worlds")
						.contains(player.getWorld().getName()) || PluginUtils.isInGame(player)) {
					continue;
				}

				Optional<Objective> obj = Optional.empty();
				int score = 0;

				if (type.equals("ping")) {
					obj = getObject(player, ObjectTypes.PING);
					if (!obj.isPresent()) {
						obj = Optional.of(
								player.getScoreboard().registerNewObjective(ObjectTypes.PING.getObjectName(), "dummy"));
					}

					if (!obj.isPresent()) {
						continue;
					}

					obj.get().setDisplayName("ms");

					score = TabListAPI.getPing(player);
				} else if (type.equals("custom")) {
					obj = getObject(player, ObjectTypes.CUSTOM);
					if (!obj.isPresent()) {
						obj = Optional.of(player.getScoreboard()
								.registerNewObjective(ObjectTypes.CUSTOM.getObjectName(), "dummy"));
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

				if (obj.isPresent()) {
					Objective object = obj.get();
					object.setDisplaySlot(DisplaySlot.PLAYER_LIST);

					if (Version.isCurrentEqualOrHigher(Version.v1_13_R2)) {
						object.setRenderType(RenderType.INTEGER);
					}

					final int s = score;
					if (object.getScore(player.getName()).getScore() != s) {
						for (Player p : Bukkit.getOnlinePlayers()) {
							(type.equals("custom") ? getObject(p, ObjectTypes.CUSTOM)
									: (type.equals("ping") ? getObject(p, ObjectTypes.PING) : Optional.empty()))
											.ifPresent(o -> ((Objective) o).getScore(player.getName()).setScore(s));
						}
					}
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