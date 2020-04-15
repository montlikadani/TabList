package hu.montlikadani.tablist.bukkit;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;
import ru.tehkode.permissions.bukkit.PermissionsEx;

@SuppressWarnings("deprecation")
public class Groups {

	private TabList plugin;

	private BukkitTask animationTask = null;
	private Integer simpleTask = -1;

	private final List<TeamHandler> groupsList = new ArrayList<>();

	public Groups(TabList plugin) {
		this.plugin = plugin;
	}

	public List<TeamHandler> getGroupsList() {
		return groupsList;
	}

	protected void load() {
		if (!plugin.getC().getBoolean("change-prefix-suffix-in-tablist.enable")) {
			return;
		}

		plugin.getConf().createGroupsFile();

		// Automatically add existing groups to the list for "lazy peoples"
		if (plugin.getC().getBoolean("change-prefix-suffix-in-tablist.sync-plugins-groups-with-tablist", false)
				&& plugin.isPluginEnabled("Vault")) {
			boolean have = false;

			me: for (String s : plugin.getVaultPerm().getGroups()) {
				for (String g : plugin.getGS().getConfigurationSection("groups").getKeys(false)) {
					if (s.equalsIgnoreCase(g)) {
						continue me;
					}
				}

				String path = "groups." + s + ".";

				// This again for lazy peoples
				ChatColor[] colors = ChatColor.values();
				ChatColor c = colors[ThreadLocalRandom.current().nextInt(colors.length)];

				String cResult = "&" + c.getChar();
				plugin.getGS().set(path + "prefix", cResult);

				c = colors[ThreadLocalRandom.current().nextInt(colors.length)];

				cResult = "&" + c.getChar();
				plugin.getGS().set(path + "suffix", cResult);

				have = true;
			}

			if (have) {
				try {
					plugin.getGS().save(plugin.getConf().getGroupsFile());
				} catch (java.io.IOException e) {
					e.printStackTrace();
				}
			}
		}

		if (plugin.getGS().contains("groups")) {
			for (String g : plugin.getGS().getConfigurationSection("groups").getKeys(false)) {
				if (g.equalsIgnoreCase("exampleGroup")) {
					continue;
				}

				String path = "groups." + g + ".";

				String prefix = plugin.getGS().getString(path + "prefix", "");
				String suffix = plugin.getGS().getString(path + "suffix", "");

				String perm = plugin.getGS().getString(path + "permission", "");
				if (perm.trim().isEmpty()) {
					perm = "tablist." + g;
				}

				int priority = plugin.getGS().getInt(path + "sort-priority", 0);

				groupsList.add(new TeamHandler(g, prefix, suffix, perm, priority));
			}
		}

		startTask(null);
	}

	public void loadGroupForPlayer(final Player p) {
		removeGroup(p);

		if (plugin.getC().getBoolean("change-prefix-suffix-in-tablist.enable")) {
			startTask(p);
		}
	}

	private void setGroup(Player p) {
		if (!isPlayerCanSeeGroup(p)) {
			return;
		}

		for (final TeamHandler team : groupsList) {
			String name = team.getTeam();

			if (name.equalsIgnoreCase(p.getName())) {
				setName(p, team);
				break;
			}

			boolean change = false;
			if (plugin.getC().getBoolean("change-prefix-suffix-in-tablist.use-vault-group-names", false)
					&& plugin.isPluginEnabled("Vault")) {
				for (String gn : plugin.getVaultPerm().getPlayerGroups(p)) {
					if (gn.equalsIgnoreCase(name)) {
						change = true;
						break;
					}
				}
			} else if (plugin.isPluginEnabled("PermissionsEx")) {
				if (PermissionsEx.getPermissionManager().has(p, team.getPermission())) {
					change = true;
				}
			} else if (p.hasPermission(team.getPermission())) {
				change = true;
			}

			if (change) {
				setName(p, team);
				break;
			}
		}
	}

