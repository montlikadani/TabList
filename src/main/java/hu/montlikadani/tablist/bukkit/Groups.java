package hu.montlikadani.tablist.bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

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

	public List<TeamHandler> getGroupsList() {
		return groupsList;
	}

	public TeamHandler getTeam(String name) {
		Validate.notEmpty(name, "The team name can't be empty/null");

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

		String globPrefix = plugin.getGS().getString("globalGroup.prefix", "");
		String globSuffix = plugin.getGS().getString("globalGroup.suffix", "");
		if (!globPrefix.isEmpty() || !globSuffix.isEmpty()) {
			TeamHandler team = new TeamHandler("global", globPrefix, globSuffix);
			team.setGlobal(true);
			groupsList.add(team);
			startTask();
			return;
		}

		if (!plugin.getGS().isConfigurationSection("groups")) {
			return;
		}

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

				plugin.getGS().set(path + "prefix", "&" + c.getChar());

				c = colors[ThreadLocalRandom.current().nextInt(colors.length)];

				plugin.getGS().set(path + "suffix", "&" + c.getChar());
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

		int last = 0;
		for (String g : plugin.getGS().getConfigurationSection("groups").getKeys(false)) {
			if (g.equalsIgnoreCase("exampleGroup")) {
				continue;
			}

			String path = "groups." + g + ".", prefix = plugin.getGS().getString(path + "prefix", ""),
					suffix = plugin.getGS().getString(path + "suffix", ""),
					perm = plugin.getGS().getString(path + "permission", "");
			int priority = plugin.getGS().getInt(path + "sort-priority", last + 1);

			groupsList.add(new TeamHandler(g, prefix, suffix, perm, priority));

			last = priority;
		}

		startTask();
	}

	public void loadGroupForPlayer(Player p) {
		removePlayerGroup(p).thenAccept(e -> {
			startTask();
			addPlayer(p);
		});
	}

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

	public void removeGroupsFromAll() {
		Bukkit.getOnlinePlayers().forEach(this::removePlayerGroup);
	}

	public CompletableFuture<Boolean> removePlayerGroup(Player p) {
		if (p == null) {
			return CompletableFuture.completedFuture(false);
		}

		TabListPlayer tlp = tLPlayerMap.remove(p.getUniqueId().toString());
		if (tlp == null) {
			return CompletableFuture.completedFuture(false);
		}

		String name = Integer.toString(100000 + tlp.getPriority())
				+ (tlp.getGroup() == null ? p.getName() : tlp.getGroup().getTeam());
		if (name.length() > 16) {
			name = name.substring(0, 16);
		}

		tlp.getTabTeam().unregisterTeam(name);
		tlp.removeGroup();
		sortedTabListPlayers.remove(tlp);
		return CompletableFuture.completedFuture(true);
	}

	public void removeGroup(String teamName) {
		TeamHandler th = getTeam(teamName);
		if (th != null) {
			groupsList.remove(th);
		}
	}

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
