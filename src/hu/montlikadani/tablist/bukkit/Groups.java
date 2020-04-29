package hu.montlikadani.tablist.bukkit;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.earth2me.essentials.Essentials;

import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;
import ru.tehkode.permissions.bukkit.PermissionsEx;

@SuppressWarnings("deprecation")
public class Groups {

	private TabList plugin;

	private BukkitTask animationTask = null;
	private Integer simpleTask = -1;

	private final List<TeamHandler> groupsList = new ArrayList<>();

	private final Scoreboard b = Bukkit.getScoreboardManager().getNewScoreboard();

	public Groups(TabList plugin) {
		this.plugin = plugin;
	}

	public List<TeamHandler> getGroupsList() {
		return groupsList;
	}

	public TeamHandler getTeam(String name) {
		Validate.notNull(name, "The team name can't be null!");
		Validate.notEmpty(name, "The team name can't be empty!");

		for (TeamHandler handler : groupsList) {
			if (handler.getTeam().equalsIgnoreCase(name)) {
				return handler;
			}
		}

		return null;
	}

	protected void load() {
		groupsList.clear();

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
			int last = 0;

			for (String g : plugin.getGS().getConfigurationSection("groups").getKeys(false)) {
				if (g.equalsIgnoreCase("exampleGroup")) {
					continue;
				}

				String path = "groups." + g + ".";

				String prefix = plugin.getGS().getString(path + "prefix", "");
				String suffix = plugin.getGS().getString(path + "suffix", "");
				String perm = plugin.getGS().getString(path + "permission", "");
				int priority = plugin.getGS().getInt(path + "sort-priority", last + 1);

				groupsList.add(new TeamHandler(g, prefix, suffix, perm, priority));

				last = priority;
			}
		}

		startTask(null);
	}

	public void loadGroupForPlayer(final Player p) {
		removePlayerGroup(p);

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
				return;
			}

			boolean change = false;
			if (plugin.isPluginEnabled("Vault") && plugin.getVaultPerm().playerInGroup(p, name)) {
				change = true;
			}

			if (!team.getPermission().isEmpty()) {
				if (plugin.isPluginEnabled("PermissionsEx")) {
					if (PermissionsEx.getPermissionManager().has(p, team.getPermission())) {
						change = true;
					}
				} else if (p.hasPermission(team.getPermission())) {
					change = true;
				}
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

		String pName = p.getName();
		String teamName = team.getTeam();

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
			String nick = JavaPlugin.getPlugin(Essentials.class).getUser(p).getNickname();
			if (nick != null) {
				teamName = nick;
				pName = nick;
			}
		}

		setPlayerTeam(p, prefix, suffix, (1000 + team.getPriority()) + teamName, pName);
	}

	public void setPlayerTeam(Player player, String prefix, String suffix, String name) {
		setPlayerTeam(player, prefix, suffix, name, player.getName());
	}

	public void setPlayerTeam(Player player, String prefix, String suffix, String name, String playerName) {
		if (name.length() > 16) {
			name = name.substring(0, 16);
		}

		Team team = b.getTeam(name);
		if (team == null) {
			team = b.registerNewTeam(name);
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

		player.setPlayerListName(prefix + playerName + suffix);

		player.setScoreboard(b);
	}

	public void removeGroupsFromAll() {
		Bukkit.getOnlinePlayers().forEach(this::removePlayerGroup);
	}

	public void removePlayerGroup(Player p) {
		if (p == null) {
			return;
		}

		p.setPlayerListName(p.getName());

		for (TeamHandler th : groupsList) {
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

			// team.unregister();

			p.setScoreboard(tboard);
		}
	}

	public void removeGroup(String teamName) {
		TeamHandler th = null;

		// We using "simply for loop" because Iterator breaks in some cases
		for (TeamHandler team : groupsList) {
			// Use contains because of priority numbers
			if (team.getTeam().contains(teamName)) {
				th = team;
				break;
			}
		}

		if (th != null) {
			groupsList.remove(th);
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
			removePlayerGroup(p);
			return false;
		}

		if (plugin.getC().getBoolean(path + "hide-group-when-player-afk") && plugin.isAfk(p, false)) {
			removePlayerGroup(p);
			return false;
		}

		return true;
	}
}
