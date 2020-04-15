package hu.montlikadani.tablist.bukkit;

import static hu.montlikadani.tablist.bukkit.utils.Util.logConsole;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import hu.montlikadani.tablist.bukkit.utils.Variables;

public class TabHandler {

	private final TabList plugin;

	private int updateInterval;

	private List<String> header;
	private List<String> footer;

	private final Map<UUID, BukkitTask> task = new HashMap<>();

	public static Map<UUID, Boolean> tabEnabled = new HashMap<>();

	@Deprecated
	private List<String> lHeader;
	@Deprecated
	private List<String> lFooter;
	@Deprecated
	private String sHeader;
	@Deprecated
	private String sFooter;

	public TabHandler(TabList plugin) {
		this(plugin, 0);
	}

	public TabHandler(TabList plugin, int updateInterval) {
		this(plugin, updateInterval, null, null);
	}

	public TabHandler(TabList plugin, int updateInterval, List<String> header, List<String> footer) {
		this.plugin = plugin;
		this.updateInterval = updateInterval;
		this.header = header;
		this.footer = footer;
	}

	public List<String> getHeader() {
		return header;
	}

	public void setHeader(List<String> header) {
		this.header = header;
	}

	public List<String> getFooter() {
		return footer;
	}

	public void setFooter(List<String> footer) {
		this.footer = footer;
	}

	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
	}

	public int getUpdateInterval() {
		return updateInterval;
	}

	public Map<UUID, BukkitTask> getTasks() {
		return task;
	}

	public void updateTab(final Player p) {
		if (p == null) {
			return;
		}

		if (plugin.isUsingOldTab()) {
			updateOldTab(p);
			return;
		}

		if (plugin.getConf().getTablistFile() == null || !plugin.getConf().getTablistFile().exists()) {
			return;
		}

		final FileConfiguration c = plugin.getTabC();
		if (c == null) {
			return;
		}

		if (plugin.isUsingOldTab() && !c.getBoolean("enabled")) {
			unregisterTab();
			return;
		}

		final UUID uuid = p.getUniqueId();

		if (tabEnabled.containsKey(uuid) && tabEnabled.get(uuid)) {
			return;
		}

		if (plugin.isHookPreventTask(p)) {
			return;
		}

		final String world = p.getWorld().getName();
		final String pName = p.getName();

		List<String> header = null;
		List<String> footer = null;

		boolean worldEnable = false;

		final List<String> worldList = new ArrayList<>();

		String group = null;
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
					for (String s : c.getConfigurationSection("per-world").getKeys(false)) {
						worldList.add(s);
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
				} else {
					t: for (String w : worldList) {
						for (String split : w.split(", ")) {
							if (world.equals(split)) {
								String path = "per-world." + w + ".";
								hu.montlikadani.tablist.bukkit.utils.Util.logConsole(path + "header");

								header = c.isList(path + "header") ? c.getStringList(path + "header")
										: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header"))
												: null;
								footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
										: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer"))
												: null;

								worldEnable = true;
								break t;
							}
						}
					}
				}
			}

			if ((header == null && footer == null) && c.contains("per-world." + world + ".per-group")) {
				if (plugin.isPluginEnabled("Vault")) {
					try {
						group = plugin.getVaultPerm().getPrimaryGroup(world, p);
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
			if (plugin.isPluginEnabled("Vault")) {
				try {
					group = plugin.getVaultPerm().getPrimaryGroup(p);
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

		setHeader(header);
		setFooter(footer);

		// Splitting the ", " from names
		List<String> newWorlds = worldList;
		worldList.clear();
		for (String w : newWorlds) {
			for (String split : w.split(", ")) {
				worldList.add(split);
			}
		}

		if (updateInterval < 1) {
			cancelTask(p);

			if (c.getStringList("disabled-worlds").contains(world)) {
				return;
			}

			if (c.getStringList("blacklisted-players").contains(pName)) {
				return;
			}

			sendTab(p, worldEnable, worldList);
			return;
		}

		final boolean enableW = worldEnable;

		task.put(uuid, createTask(() -> {
			if (Bukkit.getOnlinePlayers().isEmpty()) {
				cancelTask(p);
				return;
			}

			if (c.getStringList("disabled-worlds").contains(world)) {
				return;
			}

			if (c.getStringList("blacklisted-players").contains(pName)) {
				return;
			}

			if (plugin.isHookPreventTask(p)) {
				return;
			}

			if (tabEnabled.containsKey(uuid) && tabEnabled.get(uuid)) {
				TabTitle.sendTabTitle(p, "", "");
			} else {
				sendTab(p, enableW, worldList);
			}
		}, updateInterval));
	}

	private BukkitTask createTask(Runnable run, int interval) {
		return plugin.isSpigot() ? Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, run, interval, interval)
				: Bukkit.getScheduler().runTaskTimer(plugin, run, interval, interval);
	}

	@Deprecated
	private void updateOldTab(final Player p) {
		final FileConfiguration c = plugin.getC();
		if (!plugin.isUsingOldTab() && !c.getBoolean("tablist.enable")) {
			unregisterTab();
			return;
		}

		if (!p.isOnline())
			return;

		if (tabEnabled != null
				&& (tabEnabled.containsKey(p.getUniqueId()) && tabEnabled.get(p.getUniqueId())))
			return;

		if (plugin.isHookPreventTask(p)) {
			return;
		}

		String world = p.getWorld().getName();
		String pName = p.getName();
		lHeader = null;
		lFooter = null;
		sHeader = null;
		sFooter = null;

		lHeader = new ArrayList<>();
		lFooter = new ArrayList<>();

		boolean worldEnable = false;

		if (c.getBoolean("tablist.per-world.enable") || c.getBoolean("tablist.per-player.enable")
				|| c.getBoolean("tablist.per-group.enable")) {
			if (c.getBoolean("tablist.per-world.enable")) {
				if (c.getBoolean("tablist.per-world." + world + ".per-player.enable")) {
					if (!c.contains("tablist.per-world." + world + ".per-player." + pName)) {
						if (c.getBoolean(
								"tablist.per-world." + world + ".per-player.use-default-tab-if-player-not-specified")) {
							isListorStringHeader("tablist.header", false);
							isListorStringFooter("tablist.footer", false);
						} else {
							TabTitle.sendTabTitle(p, "", "");
							return;
						}
					} else {
						isListorStringHeader("tablist.per-world." + world + ".per-player." + pName + ".header", false);
						isListorStringFooter("tablist.per-world." + world + ".per-player." + pName + ".footer", false);
						worldEnable = true;
					}
				} else {
					if (c.getBoolean("tablist.per-world." + world + ".per-group.enable")) {
						if (plugin.isPluginEnabled("Vault")) {
							String group = plugin.getVaultPerm().getPrimaryGroup(world, p);
							if (!c.contains("tablist.per-world." + world + ".per-group." + group)) {
								if (c.getBoolean("tablist.per-world." + world
										+ ".per-group.use-default-tab-if-group-not-specified")) {
									isListorStringHeader("tablist.header", false);
									isListorStringFooter("tablist.footer", false);
								} else {
									TabTitle.sendTabTitle(p, "", "");
									return;
								}
							} else {
								isListorStringHeader("tablist.per-world." + world + ".per-group." + group + ".header", false);
								isListorStringFooter("tablist.per-world." + world + ".per-group." + group + ".footer", false);
								worldEnable = true;
							}
						} else {
							logConsole(Level.WARNING, "The Vault plugin not found. Without the per-group not work.");

							isListorStringHeader("tablist.header", false);
							isListorStringFooter("tablist.footer", false);
						}
					} else {
						if (!c.contains("tablist.per-world." + world)) {
							if (c.getBoolean("tablist.per-world.use-default-tab-if-world-not-specified")) {
								isListorStringHeader("tablist.header", false);
								isListorStringFooter("tablist.footer", false);
								worldEnable = false;
							} else {
								TabTitle.sendTabTitle(p, "", "");
								return;
							}
						} else {
							isListorStringHeader("tablist.per-world." + world + ".header", false);
							isListorStringFooter("tablist.per-world." + world + ".footer", false);
							worldEnable = true;
						}
					}
				}
			}
			if (c.getBoolean("tablist.per-player.enable")) {
				if (!c.contains("tablist.per-player." + pName)) {
					if (c.getBoolean("tablist.per-player.use-default-tab-if-player-not-specified")) {
						isListorStringHeader("tablist.header", false);
						isListorStringFooter("tablist.footer", false);
					} else {
						TabTitle.sendTabTitle(p, "", "");
						return;
					}
				} else {
					isListorStringHeader("tablist.per-player." + pName + ".header", false);
					isListorStringFooter("tablist.per-player." + pName + ".footer", false);
				}
			}
			if (c.getBoolean("tablist.per-group.enable")) {
				if (plugin.isPluginEnabled("Vault")) {
					String group = plugin.getVaultPerm().getPrimaryGroup(p);
					if (!c.contains("tablist.per-group." + group)) {
						if (c.getBoolean("tablist.per-group.use-default-tab-if-group-not-specified")) {
							isListorStringHeader("tablist.header", false);
							isListorStringFooter("tablist.footer", false);
						} else {
							TabTitle.sendTabTitle(p, "", "");
							return;
						}
					} else {
						isListorStringHeader("tablist.per-group." + group + ".header", false);
						isListorStringFooter("tablist.per-group." + group + ".footer", false);
					}
				} else {
					logConsole(Level.WARNING, "The Vault plugin not found. Without the per-group not work.");

					isListorStringHeader("tablist.header", false);
					isListorStringFooter("tablist.footer", false);
				}
			}
		} else {
			isListorStringHeader("tablist.header", false);
			isListorStringFooter("tablist.footer", false);
			worldEnable = false;
		}

		UUID uuid = p.getUniqueId();

		if (updateInterval < 1) {
			cancelTask(p);

			if (c.getStringList("tablist.disabled-worlds").contains(world)) {
				return;
			}

			if (c.getStringList("tablist.blacklisted-players").contains(pName)) {
				return;
			}

			sendTab(p, worldEnable);
			return;
		}

		final boolean enableW = worldEnable;

		try { // For Spigot and Paper
			Class.forName("org.spigotmc.SpigotConfig");

			task.put(uuid, Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
				if (Bukkit.getOnlinePlayers().isEmpty()) {
					cancelTask(p);
					return;
				}

				if (c.getStringList("tablist.disabled-worlds").contains(world)) {
					return;
				}

				if (c.getStringList("tablist.blacklisted-players").contains(pName)) {
					return;
				}

				if (plugin.isHookPreventTask(p)) {
					return;
				}

				if (tabEnabled != null
						&& (tabEnabled.containsKey(p.getUniqueId()) && tabEnabled.get(p.getUniqueId()))) {
					TabTitle.sendTabTitle(p, "", "");
				} else {
					sendTab(p, enableW);
				}
			}, updateInterval, updateInterval));
		} catch (ClassNotFoundException e) { // For CraftBukkit
			task.put(uuid, Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
				@Override
				public void run() {
					if (Bukkit.getOnlinePlayers().isEmpty()) {
						cancelTask(p);
						return;
					}

					if (c.getStringList("tablist.disabled-worlds").contains(world)) {
						return;
					}

					if (c.getStringList("tablist.blacklisted-players").contains(pName)) {
						return;
					}

					if (plugin.isHookPreventTask(p)) {
						return;
					}

					if (tabEnabled != null && (tabEnabled.containsKey(p.getUniqueId())
							&& tabEnabled.get(p.getUniqueId()))) {
						TabTitle.sendTabTitle(p, "", "");
					} else {
						sendTab(p, enableW);
					}
				}
			}, updateInterval, updateInterval));
		}
	}

	private void sendTab(Player p, boolean yesWorld, List<String> otherWorlds) {
		if (plugin.isVanished(p, false) && plugin.getTabC().getBoolean("hide-tab-when-player-vanished")) {
			TabTitle.sendTabTitle(p, "", "");
			return;
		}

		int r = 0;
		String he = "";

		if (header != null) {
			if (plugin.getTabC().getBoolean("random", false)) {
				he = header.get(ThreadLocalRandom.current().nextInt(header.size()));
			}

			if (he.isEmpty()) {
				for (String line : header) {
					r++;

					if (r > 1) {
						he = he + "\n\u00a7r";
					}

					he = he + line;
				}
			}
		}

		String fo = "";

		if (footer != null) {
			if (plugin.getTabC().getBoolean("random", false)) {
				fo = footer.get(ThreadLocalRandom.current().nextInt(footer.size()));
			}

			if (fo.isEmpty()) {
				r = 0;

				for (String line : footer) {
					r++;

					if (r > 1) {
						fo = fo + "\n\u00a7r";
					}

					fo = fo + line;
				}
			}
		}

		if (he.trim().isEmpty()) {
			he = "Something wrong with your tablist config in header section! Please check it!";
		}

		if (fo.trim().isEmpty()) {
			fo = "Something wrong with your tablist config in footer section! Please check it!";
		}

		he = plugin.makeAnim(he);
		fo = plugin.makeAnim(fo);

		Variables v = plugin.getPlaceholders();

		if (!yesWorld) {
			TabTitle.sendTabTitle(p, v.replaceVariables(p, he), v.replaceVariables(p, fo));
			return;
		}

		if (otherWorlds.isEmpty()) {
			for (Player player : p.getWorld().getPlayers()) {
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

	@Deprecated
	private void sendTab(Player p, boolean yesWorld) {
		if (plugin.isUsingOldTab()) {
			if (plugin.isVanished(p, false) && plugin.getC().getBoolean("tablist.hide-tab-when-player-vanished")) {
				TabTitle.sendTabTitle(p, "", "");
				return;
			}
		} else {
			if (plugin.isVanished(p, false) && plugin.getTabC().getBoolean("hide-tab-when-player-vanished")) {
				TabTitle.sendTabTitle(p, "", "");
				return;
			}
		}

		String he = "";
		String fo = "";
		int r = 0;
		if (sHeader == null) {
			for (String line : lHeader) {
				r++;

				if (r > 1) {
					he = he + "\n\u00a7r";
				}

				he = he + line;
			}
		} else {
			he = he + sHeader;
		}

		if (sFooter == null) {
			r = 0;
			for (String line : lFooter) {
				r++;

				if (r > 1) {
					fo = fo + "\n\u00a7r";
				}

				fo = fo + line;
			}
		} else {
			fo = fo + sFooter;
		}

		he = plugin.makeAnim(he);
		fo = plugin.makeAnim(fo);

		if (yesWorld) {
			Collection<? extends Player> worldPlayers = p.getWorld().getPlayers();
			Iterator<? extends Player> itPlayers = worldPlayers.iterator();
			while (itPlayers.hasNext()) {
				Player player = itPlayers.next();
				TabTitle.sendTabTitle(player, plugin.getPlaceholders().replaceVariables(player, he),
						plugin.getPlaceholders().replaceVariables(player, fo));
			}
		} else {
			TabTitle.sendTabTitle(p, plugin.getPlaceholders().replaceVariables(p, he),
					plugin.getPlaceholders().replaceVariables(p, fo));
		}
	}

	@Deprecated
	private void isListorStringHeader(String path, boolean newConfig) {
		if (newConfig) {
			if (plugin.getTabC().isList(path)) {
				lHeader = plugin.getTabC().getStringList(path);
			} else if (plugin.getTabC().isString(path)) {
				sHeader = plugin.getTabC().getString(path);
			}
		} else {
			if (plugin.getC().isList(path)) {
				lHeader = plugin.getC().getStringList(path);
			} else if (plugin.getC().isString(path)) {
				sHeader = plugin.getC().getString(path);
			}
		}
	}

	@Deprecated
	private void isListorStringFooter(String path, boolean newConfig) {
		if (newConfig) {
			if (plugin.getTabC().isList(path)) {
				lFooter = plugin.getTabC().getStringList(path);
			} else if (plugin.getTabC().isString(path)) {
				sFooter = plugin.getTabC().getString(path);
			}
		} else {
			if (plugin.getC().isList(path)) {
				lFooter = plugin.getC().getStringList(path);
			} else if (plugin.getC().isString(path)) {
				sFooter = plugin.getC().getString(path);
			}
		}
	}

	public void unregisterTab(Player p) {
		cancelTask(p);

		TabTitle.sendTabTitle(p, "", "");
	}

	public void unregisterTab() {
		for (Player ps : Bukkit.getOnlinePlayers()) {
			cancelTask(ps);
			TabTitle.sendTabTitle(ps, "", "");
		}

		// if there are still
		task.clear();
	}

	public void cancelTask(Player p) {
		UUID uuid = p.getUniqueId();
		if (task.containsKey(uuid)) {
			task.get(uuid).cancel();
			task.remove(uuid);
		}
	}

	protected void loadToggledTabs() {
		if (plugin.getTabC() == null || !plugin.getTabC().getBoolean("remember-toggled-tablist-to-file", true)) {
			return;
		}

		tabEnabled.clear();

		File f = new File(plugin.getFolder(), "toggledtablists.yml");
		if (f.exists()) {
			FileConfiguration t = YamlConfiguration.loadConfiguration(f);

			if (t.contains("tablists")) {
				for (String uuid : t.getConfigurationSection("tablists").getKeys(false)) {
					tabEnabled.put(UUID.fromString(uuid), t.getConfigurationSection("tablists").getBoolean(uuid));
				}
			}
		}
	}

	protected void saveToggledTabs() {
		File f = new File(plugin.getFolder(), "toggledtablists.yml");
		if (plugin.getTabC() == null || !plugin.getTabC().getBoolean("remember-toggled-tablist-to-file", true)) {
			if (f.exists()) {
				f.delete();
			}

			return;
		}

		if (tabEnabled.isEmpty()) {
			return;
		}

		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		FileConfiguration t = YamlConfiguration.loadConfiguration(f);
		t.set("tablists", null);

		for (Entry<UUID, Boolean> list : tabEnabled.entrySet()) {
			if (list.getValue()) {
				t.set("tablists." + list.getKey(), list.getValue());
			}
		}

		try {
			t.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}

		tabEnabled.clear();
	}
}
