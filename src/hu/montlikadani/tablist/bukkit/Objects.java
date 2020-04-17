package hu.montlikadani.tablist.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;

import hu.montlikadani.tablist.API.TabListAPI;
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

		if (plugin.getC().getStringList(path + "blacklisted-players").contains(pl.getName())) {
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
			if (Version.isCurrentEqualOrHigher(Version.v1_13_R1)) {
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

		final String main = "tablist-object-type.object-settings.";
		final int timer = 20 * plugin.getC().getInt(main + "refresh-interval", 3);

		task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			if (Bukkit.getOnlinePlayers().isEmpty()) {
				cancelTask();
				return;
			}

			final String type = plugin.getC().getString("tablist-object-type.type", "ping").toLowerCase();

			String path = main + type + ".";

			for (Player player : Bukkit.getOnlinePlayers()) {
				if (plugin.getC().getStringList(path + "disabled-worlds").contains(player.getWorld().getName())) {
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

					String value = plugin.getC().getString(path + "value",
							plugin.getC().getString(path + "custom-value"));
					try {
						score = Integer.parseInt(plugin.getPlaceholders().replaceVariables(player, value));
					} catch (NumberFormatException n) {
						Util.logConsole(java.util.logging.Level.WARNING,
								"Placeholder must be a number (NOT double): " + value);
						break;
					}
				}

				if (obj != null) {
					obj.setDisplaySlot(DisplaySlot.PLAYER_LIST);
					if (Version.isCurrentEqualOrHigher(Version.v1_13_R1)) {
						obj.setRenderType(RenderType.INTEGER);
					}

					obj.getScore(player.getName()).setScore(score);
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