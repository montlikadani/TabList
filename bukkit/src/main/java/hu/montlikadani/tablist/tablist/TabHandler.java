package hu.montlikadani.tablist.tablist;

import java.util.Random;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.PluginUtils;
import hu.montlikadani.tablist.utils.StrUtil;
import hu.montlikadani.tablist.utils.variables.Variables;

public class TabHandler {

	private final TabList plugin;
	private final TabListUser user;

	private boolean worldEnabled = false, tabEmpty = false;

	private final java.util.List<String> worldList = new java.util.ArrayList<>();

	private Random random;
	private String[] header, footer;
	private String linedHeader, linedFooter;

	public TabHandler(TabList plugin, TabListUser user) {
		this.plugin = plugin;
		this.user = user;
	}

	public void loadTabComponents() {
		worldList.clear();
		worldEnabled = tabEmpty = false;
		header = footer = null;

		final Player player = user.getPlayer();
		if (player == null) {
			return;
		}

		sendEmptyTab(player);

		if (TabToggleBase.isDisabled(user.getUniqueId())) {
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

		if (c.get("per-world") != null) {
			if (c.get("per-world." + world + ".per-player." + pName) != null) {
				path = "per-world." + world + ".per-player." + pName + ".";
				worldEnabled = true;
			} else {
				t: for (String s : TabConfigValues.getPerWorldkeys()) {
					for (String split : StrUtil.getCommaSpaceSeparatedPattern().split(s)) {
						if (world.equals(split)) {
							if (plugin.hasVault() && c.get("per-world." + s + ".per-group") != null) {
								String group = plugin.getVaultPerm().getPrimaryGroup(split, player);

								if (group != null) {
									group = group.toLowerCase();

									if (c.get("per-world." + s + ".per-group." + group) != null) {
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

				if (worldList.isEmpty() && c.get("per-world." + world) != null) {
					path = "per-world." + world + ".";
					worldEnabled = true;
				}
			}

			if (plugin.hasVault() && c.get("per-world." + world + ".per-group") != null) {
				String group = plugin.getVaultPerm().getPrimaryGroup(world, player);

				if (group != null) {
					group = group.toLowerCase();

					if (c.get("per-world." + world + ".per-group." + group) != null) {
						path = "per-world." + world + ".per-group." + group + ".";
						worldEnabled = true;
					}
				}
			}
		}

		for (java.util.Map.Entry<String, String> map : TabConfigValues.getPermissionkeys().entrySet()) {
			Permission perm = plugin.getServer().getPluginManager().getPermission(map.getValue());

			// Hacky solution: permission should be added before we checks for the player,
			// because operator players have all permissions by default
			if (perm != null) {

				// Set and recalculate existing permission
				perm.setDefault(PermissionDefault.FALSE);
			} else {
				perm = new Permission(map.getValue(), PermissionDefault.NOT_OP);
				plugin.getServer().getPluginManager().addPermission(perm);
			}

			if (PluginUtils.hasPermission(player, perm.getName())) {
				path = "permissions." + map.getKey() + ".";
				break;
			}
		}

		if (c.get("per-player." + pName) != null) {
			path = "per-player." + pName + ".";
		}

		if (plugin.hasVault() && c.get("per-group") != null) {
			String group = plugin.getVaultPerm().getPrimaryGroup(player);

			if (group != null) {
				group = group.toLowerCase();

				if (c.get("per-group." + group) != null) {
					path = "per-group." + group + ".";
				}
			}
		}

		if (!path.isEmpty()) {
			header = TabConfigValues.stringToArrayConversion(c.get(path + "header", null));
			footer = TabConfigValues.stringToArrayConversion(c.get(path + "footer", null));
		}

		if ((header == null || header.length == 0) && (footer == null || footer.length == 0)) {
			header = TabConfigValues.getDefaultHeader();
			footer = TabConfigValues.getDefaultFooter();
		}

		if (header != null) {
			linedHeader = "";

			for (int a = 0; a < header.length; a++) {
				if (a + 1 > 1) {
					linedHeader += "\n\u00a7r";
				}

				header[a] = Global.setSymbols(header[a]);
				linedHeader += header[a];
			}
		}

		if (footer != null) {
			linedFooter = "";

			for (int a = 0; a < footer.length; a++) {
				if (a + 1 > 1) {
					linedFooter += "\n\u00a7r";
				}

				footer[a] = Global.setSymbols(footer[a]);
				linedFooter += footer[a];
			}
		}
	}

	public void sendEmptyTab(Player player) {
		TabTitle.sendTabTitle(player, "", "");
	}

	protected void sendTab() {
		if (header == null && footer == null) {
			return;
		}

		final Player player = user.getPlayer();
		if (player == null) {
			return;
		}

		if (TabToggleBase.isDisabled(user.getUniqueId())
				|| (TabConfigValues.isHideTabWhenPlayerVanished() && PluginUtils.isVanished(player))
				|| TabConfigValues.getDisabledWorlds().contains(player.getWorld().getName())) {
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
			if (random == null) {
				random = new Random();
			}

			if (header != null)
				he = header[header.length == 1 ? 0 : random.nextInt(header.length)];

			if (footer != null)
				fo = footer[footer.length == 1 ? 0 : random.nextInt(footer.length)];
		}

		if (linedHeader != null && he.isEmpty()) {
			he = linedHeader;
		}

		if (linedFooter != null && fo.isEmpty()) {
			fo = linedFooter;
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
