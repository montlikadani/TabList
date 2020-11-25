package hu.montlikadani.tablist.bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import hu.montlikadani.tablist.bukkit.config.ConfigValues;

public class Groups {

	private TabList plugin;

	private BukkitTask animationTask;

	private final List<TeamHandler> groupsList = new ArrayList<>();
	private final HashMap<String, TabListPlayer> tLPlayerMap = new HashMap<>();
	private final List<TabListPlayer> sortedTabListPlayers = Collections
			.synchronizedList(new LinkedList<TabListPlayer>());

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
	 * Returns the team by name.
	 * 
	 * @param name Team name
	 * @return {@link Optional} -> {@link TeamHandler}
	 */
	public Optional<TeamHandler> getTeam(String name) {
		Validate.notEmpty(name, "The team name can't be empty/null");

		for (TeamHandler handler : groupsList) {
			if (handler.getTeam().equalsIgnoreCase(name)) {
				return Optional.ofNullable(handler);
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

		String globPrefix = plugin.getConf().getGroups().getString("globalGroup.prefix", "");
		String globSuffix = plugin.getConf().getGroups().getString("globalGroup.suffix", "");
		if (!globPrefix.isEmpty() || !globSuffix.isEmpty()) {
			TeamHandler team = new TeamHandler("global", globPrefix, globSuffix);
			team.setGlobal(true);
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

		int last = 0;
		for (String g : plugin.getConf().getGroups().getConfigurationSection("groups").getKeys(false)) {
			if (g.equalsIgnoreCase("exampleGroup")) {
				continue;
			}

			String path = "groups." + g + ".", prefix = plugin.getConf().getGroups().getString(path + "prefix", ""),
					suffix = plugin.getConf().getGroups().getString(path + "suffix", ""),
					perm = plugin.getConf().getGroups().getString(path + "permission", "");
			int priority = plugin.getConf().getGroups().getInt(path + "sort-priority", last + 1);

			groupsList.add(new TeamHandler(g, prefix, suffix, perm, priority));

			last = priority;
		}

		startTask();
	}

	/**
	 * Loads the group(s) for the given player. If the player have before group set, that group getting remove.
	 * The scheduler task will get started to update continuously.
	 * 
	 * @param p {@link Player}
	 */
	public void loadGroupForPlayer(Player p) {
		removePlayerGroup(p).thenAccept(e -> startTask());
	}

	/**
	 * Sets the player prefix/suffix<br><br>
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

		String name = Integer.toString(100000 + priority)
				+ (tabPlayer.getGroup() == null ? player.getName() : tabPlayer.getGroup().getTeam());
		if (name.length() > 16) {
			name = name.substring(0, 16);
		}

		/*Scoreboard tboard = ConfigValues.isUseOwnScoreboard() ? player.getScoreboard() : b;
		if (tabPlayer.getTabTeam().getScoreboard() != tboard) {
			tabPlayer.getTabTeam().setScoreboard(tboard);
		}*/

		tabPlayer.getTabTeam().setTeam(name);
	}

	/**
	 * Adds a new player to groups.<br>
	 * If the given player is not exists in the list it will be instantiated to a new one and
	 * making this <code>synchronized</code> to not cause {@link ConcurrentModificationException}.
	 * <br><br>
	 * After adding/or the player existing, their groups will get updated once to retrieve the
	 * approximately group and sets the prefix/suffix to be shown in player list. see {@link #setPlayerTeam(TabListPlayer, int)}
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

		synchronized (sortedTabListPlayers) {
			int priority = 0;
			Iterator<TabListPlayer> it = sortedTabListPlayers.iterator();
			while (it.hasNext()) {
				setPlayerTeam(it.next(), priority);
				priority++;
			}
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
	 * Removes the given player's group.<br><br>
	 * If the player does not have any groups this future will completes and returns false.
	 * Otherwise returns true if the player have any groups and its being removed from list.
	 * 
	 * @param p {@link Player}
	 * @return {@link CompletableFuture}
	 */
	public CompletableFuture<Boolean> removePlayerGroup(Player p) {
		CompletableFuture<Boolean> result = new CompletableFuture<>();
		if (p == null) {
			result.complete(false);
			return result;
		}

		TabListPlayer tlp = tLPlayerMap.remove(p.getUniqueId().toString());
		if (tlp == null) {
			result.complete(false);
			return result;
		}

		String name = Integer.toString(100000 + tlp.getPriority())
				+ (tlp.getGroup() == null ? p.getName() : tlp.getGroup().getTeam());
		if (name.length() > 16) {
			name = name.substring(0, 16);
		}

		tlp.getTabTeam().unregisterTeam(name);
		tlp.removeGroup();

		sortedTabListPlayers.remove(tlp);

		result.complete(true);
		return result;
	}

	/**
	 * Removes the given group by name.
	 * 
	 * @param teamName
	 */
	public void removeGroup(String teamName) {
		getTeam(teamName).ifPresent(th -> groupsList.remove(th));
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

		synchronized (sortedTabListPlayers) {
			int priority = 0;
			Iterator<TabListPlayer> it = sortedTabListPlayers.iterator();
			while (it.hasNext()) {
				setPlayerTeam(it.next(), priority);
				priority++;
			}
		}
	}

	private void addToTabListPlayerList(TabListPlayer tlp) {
		int pos = 0;

		synchronized (sortedTabListPlayers) {
			Iterator<TabListPlayer> it = sortedTabListPlayers.iterator();
			while (it.hasNext()) {
				if (tlp.compareTo(it.next()) < 0)
					break;

				pos++;
			}
		}

		sortedTabListPlayers.add(pos, tlp);
	}
}
