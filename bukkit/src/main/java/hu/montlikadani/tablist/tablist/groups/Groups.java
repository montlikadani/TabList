package hu.montlikadani.tablist.tablist.groups;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.tablist.TabText;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.PluginUtils;
import hu.montlikadani.tablist.utils.task.Tasks;

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
	 * @throws NullPointerException     if the specified name is null
	 * @throws IllegalArgumentException if the specified name is empty
	 */
	public Optional<TeamHandler> getTeam(String name) {
		if (name.isEmpty()) {
			throw new IllegalArgumentException("The team name can not be empty");
		}

		for (TeamHandler handler : groupsList) {
			if (handler.team.equalsIgnoreCase(name)) {
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

		org.bukkit.configuration.file.FileConfiguration gr = plugin.getConf().getGroups();

		String globPrefix = gr.getString("globalGroup.prefix", "");
		String globSuffix = gr.getString("globalGroup.suffix", "");
		String globTabName = gr.getString("globalGroup.tabname", "");

		if (!globTabName.isEmpty() || !globPrefix.isEmpty() || !globSuffix.isEmpty()) {
			TeamHandler team = new TeamHandler("global", TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(globPrefix)),
					TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(globSuffix)));

			team.global = true;
			team.tabName = TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(globTabName));

			groupsList.add(team);
		}

		ConfigurationSection cs = gr.getConfigurationSection("groups");
		if (cs == null) {
			if (!groupsList.isEmpty()) {
				startTask();
			}

			return;
		}

		Set<String> keys = cs.getKeys(false);

		if (keys.isEmpty()) {
			if (!groupsList.isEmpty()) {
				startTask();
			}

			return;
		}

		// Automatically add existing groups to the list for "lazy peoples"
		if (ConfigValues.isSyncPluginsGroups() && plugin.hasVault()) {
			boolean have = false;
			ChatColor[] colors = ChatColor.values();
			java.util.Random random = new java.util.Random();

			me: for (String s : plugin.getVaultPerm().getGroups()) {
				for (String g : keys) {
					if (s.equalsIgnoreCase(g)) {
						continue me;
					}
				}

				// This again for lazy peoples
				cs.set(s + ".prefix", "&" + colors[random.nextInt(colors.length)].getChar() + s + "&r - ");
				have = true;
			}

			if (have) {
				try {
					gr.save(plugin.getConf().getGroupsFile());
				} catch (java.io.IOException e) {
					e.printStackTrace();
				}
			}
		}

		int last = 0;

		for (String g : keys) {
			if (g.equals("exampleGroup") || g.equals("PlayerName")) {
				continue;
			}

			TabText prefix = TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(cs.getString(g + ".prefix", "")));
			TabText suffix = TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(cs.getString(g + ".suffix", "")));

			TeamHandler th = new TeamHandler(g, prefix, suffix, cs.getString(g + ".permission", ""),
					last = cs.getInt(g + ".sort-priority", last + 1));

			th.setAfkSortPriority(cs.getInt(g + ".afk-sort-priority", -1));
			th.tabName = TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(cs.getString(g + ".tabname", "")));

			groupsList.add(th);
		}

		// Sort groups by priority to match the lowest priority firstly (highest
		// priority is on the top of other)
		List<TeamHandler> newSortedList = groupsList.stream()
				.sorted(Comparator.<TeamHandler>comparingInt(t -> t.priority).reversed()).collect(Collectors.toList());
		groupsList.clear();
		groupsList.addAll(newSortedList);

		startTask();
	}

	/**
	 * Sets the player prefix, suffix, tab name, and position on tablist
	 * 
	 * @param groupPlayer  {@link GroupPlayer}
	 * @param safePriority Safe priority value. The value should be between 0 and 98.
	 */
	public void setPlayerTeam(GroupPlayer groupPlayer, int safePriority) {
		groupPlayer.setSafePriority(safePriority);
		groupPlayer.getTabTeam().setTeam(groupPlayer);
	}

	/**
	 * Adds a new player to groups.
	 * <p>
	 * After adding/or the player existing, their groups will get updated once to retrieve the approximately group and
	 * sets the prefix/suffix to be shown in player list.
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
		sortPlayers();

		return groupPlayer;
	}

	/**
	 * Removes the given player's group.
	 * 
	 * @param user {@link TabListUser}
	 */
	public void removePlayerGroup(TabListUser user) {
		GroupPlayer groupPlayer = user.getGroupPlayer();

		sortedPlayers.remove(groupPlayer);
		afkPlayersCache.remove(groupPlayer);

		// Is this even required?
		/*if (!sortedPlayers.isEmpty()) {
			sortPlayers();
		}*/

		groupPlayer.getTabTeam().unregisterTeam(groupPlayer);
		groupPlayer.removeGroup();
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

		sortedPlayers.clear();
		afkPlayersCache.clear();

		for (TabListUser user : plugin.getUsers()) {
			GroupPlayer groupPlayer = user.getGroupPlayer();

			groupPlayer.getTabTeam().unregisterTeam(groupPlayer);
			groupPlayer.removeGroup();
		}
	}

	public void cancelTask() {
		if (animationTask != null) {
			animationTask.cancel();
			animationTask = null;
		}
	}

	public void startTask() {
		if (!ConfigValues.isPrefixSuffixEnabled()) {
			return;
		}

		final int refreshInt = ConfigValues.getGroupsRefreshInterval();
		if (refreshInt < 1) {
			updatePlayers();
			return;
		}

		if (animationTask == null) {
			animationTask = Tasks.submitAsync(() -> {
				if (plugin.performanceIsUnderValue() || plugin.getUsers().isEmpty()) {
					cancelTask();
					return;
				}

				// Skip method execution to sort players until the current thread is locked
				if (!lock.isLocked()) {
					updatePlayers();
				}
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

		sortPlayers();
	}

	private final ReentrantLock lock = new ReentrantLock();

	/**
	 * This method is used to sort and update players' groups. Includes sorting of AFK players. If there is no need to
	 * change the places of groups, they will only be updated in the custom name, prefix, suffix and others.
	 */
	private void sortPlayers() {
		// Pauses the current thread until the stream unlocks.
		//
		// This was implemented to fix a less reproducible and undetectable exception.
		// This lock was needed for the stream collect, so it pauses the current thread
		// until the stream ends and unlocks the thread.
		// Without this lock some of data will be lost.
		lock.lock();

		List<GroupPlayer> playerGroups;
		try {
			playerGroups = sortedPlayers.stream().sorted(Comparator.comparingInt(GroupPlayer::getPriority))
					.collect(Collectors.toList());
		} finally {
			lock.unlock();
		}

		sortedPlayers.clear();
		sortedPlayers.addAll(playerGroups);

		int priority = playerGroups.size();

		for (GroupPlayer groupPlayer : sortedPlayers) {
			if (ConfigValues.isAfkStatusEnabled() && (ConfigValues.isAfkSortLast()
					|| (groupPlayer.getGroup() != null && groupPlayer.getGroup().getAfkSortPriority() != -1))) {
				if (PluginUtils.isAfk(groupPlayer.getUser().getPlayer())) {
					if (afkPlayersCache.add(groupPlayer) && !toSort) {
						setToSort(true);
					}

					continue;
				}

				if (afkPlayersCache.remove(groupPlayer) && !toSort) {
					setToSort(true);
				}
			}

			setPlayerTeam(groupPlayer, priority--);
		}

		if (!afkPlayersCache.isEmpty()) {
			int size = playerGroups.size();

			for (GroupPlayer afk : afkPlayersCache) {
				setPlayerTeam(afk, (afk.getGroup() == null || afk.getGroup().getAfkSortPriority() == -1) ? size++
						: afk.getGroup().getAfkSortPriority());
			}
		}

		if (toSort) {
			setToSort(false);
		}
	}
}
