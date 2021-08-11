package hu.montlikadani.tablist.bukkit.tablist;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.bukkit.utils.PluginUtils;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.variables.Variables;

public class TabHandler {

	private final TabList plugin;
	private final UUID playerUUID;

	private boolean worldEnabled = false, tabEmpty = false;

	private final List<String> worldList = new ArrayList<>();

	private String[] header, footer;

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

		final Player player = plugin.getServer().getPlayer(playerUUID);
		if (player == null) {
			return;
		}

		sendEmptyTab(player);

		if (!TabConfigValues.isEnabled() || TabToggleBase.isDisabled(playerUUID) || PluginUtils.isInGame(player)) {
			return;
		}

		final String world = player.getWorld().getName();

		if (TabConfigValues.getDisabledWorlds().contains(world)) {
			return;
		}

		final String pName = player.getName();

		if (TabConfigValues.getBlackListedPlayers().contains(pName)) {
			return;
		}

		final FileConfiguration c = plugin.getConf().getTablist();
		String path = "";

		if (c.contains("per-world")) {
			if (c.contains("per-world." + world + ".per-player." + pName)) {
				path = "per-world." + world + ".per-player." + pName + ".";
				worldEnabled = true;
			} else {
				t: for (String s : TabConfigValues.getPerWorldkeys()) {
					for (String split : TabConfigValues.COMMA_SPACE_SEPARATED_PATTERN.split(s)) {
						if (world.equals(split)) {
							if (plugin.hasVault() && c.contains("per-world." + s + ".per-group")) {
								String group = plugin.getVaultPerm().getPrimaryGroup(split, player);

								if (group != null) {
									group = group.toLowerCase();

									if (c.contains("per-world." + s + ".per-group." + group)) {
										path = "per-world." + s + ".per-group." + group + ".";
										worldEnabled = true;
									}
								}
							}

							if (path.isEmpty()) {
								path = "per-world." + s + ".";
								worldEnabled = worldList.add(split);
							}

							break t;
						}
					}
				}

				if (worldList.isEmpty() && c.contains("per-world." + world)) {
					path = "per-world." + world + ".";
					worldEnabled = true;
				}
			}

			if (plugin.hasVault() && c.contains("per-world." + world + ".per-group")) {
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

		for (String name : TabConfigValues.getPermissionkeys()) {
			if (PluginUtils.hasPermission(player, new Permission(name, PermissionDefault.NOT_OP).getName())) {
				path = "permissions." + name + ".";
				break;
			}
		}

		if (c.contains("per-player." + pName)) {
			path = "per-player." + pName + ".";
		}

		if (plugin.hasVault() && c.contains("per-group")) {
			String group = plugin.getVaultPerm().getPrimaryGroup(player);

			if (group != null) {
				group = group.toLowerCase();

				if (c.contains("per-group." + group)) {
					path = "per-group." + group + ".";
				}
			}
		}

		if (!path.isEmpty()) {
			header = c.isList(path + "header") ? c.getStringList(path + "header").toArray(new String[0])
					: c.isString(path + "header") ? Util.toArray(c.getString(path + "header")) : null;
			footer = c.isList(path + "footer") ? c.getStringList(path + "footer").toArray(new String[0])
					: c.isString(path + "footer") ? Util.toArray(c.getString(path + "footer")) : null;
		}

		if ((header == null || header.length == 0) && (footer == null || footer.length == 0)) {
			header = TabConfigValues.getDefaultHeader();
			footer = TabConfigValues.getDefaultFooter();
		}
	}

	public void sendEmptyTab(Player player) {
		TabTitle.sendTabTitle(player, "", "");
	}

	protected void sendTab() {
		if (header == null && footer == null) {
			return;
		}

		final Player player = plugin.getServer().getPlayer(playerUUID);
		if (player == null) {
			return;
		}

		if (TabToggleBase.isDisabled(playerUUID)
				|| (TabConfigValues.isHideTabWhenPlayerVanished() && PluginUtils.isVanished(player))
				|| TabConfigValues.getDisabledWorlds().contains(player.getWorld().getName())
				|| TabConfigValues.getBlackListedPlayers().contains(player.getName()) || PluginUtils.isInGame(player)) {
			if (!tabEmpty) { // Only send it once to allow other plugins to overwrite tablist
				sendEmptyTab(player);
				tabEmpty = true;
			}

			return;
		}

		if (header != null && header.length == 0 && footer != null && footer.length == 0) {
			return;
		}

		String he = "";
		String fo = "";

		if (TabConfigValues.isRandom()) {
			ThreadLocalRandom random = ThreadLocalRandom.current();

			if (header != null)
				he = header[header.length == 1 ? 0 : random.nextInt(header.length)];

			if (footer != null)
				fo = footer[footer.length == 1 ? 0 : random.nextInt(footer.length)];
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

		tabEmpty = false;

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
			org.bukkit.World world = plugin.getServer().getWorld(l);

			if (world != null) {
				for (Player all : world.getPlayers()) {
					TabTitle.sendTabTitle(all, v.replaceVariables(all, he), v.replaceVariables(all, fo));
				}
			}
		}
	}
}