	private void setName(Player p, TeamHandler team) {
		String prefix = plugin.getPlaceholders().replaceVariables(p, plugin.makeAnim(team.getPrefix()));
		String suffix = plugin.getPlaceholders().replaceVariables(p, plugin.makeAnim(team.getSuffix()));

		String phPath = "placeholder-format.afk-status.";
		if (plugin.getC().getBoolean(phPath + "enable") && plugin.isAfk(p, false)
				&& !plugin.getC().getBoolean(phPath + "show-player-group")) {
			return;
		}

		final boolean rightLeft = plugin.getC().getBoolean(phPath + "show-in-right-or-left-side");

		if (plugin.getChangeType().equals("scoreboard")) {
			if (plugin.getC().getBoolean(phPath + "enable")) {
				if (rightLeft) {
					suffix = suffix + colorMsg(
							plugin.getC().getString(phPath + "format-" + (plugin.isAfk(p, false) ? "yes" : "no"), ""));
				} else {
					prefix = colorMsg(
							plugin.getC().getString(phPath + "format-" + (plugin.isAfk(p, false) ? "yes" : "no"), ""))
							+ prefix;
				}
			}

			if (plugin.isPluginEnabled("Essentials")
					&& plugin.getC().getBoolean("change-prefix-suffix-in-tablist.use-essentials-nickname")) {
				User user = JavaPlugin.getPlugin(Essentials.class).getUser(p);
				if (user.getNickname() != null) {
					p.setPlayerListName(prefix + user.getNickname() + suffix);
					return;
				}
			}

			setPlayerTeam(p, prefix, suffix, team.getFullTeamName());
		} else if (plugin.getChangeType().equals("namer")) {
			String result = "";

			String userName = p.getName();
			if (plugin.isPluginEnabled("Essentials")
					&& plugin.getC().getBoolean("change-prefix-suffix-in-tablist.use-essentials-nickname")) {
				User user = JavaPlugin.getPlugin(Essentials.class).getUser(p);
				if (user.getNickname() != null) {
					userName = user.getNickname();
				}
			}

			if (plugin.getC().getBoolean(phPath + "enable")) {
				if (plugin.isAfk(p, false)) {
					result = colorMsg(
							rightLeft ? prefix + userName + suffix + plugin.getC().getString(phPath + "format-yes", "")
									: plugin.getC().getString(phPath + "format-yes", "") + prefix + userName + suffix);
				} else {
					prefix = colorMsg(rightLeft ? prefix + plugin.getC().getString(phPath + "format-no", "")
							: plugin.getC().getString(phPath + "format-no", "") + prefix);
					suffix = colorMsg(rightLeft ? suffix + plugin.getC().getString(phPath + "format-no", "")
							: plugin.getC().getString(phPath + "format-no", "") + suffix);
				}
			}

			if (result.isEmpty()) {
				result = prefix + userName + suffix;
			}

			if (!result.isEmpty()) {
				p.setPlayerListName(result);
			}
		}
	}

	private void setPlayerTeam(Player player, String prefix, String suffix, String name) {
		Scoreboard tboard = player.getScoreboard();
		Team team = tboard.getTeam(name);
		if (team == null) {
			team = tboard.registerNewTeam(name);
		}

		prefix = Util.splitStringByVersion(prefix);
		suffix = Util.splitStringByVersion(suffix);

		team.setPrefix(prefix);
		team.setSuffix(suffix);

		if (Version.isCurrentEqualOrHigher(Version.v1_13_R1)) {
			team.setColor(Util.fromPrefix(prefix));
		}

		if (Version.isCurrentLower(Version.v1_9_R1)) {
			if (!team.hasPlayer(player)) {
				team.addPlayer(player);
			}
		} else if (!team.hasEntry(player.getName())) {
			team.addEntry(player.getName());
		}

		player.setScoreboard(tboard);
	}

	public void removeGroupsFromAll() {
		Bukkit.getOnlinePlayers().forEach(this::removeGroup);
		groupsList.clear();
	}

