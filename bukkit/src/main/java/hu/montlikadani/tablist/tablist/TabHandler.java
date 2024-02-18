package hu.montlikadani.tablist.tablist;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.packets.PacketNM;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.Pair;
import hu.montlikadani.tablist.utils.PluginUtils;
import hu.montlikadani.tablist.utils.reflection.ComponentParser;
import org.bukkit.entity.Player;

import java.util.Random;

public class TabHandler {

	private final TabList plugin;
	private final TabListUser user;

	private boolean tabEmpty;

	private Random random;
	private TabText[] header, footer;
	private TabText linedHeader, linedFooter;

	public TabHandler(TabList plugin, TabListUser user) {
		this.plugin = plugin;
		this.user = user;
	}

	public void loadTabComponents() {
		tabEmpty = false;
		header = footer = null;

		Player player = user.getPlayer();

		if (player == null) {
			return;
		}

		sendEmptyTab(player);

		if (!TabConfigValues.isEnabled() || TabToggleBase.isDisabled(user)) {
			return;
		}

		String world = player.getWorld().getName();

		if (TabConfigValues.getDisabledWorlds().contains(world)) {
			return;
		}

		String pName = player.getName();

		if (TabConfigValues.getBlackListedPlayers().contains(pName)) {
			return;
		}

		TabConfigValues.OptionSeparator optionSeparator = TabConfigValues.SEPARATOR_MAP.get(world);
		String[] playerGroups = null;

		if (optionSeparator != null) {
			Pair<TabText[], TabText[]> pair = optionSeparator.getConfigKeyMap().get(pName);

			if (pair == null && plugin.hasPermissionService()) {
				for (String one : playerGroups = plugin.getPermissionService().getGroups()) {
					if (plugin.getPermissionService().playerInGroup(player, world, one) && (pair = optionSeparator.getConfigKeyMap().get(one)) != null) {
						break;
					}
				}
			}

			if (pair == null) {
				pair = optionSeparator.pair;
			}

			header = pair.key;
			footer = pair.value;
		}

		for (java.util.Map.Entry<org.bukkit.permissions.Permission, Pair<TabText[], TabText[]>> map : TabConfigValues.PERMISSION_MAP.entrySet()) {
			if (PluginUtils.hasPermission(player, map.getKey().getName())) {
				header = map.getValue().key;
				footer = map.getValue().value;
				break;
			}
		}

		if ((optionSeparator = TabConfigValues.SEPARATOR_MAP.get(pName)) != null) {
			header = optionSeparator.pair.key;
			footer = optionSeparator.pair.value;
		}

		if (plugin.hasPermissionService()) {
			for (String one : playerGroups == null ? plugin.getPermissionService().getGroups() : playerGroups) {
				if (plugin.getPermissionService().playerInGroup(player, one) && (optionSeparator = TabConfigValues.SEPARATOR_MAP.get(one)) != null) {
					header = optionSeparator.pair.key;
					footer = optionSeparator.pair.value;
					break;
				}
			}
		}

		if ((header == null || header.length == 0) && (footer == null || footer.length == 0)) {
			header = TabConfigValues.getDefaultHeader();
			footer = TabConfigValues.getDefaultFooter();
		}

		if (header != null) {
			if (header.length == 0) {
				header = null;
			} else {
				StringBuilder lh = new StringBuilder();

				for (int a = 0; a < header.length; a++) {
					if (a != 0) {
						lh.append("\n\u00a7r");
					}

					TabText tt = header[a];
					lh.append(tt.plainText);
					header[a] = tt;
				}

				(linedHeader = new TabText()).updateText(lh.toString());
			}
		}

		if (footer == null) {
			return;
		}

		if (footer.length == 0) {
			footer = null;
			return;
		}

		StringBuilder lf = new StringBuilder();

		for (int a = 0; a < footer.length; a++) {
			if (a != 0) {
				lf.append("\n\u00a7r");
			}

			TabText tt = footer[a];
			lf.append(tt.plainText);
			footer[a] = tt;
		}

		(linedFooter = new TabText()).updateText(lf.toString());
	}

	public void sendEmptyTab(Player player) {
		if (player != null && !tabEmpty) { // Only send it once to allow other plugins to overwrite tablist
			PacketNM.NMS_PACKET.sendTabTitle(player, ComponentParser.EMPTY_COMPONENT, ComponentParser.EMPTY_COMPONENT);
			tabEmpty = true;
		}
	}

	protected void sendTab() {
		if (header == null && footer == null) {
			return;
		}

		Player player = user.getPlayer();

		if (player == null) {
			return;
		}

		if (TabToggleBase.isDisabled(user) || (TabConfigValues.isHideTabWhenPlayerVanished() && PluginUtils.isVanished(player))) {
			sendEmptyTab(player);
			return;
		}

		TabText he = linedHeader;
		TabText fo = linedFooter;

		if (TabConfigValues.isRandom()) {
			if (random == null) {
				random = new Random();
			}

			if (header != null)
				he = header[header.length == 1 ? 0 : random.nextInt(header.length)];

			if (footer != null)
				fo = footer[footer.length == 1 ? 0 : random.nextInt(footer.length)];
		}

		Object head = ComponentParser.EMPTY_COMPONENT;
		Object foot = ComponentParser.EMPTY_COMPONENT;

		if (he != null) {
			TabText tt = new TabText(he);

			tt.plainText = plugin.makeAnim(tt.plainText);
			head = toComponentWithVariables(player, tt);
			tabEmpty = false;
		}

		if (fo != null) {
			TabText tt = new TabText(fo);

			tt.plainText = plugin.makeAnim(tt.plainText);
			foot = toComponentWithVariables(player, tt);
			tabEmpty = false;
		}

		PacketNM.NMS_PACKET.sendTabTitle(player, head, foot);
	}

	private Object toComponentWithVariables(Player player, TabText text) {
		if (!text.plainText.isEmpty()) {
			text.updateText(plugin.getPlaceholders().replaceVariables(player, text.plainText));
		}

		return text.toComponent();
	}
}
