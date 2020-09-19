package hu.montlikadani.tablist.bukkit;

import java.util.List;
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

		if (PluginUtils.isInGame(pl)) {
			return;
		}

		// TODO Fix not show correctly the health when reload

		org.bukkit.scoreboard.Scoreboard board = pl.getScoreboard();
		Objective objective = getHealthObject(pl).orElse(null);
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

				if (PluginUtils.isInGame(player)) {
					continue;
				}

				Optional<Objective> obj = Optional.empty();
				int score = 0;

				if (type.equals("ping")) {
					obj = getPingObject(player);
					if (!obj.isPresent()) {
						obj = Optional.ofNullable(player.getScoreboard().registerNewObjective("PingTab", "dummy"));
					}

					if (!obj.isPresent()) {
						continue;
					}

					obj.get().setDisplayName("ms");

					score = TabListAPI.getPing(player);
				} else if (type.equals("custom")) {
					obj = getCustomObject(player);
					if (!obj.isPresent()) {
						obj = Optional.ofNullable(player.getScoreboard().registerNewObjective("customObj", "dummy"));
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
							(type.equals("custom") ? getCustomObject(p)
									: (type.equals("ping") ? getPingObject(p) : Optional.empty()))
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
		// Do NOT use #isCancelled method, it depends from version
		return task == null;
	}

	public void unregisterPingTab() {
		Bukkit.getOnlinePlayers().forEach(this::unregisterPingTab);
		cancelTask();
	}

	public void unregisterPingTab(Player p) {
		getPingObject(p).ifPresent(Objective::unregister);
	}

	public void unregisterCustomValue() {
		Bukkit.getOnlinePlayers().forEach(this::unregisterCustomValue);
		cancelTask();
	}

	public void unregisterCustomValue(Player p) {
		getCustomObject(p).ifPresent(Objective::unregister);
	}

	public void unregisterHealthObjective() {
		Bukkit.getOnlinePlayers().forEach(this::unregisterHealthObjective);
	}

	public void unregisterHealthObjective(Player player) {
		getHealthObject(player).ifPresent(Objective::unregister);
	}

	public Optional<Objective> getHealthObject(Player p) {
		return Optional.ofNullable(p.getScoreboard().getObjective("showhealth"));
	}

	public Optional<Objective> getPingObject(Player p) {
		return Optional.ofNullable(p.getScoreboard().getObjective("PingTab"));
	}

	public Optional<Objective> getCustomObject(Player p) {
		return Optional.ofNullable(p.getScoreboard().getObjective("customObj"));
	}
}