	public void removeGroup(Player p) {
		if (p == null) {
			return;
		}

		if (plugin.getChangeType().equals("namer")
				|| plugin.getC().getBoolean("change-prefix-suffix-in-tablist.use-essentials-nickname")) {
			p.setPlayerListName(p.getName());
		}

		if (plugin.getChangeType().equals("scoreboard")) {
			for (Iterator<TeamHandler> it = groupsList.iterator(); it.hasNext();) {
				TeamHandler th = it.next();
				if (th == null) {
					continue;
				}

				Scoreboard tboard = p.getScoreboard();
				Team team = tboard.getTeam(th.getFullTeamName());
				if (team == null) {
					continue;
				}

				if (Version.isCurrentLower(Version.v1_9_R1)) {
					if (team.hasPlayer(p)) {
						team.removePlayer(p);
					}
				} else if (team.hasEntry(p.getName())) {
					team.removeEntry(p.getName());
				}

				// do not unregister team to prevent removing teams from others
				/*
				 * team.setPrefix(""); team.setSuffix(""); try { team.unregister(); } catch
				 * (IllegalStateException e) { }
				 */

				p.setScoreboard(tboard);
			}
		}
	}

	public void cancelUpdate() {
		if (simpleTask != -1) {
			Bukkit.getServer().getScheduler().cancelTask(simpleTask);
			simpleTask = -1;
		}

		if (animationTask != null) {
			animationTask.cancel();
			animationTask = null;
		}

		removeGroupsFromAll();
	}

	private void startTask(Player p) {
		final int refreshInt = plugin.getC().getInt("change-prefix-suffix-in-tablist.refresh-interval");

		if (refreshInt < 1) {
			if (p != null) {
				setGroup(p);
			} else {
				for (final Player pla : Bukkit.getOnlinePlayers()) {
					setGroup(pla);
				}
			}

			return;
		}

		if (plugin.getC().getBoolean("change-prefix-suffix-in-tablist.enable-animation")) {
			if (animationTask == null) {
				animationTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
					if (Bukkit.getOnlinePlayers().isEmpty()) {
						animationTask.cancel();
						animationTask = null;
						return;
					}

					for (Player pl : Bukkit.getOnlinePlayers()) {
						setGroup(pl);
					}
				}, refreshInt, refreshInt);
			}
		} else {
			if (simpleTask == -1) {
				simpleTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
					@Override
					public void run() {
						if (Bukkit.getOnlinePlayers().isEmpty()) {
							Bukkit.getServer().getScheduler().cancelTask(simpleTask);
							simpleTask = -1;
							return;
						}

						for (Player pl : Bukkit.getOnlinePlayers()) {
							setGroup(pl);
						}
					}
				}, 0L, refreshInt * 20L);
			}
		}
	}

	private boolean isPlayerCanSeeGroup(Player p) {
		String path = "change-prefix-suffix-in-tablist.";
		if (plugin.getC().getBoolean(path + "disabled-worlds.use-as-whitelist", false)) {
			if (!plugin.getC().getStringList(path + "disabled-worlds.list").contains(p.getWorld().getName())) {
				return false;
			}
		} else {
			if (plugin.getC().getStringList(path + "disabled-worlds.list").contains(p.getWorld().getName())) {
				return false;
			}
		}

		if (plugin.isHookPreventTask(p)) {
			return false;
		}

		if (plugin.getC().getBoolean(path + "hide-group-when-player-vanished") && plugin.isVanished(p, false)) {
			removeGroup(p);
			return false;
		}

		if (plugin.getC().getBoolean(path + "hide-group-when-player-afk") && plugin.isAfk(p, false)) {
			removeGroup(p);
			return false;
		}

		if (plugin.getC().getBoolean(path + "use-displayname")) {
			p.setPlayerListName(p.getDisplayName());
			return false;
		}

		return true;
	}
}
