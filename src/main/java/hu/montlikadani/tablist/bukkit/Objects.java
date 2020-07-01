package hu.montlikadani.tablist.bukkit;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;

import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public class Objects {

	private final TabList plugin = TabListAPI.getPlugin();

	private BukkitTask task;

	@SuppressWarnings("deprecation")
	void registerHealthTab(Player pl) {
		String path = "tablist-object-type.object-settings.health.";
		if (plugin.getC().getStringList(path + "disabled-worlds").contains(pl.getWorld().getName())) {
			unregisterHealthObjective(pl);
			return;
		}

		// For better understand and rule changes
		List<String> restrictedPlayers = plugin.getC().getStringList(path + "blacklisted-players");
		if (restrictedPlayers.isEmpty()) {
			restrictedPlayers = plugin.getC().getStringList(path + "restricted-players");
		}

		if (restrictedPlayers.contains(pl.getName())) {
			unregisterHealthObjective(pl);
			return;
		}

		if (plugin.isHookPreventTask(pl)) {
			return;
		}

		// TODO Fix not show correctly the health when reload

		org.bukkit.scoreboard.Scoreboard board = pl.getScoreboard();
		Objective objective = getHealthObject(pl);
		if (objective == null) {
			String dName = ChatColor.RED + "\u2665";
			if (Version.isCurrentEqualOrHigher(Version.v1_13_R2)) {
				objective = board.registerNewObjective("showhealth", "health", dName, RenderType.HEARTS);
			} else {
				objective = board.registerNewObjective("showhealth", "health");
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
				if (plugin.getC().getStringList("tablist-object-type.object-settings." + type + ".disabled-worlds")
						.contains(player.getWorld().getName())) {
					continue;
				}

				if (plugin.isHookPreventTask(player)) {
					continue;
				}

				Objective obj = null;
				int score = 0;

				if (type.equals("ping")) {
					obj = getPingObject(player);
					if (obj == null) {
						obj = player.getScoreboard().registerNewObjective("PingTab", "dummy");
					}

					if (obj == null) {
						continue;
					}

					obj.setDisplayName("ms");

					score = TabListAPI.getPing(player);
				} else if (type.equals("custom")) {
					obj = getCustomObject(player);
					if (obj == null) {
						obj = player.getScoreboard().registerNewObjective("customObj", "dummy");
					}

					if (obj == null) {
						continue;
					}

					final String value = ConfigValues.getCustomObjectSetting();
					String result = plugin.getPlaceholders().replaceVariables(player, value);
					result = result.replaceAll("[^\\d]", "");

					try {
						score = Integer.parseInt(result);
					} catch (NumberFormatException e) {
						Util.logConsole("Not correct custom objective: " + value);
					}
				}

				if (obj != null) {
					obj.setDisplaySlot(DisplaySlot.PLAYER_LIST);

					if (Version.isCurrentEqualOrHigher(Version.v1_13_R2)) {
						obj.setRenderType(RenderType.INTEGER);
					}

					if (obj.getScore(player.getName()).getScore() != score) {
						for (Player p : Bukkit.getOnlinePlayers()) {
							Objective object = type.equals("custom") ? getCustomObject(p)
									: (type.equals("ping") ? getPingObject(p) : null);
							if (object != null) {
								object.getScore(player.getName()).setScore(score);
							}
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
		// Do NOT use #isCancelled method, it depends from version
		return task == null;
	}

	public void unregisterPingTab() {
		Bukkit.getOnlinePlayers().forEach(this::unregisterPingTab);

		cancelTask();
	}

	public void unregisterPingTab(Player p) {
		Objective obj = getPingObject(p);
		if (obj != null) {
			obj.unregister();
		}
	}

	public void unregisterCustomValue() {
		Bukkit.getOnlinePlayers().forEach(this::unregisterCustomValue);

		cancelTask();
	}

	public void unregisterCustomValue(Player p) {
		Objective obj = getCustomObject(p);
		if (obj != null) {
			obj.unregister();
		}
	}

	public void unregisterHealthObjective() {
		Bukkit.getOnlinePlayers().forEach(this::unregisterHealthObjective);
	}

	public void unregisterHealthObjective(Player player) {
		if (getHealthObject(player) != null) {
			getHealthObject(player).unregister();
		}
	}

	public Objective getHealthObject(Player p) {
		return p.getScoreboard().getObjective("showhealth");
	}

	public Objective getPingObject(Player p) {
		return p.getScoreboard().getObjective("PingTab");
	}

	public Objective getCustomObject(Player p) {
		return p.getScoreboard().getObjective("customObj");
	}
}