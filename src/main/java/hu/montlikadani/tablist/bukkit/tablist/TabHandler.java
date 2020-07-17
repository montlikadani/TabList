package hu.montlikadani.tablist.bukkit.tablist;

import static hu.montlikadani.tablist.bukkit.utils.Util.logConsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.utils.PluginUtils;
import hu.montlikadani.tablist.bukkit.utils.Variables;

public class TabHandler implements ITabHandler {

	private final TabList plugin;

	private Player player;

	private BukkitTask task;
	private TabBuilder builder;

	public TabHandler(TabList plugin, Player player) {
		this(plugin, player, null);
	}

	public TabHandler(TabList plugin, Player player, TabBuilder builder) {
		this.plugin = plugin;
		this.player = player;
		this.builder = builder == null ? TabBuilder.builder().build() : builder;
	}

	@Override
	public Player getPlayer() {
		return player;
	}

	@Override
	public TabBuilder getBuilder() {
		return builder;
	}

	@Override
	public BukkitTask getTask() {
		return task;
	}

	public void updateTab() {
		if (player == null || !player.isOnline()) {
			return;
		}

		unregisterTab();

		if (plugin.getConf().getTablistFile() == null || !plugin.getConf().getTablistFile().exists()) {
			return;
		}

		final FileConfiguration c = plugin.getTabC();
		if (c == null || !c.getBoolean("enabled")) {
			return;
		}

		final UUID uuid = player.getUniqueId();
		final String world = player.getWorld().getName();
		final String pName = player.getName();

		if (c.getStringList("disabled-worlds").contains(world) || c.getStringList("blacklisted-players").contains(pName)
				|| (TabManager.TABENABLED.containsKey(uuid) && TabManager.TABENABLED.get(uuid))
				|| plugin.isHookPreventTask(player)) {
			return;
		}

		boolean worldEnable = false;

		final List<String> worldList = new ArrayList<>();

		List<String> header = null, footer = null;

		if (c.contains("per-world")) {
			if (c.contains("per-world." + world + ".per-player." + pName)) {
				String path = "per-world." + world + ".per-player." + pName + ".";
				header = c.isList(path + "header") ? c.getStringList(path + "header")
						: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header")) : null;
				footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
						: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer")) : null;

				worldEnable = true;
			}

			if (header == null && footer == null) {
				if (c.isConfigurationSection("per-world")) {
					t: for (String s : c.getConfigurationSection("per-world").getKeys(false)) {
						for (String split : s.split(", ")) {
							if (world.equals(split)) {
								String path = "per-world." + s + ".";

								header = c.isList(path + "header") ? c.getStringList(path + "header")
										: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header"))
												: null;
								footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
										: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer"))
												: null;

								worldList.add(split);
								worldEnable = true;
								break t;
							}
						}
					}
				}

				if (worldList.isEmpty()) {
					if (c.contains("per-world." + world)) {
						String path = "per-world." + world + ".";
						header = c.isList(path + "header") ? c.getStringList(path + "header")
								: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header")) : null;
						footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
								: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer")) : null;

						worldEnable = true;
					}
				}
			}

			if ((header == null && footer == null) && c.contains("per-world." + world + ".per-group")) {
				String group = null;

				if (plugin.isPluginEnabled("Vault")) {
					try {
						group = plugin.getVaultPerm().getPrimaryGroup(world, player);
					} catch (UnsupportedOperationException e) {
						logConsole(Level.WARNING, "You not using any permission manager plugin!");
					}
				}

				if (group != null) {
					if (c.contains("per-world." + world + ".per-group." + group)) {
						String path = "per-world." + world + ".per-group." + group + ".";
						header = c.isList(path + "header") ? c.getStringList(path + "header")
								: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header")) : null;
						footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
								: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer")) : null;

						worldEnable = true;
					}
				}
			}
		}

		if ((header == null && footer == null) && c.contains("per-player")) {
			if (c.contains("per-player." + pName)) {
				String path = "per-player." + pName + ".";
				header = c.isList(path + "header") ? c.getStringList(path + "header")
						: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header")) : null;
				footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
						: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer")) : null;
			}
		}

		if ((header == null && footer == null) && c.contains("per-group")) {
			String group = null;

			if (plugin.isPluginEnabled("Vault")) {
				try {
					group = plugin.getVaultPerm().getPrimaryGroup(player);
				} catch (UnsupportedOperationException e) {
					logConsole(Level.WARNING, "You not using any permission manager plugin!");
				}
			}

			if (group != null) {
				if (c.contains("per-group." + group)) {
					String path = "per-group." + group + ".";
					header = c.isList(path + "header") ? c.getStringList(path + "header")
							: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header")) : null;
					footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
							: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer")) : null;
				}
			}
		}

		if (header == null && footer == null) {
			header = c.isList("header") ? c.getStringList("header")
					: c.isString("header") ? Arrays.asList(c.getString("header")) : null;
			footer = c.isList("footer") ? c.getStringList("footer")
					: c.isString("footer") ? Arrays.asList(c.getString("footer")) : null;
		}

		this.builder = TabBuilder.builder().header(header).footer(footer)
				.random(plugin.getTabC().getBoolean("random", false)).build();

		final int refreshTime = plugin.getTabRefreshTime();
		if (refreshTime < 1) {
			cancelTask();
			sendTab(worldEnable, worldList);
			return;
		}

		final boolean enableW = worldEnable;

		task = createTask(() -> {
			if (player == null || !player.isOnline()) {
				unregisterTab();
				return;
			}

			if (c.getStringList("disabled-worlds").contains(world)
					|| c.getStringList("blacklisted-players").contains(pName) || plugin.isHookPreventTask(player)
					|| (TabManager.TABENABLED.containsKey(uuid) && TabManager.TABENABLED.get(uuid))) {
				unregisterTab();
				return;
			}

			sendTab(enableW, worldList);
		}, refreshTime);
	}

