package hu.montlikadani.tablist.bukkit.tablist.groups;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import hu.montlikadani.tablist.bukkit.utils.PluginUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.task.Tasks;

public final class Groups {

	private final TabList plugin;

	private BukkitTask animationTask;

	private final List<TeamHandler> groupsList = new ArrayList<>();
	private final Deque<GroupPlayer> sortedPlayers = new ConcurrentLinkedDeque<>();
	private final Set<GroupPlayer> afkPlayersCache = new HashSet<>();

	private boolean toSort = true;

	public Groups(TabList plugin) {
		this.plugin = plugin;
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

	public boolean isToSort() {
		return toSort;
	}

	public void setToSort(boolean toSort) {
		this.toSort = toSort;
	}

	public void load() {
		groupsList.clear();

		if (!ConfigValues.isPrefixSuffixEnabled()) {
			return;
		}

		String globPrefix = plugin.getConf().getGroups().getString("globalGroup.prefix", ""),
				globSuffix = plugin.getConf().getGroups().getString("globalGroup.suffix", "");

		if (!globPrefix.isEmpty() || !globSuffix.isEmpty()) {
			TeamHandler team = new TeamHandler("global", globPrefix, globSuffix);
			team.setGlobal(true);
			team.setTabName(plugin.getConf().getGroups().getString("globalGroup.tabname", ""));
			groupsList.add(team);
		}

		ConfigurationSection cs = plugin.getConf().getGroups().getConfigurationSection("groups");
		if (cs == null) {
			if (!groupsList.isEmpty()) {
				startTask();
			}

			return;
		}

		// Automatically add existing groups to the list for "lazy peoples"
		if (ConfigValues.isSyncPluginsGroups() && plugin.hasVault()) {
			boolean have = false;

			me: for (String s : plugin.getVaultPerm().getGroups()) {
				for (String g : cs.getKeys(false)) {
					if (s.equalsIgnoreCase(g)) {
						continue me;
					}
				}

				// This again for lazy peoples
				ChatColor[] colors = ChatColor.values();
				ChatColor c = colors[java.util.concurrent.ThreadLocalRandom.current().nextInt(colors.length)];

				cs.set(s + ".prefix", "&" + c.getChar() + s + "&r - ");
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

		int last = 0;
		for (String g : cs.getKeys(false)) {
			if (g.equalsIgnoreCase("exampleGroup")) {
				continue;
			}

			String prefix = cs.getString(g + ".prefix", ""), suffix = cs.getString(g + ".suffix", ""),
					tabName = cs.getString(g + ".tabname", ""), perm = cs.getString(g + ".permission", "");
			int priority = cs.getInt(g + ".sort-priority", last + 1);

			TeamHandler th = new TeamHandler(g, prefix, suffix, perm, last = priority);
			th.setTabName(tabName);
			groupsList.add(th);
		}

		startTask();
	}

	/**
	 * Sets the player prefix, suffix, tab name, and position on tablist, except if
	 * the player is hidden.
	 * 
	 * @param groupPlayer  {@link GroupPlayer}
	 * @param safePriority Safe priority value. Should be between 0 and 999999999.
	 */
	public void setPlayerTeam(GroupPlayer groupPlayer, int safePriority) {
		groupPlayer.setSafePriority(safePriority);

		if (!groupPlayer.getUser().isHidden()) {
			groupPlayer.getTabTeam().setTeam(groupPlayer);
		}
	}

	/**
	 * Adds a new player to groups.
	 * <p>
	 * After adding/or the player existing, their groups will get updated once to
	 * retrieve the approximately group and sets the prefix/suffix to be shown in
	 * player list.
	 * 
	 * @param player {@link Player}
	 * @return {@link GroupPlayer} if ever exists or not
	 */
	public GroupPlayer addPlayer(Player player) {
		TabListUser user = plugin.getUser(player).orElse(null);
		if (user == null) {
			return null;
		}

		GroupPlayer groupPlayer = user.getGroupPlayer();

		groupPlayer.update();
		setToSort(true);
		sortedPlayers.add(groupPlayer);
		sortScoreboards();

		return groupPlayer;
	}

	/**
	 * Removes all groups from every online players.
	 */
	public void removeGroupsFromAll() {
		plugin.getUsers().forEach(this::removePlayerGroup);
	}

	/**
	 * Removes the given player's group.
	 * 
	 * @param user {@link TabListUser}
	 */
	public void removePlayerGroup(TabListUser user) {
		GroupPlayer groupPlayer = user.getGroupPlayer();
		groupPlayer.getTabTeam().unregisterTeam(groupPlayer);
		groupPlayer.removeGroup();

		sortedPlayers.remove(groupPlayer);
	}

	/**
	 * Removes the given group by name.
	 * 
	 * @param teamName Name of team.
	 */
	public void removeGroup(String teamName) {
		getTeam(teamName).ifPresent(groupsList::remove);
	}

	/**
	 * Cancels the current running task of groups and removes from players.
	 */
	public void cancelUpdate() {
		cancelTask();
		removeGroupsFromAll();
	}

	public void cancelTask() {
		if (animationTask != null) {
			animationTask.cancel();
			animationTask = null;
		}
	}

	public void startTask() {
		if (!ConfigValues.isPrefixSuffixEnabled() || plugin.getUsers().isEmpty()) {
			return;
		}

		final int refreshInt = ConfigValues.getGroupsRefreshInterval();
		if (refreshInt < 1) {
			updatePlayers();
			return;
		}

		if (animationTask == null) {
			animationTask = Tasks.submitAsync(() -> {
				if (plugin.getUsers().isEmpty()) {
					cancelTask();
					return;
				}

				updatePlayers();
			}, refreshInt, refreshInt);
		}
	}

	private void updatePlayers() {
		for (TabListUser user : plugin.getUsers()) {
			GroupPlayer gp = user.getGroupPlayer();

			if (gp.update()) {
				sortedPlayers.remove(gp);
				sortedPlayers.add(gp);
			}
		}

		sortScoreboards();
	}

	/**
	 * This method is used to sort and update players' scoreboards. Includes sorting
	 * of AFK players. If there is no need to change the places of groups, they will
	 * only be updated in the custom name, prefix, suffix and others.
	 */
	public void sortScoreboards() {
		// TODO Improve or get rid from streams
		Set<GroupPlayer> playerGroups = sortedPlayers.stream().sorted(Comparator.comparingInt(GroupPlayer::getPriority))
				.collect(Collectors.toCollection(java.util.LinkedHashSet::new));

		sortedPlayers.clear();
		sortedPlayers.addAll(playerGroups);

		int priority = sortedPlayers.size();

		for (GroupPlayer groupPlayer : sortedPlayers) {
			if (ConfigValues.isAfkStatusEnabled() && PluginUtils.isAfk(groupPlayer.getUser().getPlayer())) {
				if (afkPlayersCache.add(groupPlayer)) {
					setToSort(true);
				}

				continue;
			}

			if (afkPlayersCache.remove(groupPlayer)) {
				setToSort(true);
			}

			setPlayerTeam(groupPlayer, priority--);
		}

		if (ConfigValues.isHideGroupWhenAfk()) {
			setToSort(false);
			return;
		}

		for (GroupPlayer afk : afkPlayersCache) {
			setPlayerTeam(afk, sortedPlayers.size() + 1);
		}

		setToSort(false);
	}

}
