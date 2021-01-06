package hu.montlikadani.tablist.bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import hu.montlikadani.tablist.bukkit.config.ConfigValues;

public class Groups {

	private TabList plugin;

	private BukkitTask animationTask;

	private final List<TeamHandler> groupsList = new ArrayList<>();
	private final HashMap<String, TabListPlayer> tLPlayerMap = new HashMap<>();
	private final ConcurrentLinkedDeque<TabListPlayer> sortedTabListPlayers = new ConcurrentLinkedDeque<>();

	//private final Scoreboard b = Bukkit.getScoreboardManager().getNewScoreboard();

	public Groups(TabList plugin) {
		this.plugin = plugin;
	}

	public HashMap<String, TabListPlayer> getTLPlayerMap() {
		return tLPlayerMap;
	}

	/**
	 * Returns the list of teams
	 * 
	 * @return {@link List}
	 */
	public List<TeamHandler> getGroupsList() {
		return groupsList;
	}

	/**
	 * Returns the team by name
	 * 
	 * @param name Team name
	 * @return {@link TeamHandler} if present, otherwise {@link Optional#empty()}
	 */
	public Optional<TeamHandler> getTeam(String name) {
		Validate.notEmpty(name, "The team name can't be empty/null");

		for (TeamHandler handler : groupsList) {
			if (handler.getTeam().equalsIgnoreCase(name)) {
				return Optional.of(handler);
			}
		}

		return Optional.empty();
	}

	protected void load() {
		groupsList.clear();

		if (!ConfigValues.isPrefixSuffixEnabled()) {
			return;
		}

		plugin.getConf().createGroupsFile();

		String globPrefix = plugin.getConf().getGroups().getString("globalGroup.prefix", ""),
				globSuffix = plugin.getConf().getGroups().getString("globalGroup.suffix", "");
		if (!globPrefix.isEmpty() || !globSuffix.isEmpty()) {
			String customTabName = plugin.getConf().getGroups().getString("globalGroup.tabname", "");
			TeamHandler team = new TeamHandler("global", globPrefix, globSuffix);
			team.setGlobal(true);
			team.setTabName(customTabName);
			groupsList.add(team);
		}

		if (!plugin.getConf().getGroups().isConfigurationSection("groups")) {
			if (!groupsList.isEmpty()) {
				startTask();
			}

			return;
		}

		// Automatically add existing groups to the list for "lazy peoples"
		if (ConfigValues.isSyncPluginsGroups() && plugin.hasVault()) {
			boolean have = false;

			me: for (String s : plugin.getVaultPerm().getGroups()) {
				for (String g : plugin.getConf().getGroups().getConfigurationSection("groups").getKeys(false)) {
					if (s.equalsIgnoreCase(g)) {
						continue me;
					}
				}

				String path = "groups." + s + ".";

				// This again for lazy peoples
				ChatColor[] colors = ChatColor.values();
				ChatColor c = colors[ThreadLocalRandom.current().nextInt(colors.length)];

				plugin.getConf().getGroups().set(path + "prefix", "&" + c.getChar() + s + "&r - ");
				have = true;
			}

			if (have) {
				try {
					plugin.getConf().getGroups().save(plugin.getConf().getGroupsFile());
				} catch (java.io.IOException e) {
					e.printStackTrace();
				}
			}
		}

		ConfigurationSection cs = plugin.getConf().getGroups().getConfigurationSection("groups");
		int last = 0;
		for (String g : cs.getKeys(false)) {
			if (g.equalsIgnoreCase("exampleGroup")) {
				continue;
			}

			String path = g + ".", prefix = cs.getString(path + "prefix", ""),
					suffix = cs.getString(path + "suffix", ""),
					tabName = cs.getString(path + "tabname", ""),
					perm = cs.getString(path + "permission", "");
			int priority = cs.getInt(path + "sort-priority", last + 1);

			TeamHandler th = new TeamHandler(g, prefix, suffix, perm, priority);
			th.setTabName(tabName);
			groupsList.add(th);

			last = priority;
		}

		startTask();
	}

	/**
	 * Loads the group(s) for the given player. If the player have before group set,
	 * that group get removing. After remove the scheduler task will get started to
	 * update repeatedly.
	 * 
	 * @param p {@link Player}
	 */
	public void loadGroupForPlayer(Player p) {
		removePlayerGroup(p);
		startTask();
	}

