package hu.montlikadani.tablist.bukkit.tablist.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.task.Tasks;

public class Groups {

	private TabList plugin;

	private BukkitTask animationTask;

	private final List<TeamHandler> groupsList = new ArrayList<>();
	private final java.util.Deque<GroupPlayer> sortedPlayers = new java.util.concurrent.ConcurrentLinkedDeque<>();

	public Groups(TabList plugin) {
		this.plugin = plugin;
	}

	public BukkitTask getTask() {
		return animationTask;
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

				// This again for lazy peoples
				ChatColor[] colors = ChatColor.values();
				ChatColor c = colors[ThreadLocalRandom.current().nextInt(colors.length)];

				plugin.getConf().getGroups().set("groups." + s + ".prefix", "&" + c.getChar() + s + "&r - ");
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
	 * Sets the player prefix, suffix and tab name except if the player is hidden.
	 * 
	 * @param groupPlayer {@link GroupPlayer}
	 * @param priority
	 */
	public void setPlayerTeam(GroupPlayer groupPlayer, int priority) {
		if (groupPlayer != null && !groupPlayer.getUser().isHidden()) {
			groupPlayer.getTabTeam().setTeam(groupPlayer.getFullGroupTeamName());
		}
	}

	/**
	 * Adds a new player to groups.
	 * <p>
	 * After adding/or the player existing, their groups will get updated once to
	 * retrieve the approximately group and sets the prefix/suffix to be shown in
	 * player list. See {@link #setPlayerTeam(GroupPlayer, int)}
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
		addToTabListPlayerList(groupPlayer);

		int priority = 0;
		for (GroupPlayer gp : sortedPlayers) {
			setPlayerTeam(gp, priority);
			priority++;
		}

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
		groupPlayer.getTabTeam().unregisterTeam(groupPlayer.getFullGroupTeamName());
		groupPlayer.removeGroup();

		sortedPlayers.remove(groupPlayer);
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
				addToTabListPlayerList(gp);
			}
		}

		int priority = 0;
		for (GroupPlayer groupPlayer : sortedPlayers) {
			setPlayerTeam(groupPlayer, priority);
			priority++;
		}
	}

	private void addToTabListPlayerList(GroupPlayer tlp) {
		int pos = 0;

		for (GroupPlayer groupPlayer : sortedPlayers) {
			if (tlp.compareTo(groupPlayer) < 0)
				break;

			pos++;
		}

		if (pos > 0) {
			sortedPlayers.offerFirst(tlp);
		} else {
			sortedPlayers.offerLast(tlp);
		}
	}
}
