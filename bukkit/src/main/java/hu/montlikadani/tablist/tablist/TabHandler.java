package hu.montlikadani.tablist.tablist;

import java.util.Random;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

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
	private TabText[] header, footer;
	private TabText linedHeader, linedFooter;

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

		if (c.get("per-world", null) != null) {
			if (c.get("per-world." + world + ".per-player." + pName, null) != null) {
				path = "per-world." + world + ".per-player." + pName + ".";
				worldEnabled = true;
			} else {
				t: for (String s : TabConfigValues.getPerWorldkeys()) {
					for (String split : StrUtil.getCommaSpaceSeparatedPattern().split(s)) {
						if (world.equals(split)) {
							if (plugin.hasVault() && c.get("per-world." + s + ".per-group", null) != null) {
								String group = plugin.getVaultPerm().getPrimaryGroup(split, player);

								if (group != null) {
									group = group.toLowerCase();

									if (c.get("per-world." + s + ".per-group." + group, null) != null) {
										path = "per-world." + s + ".per-group." + group + ".";
										worldEnabled = true;
									}
								}
							}

							if (path.isEmpty()) {
								path = "per-world." + s + '.';
								worldEnabled = worldList.add(split);
							}

							break t;
						}
					}
				}

				if (worldList.isEmpty() && c.get("per-world." + world, null) != null) {
					path = "per-world." + world + '.';
					worldEnabled = true;
				}
			}

			if (plugin.hasVault() && c.get("per-world." + world + ".per-group", null) != null) {
				String group = plugin.getVaultPerm().getPrimaryGroup(world, player);

				if (group != null) {
					group = group.toLowerCase();

					if (c.get("per-world." + world + ".per-group." + group, null) != null) {
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
				path = "permissions." + map.getKey() + '.';
				break;
			}
		}

		if (c.get("per-player." + pName, null) != null) {
			path = "per-player." + pName + ".";
		}

		if (plugin.hasVault() && c.get("per-group", null) != null) {
			String group = plugin.getVaultPerm().getPrimaryGroup(player);

			if (group != null) {
				group = group.toLowerCase();

				if (c.get("per-group." + group, null) != null) {
					path = "per-group." + group + ".";
				}
			}
		}

		if (!path.isEmpty()) {
			header = TabConfigValues.objectToArrayConversion(c.get(path + "header", null));
			footer = TabConfigValues.objectToArrayConversion(c.get(path + "footer", null));
		}

		if ((header == null || header.length == 0) && (footer == null || footer.length == 0)) {
			header = TabConfigValues.getDefaultHeader();
			footer = TabConfigValues.getDefaultFooter();
		}

		if (header != null) {
			linedHeader = new TabText();
			StringBuilder lh = new StringBuilder();

			for (int a = 0; a < header.length; a++) {
				if (a != 0) {
					lh.append("\n\u00a7r");
				}

				TabText tt = header[a];
				lh.append(tt.plainText = plugin.getPlaceholders().replaceMiscVariables(tt.plainText));
				header[a] = tt;
			}

			linedHeader.updateText(lh.toString());
		}

		if (footer != null) {
			linedFooter = new TabText();
			StringBuilder lf = new StringBuilder();

			for (int a = 0; a < footer.length; a++) {
				if (a != 0) {
					lf.append("\n\u00a7r");
				}

				TabText tt = footer[a];
				lf.append(tt.plainText = plugin.getPlaceholders().replaceMiscVariables(tt.plainText));
				footer[a] = tt;
			}

			linedFooter.updateText(lf.toString());
		}
	}

	public void sendEmptyTab(Player player) {
		if (player != null && !tabEmpty) { // Only send it once to allow other plugins to overwrite tablist
			TabTitle.sendTabTitle(player, TabText.EMPTY, TabText.EMPTY);
			tabEmpty = true;
		}
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
				|| (TabConfigValues.isHideTabWhenPlayerVanished() && PluginUtils.isVanished(player))) {
			sendEmptyTab(player);
			return;
		}

		if (header != null && header.length == 0 && footer != null && footer.length == 0) {
			return;
		}

		TabText he = null;
		TabText fo = null;

		if (TabConfigValues.isRandom()) {
			if (random == null) {
				random = new Random();
			}

			if (header != null)
				he = header[header.length == 1 ? 0 : random.nextInt(header.length)];

			if (footer != null)
				fo = footer[footer.length == 1 ? 0 : random.nextInt(footer.length)];
		}

		if (he == null) {
			he = linedHeader;
		}

		if (fo == null) {
			fo = linedFooter;
		}

		if (he == null && fo == null) {
			return;
		}

		if (tabEmpty) {
			tabEmpty = false;
		}

		if (he != null) {
			TabText tt = new TabText(he);
			tt.plainText = plugin.makeAnim(tt.plainText);
			he = tt;
		}

		if (fo != null) {
			TabText tt = new TabText(fo);
			tt.plainText = plugin.makeAnim(tt.plainText);
			fo = tt;
		}

		final Variables v = plugin.getPlaceholders();

		if (!worldEnabled) {
			TabTitle.sendTabTitle(player, v.replaceVariables(player, he), v.replaceVariables(player, fo));
			return;
		}

		if (worldList.isEmpty()) {
			for (Player all : player.getWorld().getPlayers()) {
				TabTitle.sendTabTitle(all, v.replaceVariables(all, new TabText(he)), v.replaceVariables(all, new TabText(fo)));
			}

			return;
		}

		org.bukkit.World world;

		for (String l : worldList) {
			if ((world = plugin.getServer().getWorld(l)) != null) {
				for (Player all : world.getPlayers()) {
					TabTitle.sendTabTitle(all, v.replaceVariables(all, new TabText(he)),
							v.replaceVariables(all, new TabText(fo)));
				}
			}
		}
	}
}
