package hu.montlikadani.tablist.bukkit.tablist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.bukkit.utils.PluginUtils;
import hu.montlikadani.tablist.bukkit.utils.Variables;

public class TabHandler {

	private final TabList plugin;

	private UUID playerUUID;

	private boolean worldEnabled = false, tabEmpty = false, random = false;

	private final List<String> worldList = new ArrayList<>();

	private List<String> header, footer;

	public TabHandler(TabList plugin, UUID playerUUID) {
		this.plugin = plugin;
		this.playerUUID = playerUUID;
	}

	public UUID getPlayerUUID() {
		return playerUUID;
	}

	public void updateTab() {
		worldList.clear();
		worldEnabled = tabEmpty = false;
		header = footer = null;

		final Player player = Bukkit.getServer().getPlayer(playerUUID);
		if (player == null || !player.isOnline()) {
			return;
		}

		TabTitle.sendTabTitle(player, "", "");

		if (!TabConfigValues.isEnabled()) {
			return;
		}

		final String world = player.getWorld().getName();
		final String pName = player.getName();

		if (TabConfigValues.getDisabledWorlds().contains(world)
				|| TabConfigValues.getBlackListedPlayers().contains(pName)
				|| TabManager.TABENABLED.getOrDefault(playerUUID, false) || PluginUtils.isInGame(player)) {
			return;
		}

		random = TabConfigValues.isRandom();

		final FileConfiguration c = plugin.getConf().getTablist();
		String path = "";

		if (c.contains("per-world")) {
			if (c.contains("per-world." + world + ".per-player." + pName)) {
				path = "per-world." + world + ".per-player." + pName + ".";
				worldEnabled = true;
			}

			if (path.isEmpty()) {
				if (c.isConfigurationSection("per-world")) {
					t: for (String s : c.getConfigurationSection("per-world").getKeys(false)) {
						for (String split : s.split(", ")) {
							if (world.equals(split)) {
								path = "per-world." + s + ".";
								worldEnabled = worldList.add(split);
								break t;
							}
						}
					}
				}

				if (worldList.isEmpty() && c.contains("per-world." + world)) {
					path = "per-world." + world + ".";
					worldEnabled = true;
				}
			}

			if (path.isEmpty() && plugin.hasVault() && c.contains("per-world." + world + ".per-group")) {
				String group = plugin.getVaultPerm().getPrimaryGroup(world, player);
				if (group != null) {
					group = group.toLowerCase();

					if (c.contains("per-world." + world + ".per-group." + group)) {
						path = "per-world." + world + ".per-group." + group + ".";
						worldEnabled = true;
					}
				}
			}
		}

		if (path.isEmpty() && c.isConfigurationSection("permissions")) {
			for (String name : c.getConfigurationSection("permissions").getKeys(false)) {
				Permission permission = new Permission(name.startsWith("tablist.") ? name : "tablist." + name,
						PermissionDefault.NOT_OP);
				if (PluginUtils.hasPermission(player, permission.getName())) {
					path = "permissions." + name + ".";
					break;
				}
			}
		}

		if (path.isEmpty() && c.contains("per-player." + pName)) {
			path = "per-player." + pName + ".";
		}

		if (path.isEmpty() && plugin.hasVault() && c.contains("per-group")) {
			String group = plugin.getVaultPerm().getPrimaryGroup(player);
			if (group != null) {
				group = group.toLowerCase();

				if (c.contains("per-group." + group)) {
					path = "per-group." + group + ".";
				}
			}
		}

		if (!path.isEmpty()) {
			header = c.isList(path + "header") ? c.getStringList(path + "header")
					: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header")) : null;
			footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
					: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer")) : null;
		}

		if ((header == null || header.isEmpty()) && (footer == null || footer.isEmpty())) {
			header = c.isList("header") ? c.getStringList("header")
					: c.isString("header") ? Arrays.asList(c.getString("header")) : null;
			footer = c.isList("footer") ? c.getStringList("footer")
					: c.isString("footer") ? Arrays.asList(c.getString("footer")) : null;
		}
	}

	protected void sendTab() {
		if (header == null && footer == null) {
			return;
		}

		final Player player = Bukkit.getServer().getPlayer(playerUUID);
		if (player == null || !player.isOnline()) {
			return;
		}

		if ((TabConfigValues.isHideTabWhenPlayerVanished() && PluginUtils.isVanished(player))
				|| TabConfigValues.getDisabledWorlds().contains(player.getWorld().getName())
				|| TabConfigValues.getBlackListedPlayers().contains(player.getName()) || PluginUtils.isInGame(player)
				|| TabManager.TABENABLED.getOrDefault(playerUUID, false)) {
			if (!tabEmpty) { // Only send it once to allow other plugins to overwrite tablist
				TabTitle.sendTabTitle(player, "", "");
				tabEmpty = true;
			}

			return;
		}

		if (header != null && header.isEmpty() && footer != null && footer.isEmpty()) {
			return;
		}

		String he = "";
		String fo = "";

		if (random) {
			ThreadLocalRandom random = ThreadLocalRandom.current();

			if (header != null)
				he = header.get(random.nextInt(header.size()));

			if (footer != null)
				fo = footer.get(random.nextInt(footer.size()));
		}

		int r = 0;

		if (header != null && he.isEmpty()) {
			for (String line : header) {
				r++;

				if (r > 1) {
					he += "\n\u00a7r";
				}

				he += line;
			}
		}

		if (footer != null && fo.isEmpty()) {
			r = 0;

			for (String line : footer) {
				r++;

				if (r > 1) {
					fo += "\n\u00a7r";
				}

				fo += line;
			}
		}

		if (he.isEmpty() && fo.isEmpty()) {
			return;
		}

		he = plugin.makeAnim(he);
		fo = plugin.makeAnim(fo);

		final Variables v = plugin.getPlaceholders();

		if (!worldEnabled) {
			TabTitle.sendTabTitle(player, v.replaceVariables(player, he), v.replaceVariables(player, fo));
			return;
		}

		if (worldList.isEmpty()) {
			for (Player all : player.getWorld().getPlayers()) {
				TabTitle.sendTabTitle(all, v.replaceVariables(all, he), v.replaceVariables(all, fo));
			}

			return;
		}

		for (String l : worldList) {
			org.bukkit.World world = Bukkit.getServer().getWorld(l);
			if (world != null) {
				for (Player all : world.getPlayers()) {
					TabTitle.sendTabTitle(all, v.replaceVariables(all, he), v.replaceVariables(all, fo));
				}
			}
		}
	}
}
