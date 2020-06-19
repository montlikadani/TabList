package hu.montlikadani.tablist.bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import hu.montlikadani.tablist.bukkit.utils.NMS;

public class Groups {

	private TabList plugin;

	private BukkitTask animationTask = null;
	private Integer simpleTask = -1;

	private final List<TeamHandler> groupsList = new ArrayList<>();
	private final HashMap<String, TabListPlayer> tLPlayerMap = new HashMap<>();
	private final LinkedList<TabListPlayer> sortedTabListPlayers = new LinkedList<>();

	private final Scoreboard b = Bukkit.getScoreboardManager().getNewScoreboard();

	public Groups(TabList plugin) {
		this.plugin = plugin;
	}

	public HashMap<String, TabListPlayer> getTLPlayerMap() {
		return tLPlayerMap;
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

		if (!ConfigValues.isPrefixSuffixEnabled()) {
			return;
		}

		plugin.getConf().createGroupsFile();

		// Automatically add existing groups to the list for "lazy peoples"
		if (ConfigValues.isSyncPluginsGroups() && plugin.isPluginEnabled("Vault")) {
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

				String prefix = plugin.getGS().getString(path + "prefix", ""),
						suffix = plugin.getGS().getString(path + "suffix", ""),
						perm = plugin.getGS().getString(path + "permission", "");
				int priority = plugin.getGS().getInt(path + "sort-priority", last + 1);

				groupsList.add(new TeamHandler(g, prefix, suffix, perm, priority));

				last = priority;
			}
		}

		startTask();
	}

	public void loadGroupForPlayer(final Player p) {
		removePlayerGroup(p);
		startTask();
	}

	public void setPlayerTeam(Player player, String prefix, String suffix, String name) {
		setPlayerTeam(player, prefix, suffix, name, player.getName());
	}

	public void setPlayerTeam(Player player, String prefix, String suffix, String name, String playerName) {
		if (name.length() > 16) {
			name = name.substring(0, 16);
		}

		Scoreboard tboard = ConfigValues.isUseOwnScoreboard() ? player.getScoreboard() : b;
		Team team = tboard.getTeam(name);
		if (team == null) {
			team = tboard.registerNewTeam(name);
		}

		NMS.addEntry(player, team);

		player.setPlayerListName(prefix + playerName + suffix);
		player.setScoreboard(tboard);
	}

	public TabListPlayer addPlayer(Player player) {
		String uuid = player.getUniqueId().toString();

		if (tLPlayerMap.containsKey(uuid)) {
			return tLPlayerMap.get(uuid);
		}

		TabListPlayer tabPlayer = new TabListPlayer(plugin, player);
		tLPlayerMap.put(uuid, tabPlayer);
		tabPlayer.update();
		addToTabListPlayerList(tabPlayer);

		int priority = 0;
		for (TabListPlayer tlp : sortedTabListPlayers) {
			String group = Integer.toString(100000 + priority)
					+ (tlp.getGroup() == null ? tlp.getPlayer().getName() : tlp.getGroup().getTeam());
			setPlayerTeam(tlp.getPlayer(), tlp.getPrefix(), tlp.getSuffix(), group, tlp.getPlayerName());
			priority++;
		}

		return tabPlayer;
	}

	public void removeGroupsFromAll() {
		Bukkit.getOnlinePlayers().forEach(this::removePlayerGroup);
	}

	public void removePlayerGroup(Player p) {
		if (p == null) {
			return;
		}

		String uuid = p.getUniqueId().toString();

		if (!tLPlayerMap.containsKey(uuid))
			return;

		p.setPlayerListName(p.getName());

		TabListPlayer tlp = tLPlayerMap.remove(uuid);
		if (tlp != null) {
			tlp.removeGroup();
			sortedTabListPlayers.removeFirstOccurrence(tlp);
		}

		Scoreboard tboard = p.getScoreboard();
		NMS.removeEntry(p, tboard);

		// team.unregister();

		p.setScoreboard(tboard);
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

	private void startTask() {
		if (!ConfigValues.isPrefixSuffixEnabled()) {
			return;
		}

		final int refreshInt = ConfigValues.getGroupsRefreshInterval();

		if (refreshInt < 1) {
			updatePlayers();
			return;
		}

		if (ConfigValues.isGroupAnimationEnabled()) {
			if (animationTask == null) {
				animationTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
					if (Bukkit.getOnlinePlayers().isEmpty()) {
						animationTask.cancel();
						animationTask = null;
						return;
					}

					updatePlayers();
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

						updatePlayers();
					}
				}, 0L, refreshInt * 20L);
			}
		}
	}

	private void updatePlayers() {
		for (Player pl : Bukkit.getOnlinePlayers()) {
			TabListPlayer tlp = tLPlayerMap.get(pl.getUniqueId().toString());
			if (tlp == null) {
				tlp = new TabListPlayer(plugin, pl);

				tLPlayerMap.put(pl.getUniqueId().toString(), tlp);

				tlp.update();
				addToTabListPlayerList(tlp);
			} else if (tlp.update()) {
				sortedTabListPlayers.removeFirstOccurrence(tlp);
				addToTabListPlayerList(tlp);
			}
		}

		int priority = 0;
		for (TabListPlayer tlp : sortedTabListPlayers) {
			String group = Integer.toString(100000 + priority)
					+ (tlp.getGroup() == null ? tlp.getPlayer().getName() : tlp.getGroup().getTeam());
			setPlayerTeam(tlp.getPlayer(), tlp.getPrefix(), tlp.getSuffix(), group, tlp.getPlayerName());
			priority++;
		}
	}

	private void addToTabListPlayerList(TabListPlayer tlp) {
		int pos = 0;

		for (TabListPlayer p : sortedTabListPlayers) {
			if (tlp.compareTo(p) < 0)
				break;

			pos++;
		}

		sortedTabListPlayers.add(pos, tlp);
	}
}