	/**
	 * Sets the player prefix/suffix
	 * <p>
	 * If the player is hidden using {@link TabList#getHidePlayers()}, returned.
	 * 
	 * @param tabPlayer {@link TabListPlayer}
	 * @param priority
	 */
	public void setPlayerTeam(TabListPlayer tabPlayer, int priority) {
		if (tabPlayer == null) {
			return;
		}

		Player player = tabPlayer.getPlayer();
		if (plugin.getHidePlayers().containsKey(player)) {
			return;
		}

		/*Scoreboard tboard = ConfigValues.isUseOwnScoreboard() ? player.getScoreboard() : b;
		if (tabPlayer.getTabTeam().getScoreboard() != tboard) {
			tabPlayer.getTabTeam().setScoreboard(tboard);
		}*/

		tabPlayer.getTabTeam().setTeam(tabPlayer.getFullGroupTeamName());
	}

	/**
	 * Adds a new player to groups.
	 * <p>
	 * After adding/or the player existing, their groups will get updated once to
	 * retrieve the approximately group and sets the prefix/suffix to be shown in
	 * player list. see {@link #setPlayerTeam(TabListPlayer, int)}
	 * 
	 * @param player {@link Player}
	 * @return {@link TabListPlayer} if ever exists or not
	 */
	public TabListPlayer addPlayer(Player player) {
		String uuid = player.getUniqueId().toString();

		TabListPlayer tabPlayer = tLPlayerMap.get(uuid);
		if (tabPlayer == null) {
			tabPlayer = new TabListPlayer(plugin, player);
			tLPlayerMap.put(uuid, tabPlayer);
		}

		tabPlayer.update();
		addToTabListPlayerList(tabPlayer);

		int priority = 0;
		for (TabListPlayer tlp : sortedTabListPlayers) {
			setPlayerTeam(tlp, priority);
			priority++;
		}

		return tabPlayer;
	}

	/**
	 * Removes all groups from every online players.
	 */
	public void removeGroupsFromAll() {
		Bukkit.getOnlinePlayers().forEach(this::removePlayerGroup);
	}

	/**
	 * Removes the given player's group.
	 * 
	 * @param p {@link Player}
	 */
	public void removePlayerGroup(Player p) {
		if (p == null) {
			return;
		}

		TabListPlayer tlp = tLPlayerMap.remove(p.getUniqueId().toString());
		if (tlp == null) {
			return;
		}

		tlp.getTabTeam().unregisterTeam(tlp.getFullGroupTeamName());
		tlp.removeGroup();

		sortedTabListPlayers.remove(tlp);
		return;
	}

	/**
	 * Removes the given group by name.
	 * 
	 * @param teamName
	 */
	public void removeGroup(String teamName) {
		getTeam(teamName).ifPresent(groupsList::remove);
	}

	/**
	 * Cancels the current running task of groups and removes from players.
	 */
	public void cancelUpdate() {
		if (animationTask != null) {
			animationTask.cancel();
			animationTask = null;
		}

		removeGroupsFromAll();
	}

	private void startTask() {
		if (!ConfigValues.isPrefixSuffixEnabled() || Bukkit.getOnlinePlayers().isEmpty()) {
			return;
		}

		final int refreshInt = ConfigValues.getGroupsRefreshInterval();
		if (refreshInt < 1) {
			updatePlayers();
			return;
		}

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
				sortedTabListPlayers.remove(tlp);
				addToTabListPlayerList(tlp);
			}
		}

		int priority = 0;
		for (TabListPlayer tabPlayer : sortedTabListPlayers) {
			setPlayerTeam(tabPlayer, priority);
			priority++;
		}
	}

	private void addToTabListPlayerList(TabListPlayer tlp) {
		int pos = 0;

		for (TabListPlayer tabPlayer : sortedTabListPlayers) {
			if (tlp.compareTo(tabPlayer) < 0)
				break;

			pos++;
		}

		if (pos > 0) {
			sortedTabListPlayers.offerFirst(tlp);
		} else {
			sortedTabListPlayers.offerLast(tlp);
		}
	}
}