	private BukkitTask createTask(Runnable run, int interval) {
		return plugin.isSpigot() ? Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, run, interval, interval)
				: Bukkit.getScheduler().runTaskTimer(plugin, run, interval, interval);
	}

	private void sendTab(boolean yesWorld, List<String> otherWorlds) {
		if (plugin.getTabC().getBoolean("hide-tab-when-player-vanished") && PluginUtils.isVanished(player)) {
			TabTitle.sendTabTitle(player, "", "");
			return;
		}

		List<String> header = builder.getHeader(), footer = builder.getFooter();

		String he = "";
		String fo = "";

		if (builder.isRandom()) {
			he = header.get(ThreadLocalRandom.current().nextInt(header.size()));
			fo = footer.get(ThreadLocalRandom.current().nextInt(footer.size()));
		}

		int r = 0;

		if (he.isEmpty()) {
			for (String line : header) {
				r++;

				if (r > 1) {
					he += "\n\u00a7r";
				}

				he += line;
			}
		}

		if (fo.isEmpty()) {
			r = 0;

			for (String line : builder.getFooter()) {
				r++;

				if (r > 1) {
					fo += "\n\u00a7r";
				}

				fo += line;
			}
		}

		he = plugin.makeAnim(he);
		fo = plugin.makeAnim(fo);

		Variables v = plugin.getPlaceholders();

		if (!yesWorld) {
			TabTitle.sendTabTitle(player, v.replaceVariables(player, he), v.replaceVariables(player, fo));
			return;
		}

		if (otherWorlds.isEmpty()) {
			for (Player player : this.player.getWorld().getPlayers()) {
				TabTitle.sendTabTitle(player, v.replaceVariables(player, he), v.replaceVariables(player, fo));
			}

			return;
		}

		for (String l : otherWorlds) {
			for (Player player : Bukkit.getWorld(l).getPlayers()) {
				TabTitle.sendTabTitle(player, v.replaceVariables(player, he), v.replaceVariables(player, fo));
			}
		}
	}

	public void unregisterTab() {
		cancelTask();

		TabTitle.sendTabTitle(player, "", "");
	}

	public void cancelTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}
}
