package hu.montlikadani.tablist.tablist;

import java.util.List;
import java.util.UUID;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.Misc;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.ConfigConstants;
import hu.montlikadani.tablist.tablist.text.LegacyTextConverter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

public class PlayerTab {

	private final TabList plugin;
	public final UUID playerId;

	private int i = 0, i2 = 0;

	private String[] header, footer;

	public PlayerTab(TabList plugin, UUID playerId) {
		this.plugin = plugin;
		this.playerId = playerId;
	}

	public ProxiedPlayer getPlayer() {
		return plugin.getProxy().getPlayer(playerId);
	}

	public String[] getHeader() {
		return header;
	}

	public String[] getFooter() {
		return footer;
	}

	private BaseComponent[] getNextHeader(ProxiedPlayer source) {
		if (header == null || header.length == 0) {
			return Misc.EMPTY_COMPONENT_ARRAY;
		}

		if (i >= header.length) {
			i = 0;
		}

		String head = header[i];

		if (head.isEmpty()) {
			return Misc.EMPTY_COMPONENT_ARRAY;
		}

		if (i < header.length - 1) {
			i++;
		} else {
			i = 0;
		}

		return LegacyTextConverter.toBaseComponent(Misc.replaceVariables(head, source));
	}

	private BaseComponent[] getNextFooter(ProxiedPlayer source) {
		if (footer == null || footer.length == 0) {
			return Misc.EMPTY_COMPONENT_ARRAY;
		}

		if (i2 >= footer.length) {
			i2 = 0;
		}

		String foot = footer[i2];

		if (foot.isEmpty()) {
			return Misc.EMPTY_COMPONENT_ARRAY;
		}

		if (i2 < footer.length - 1) {
			i2++;
		} else {
			i2 = 0;
		}

		return LegacyTextConverter.toBaseComponent(Misc.replaceVariables(foot, source));
	}

	public void clearTab() {
		header = footer = null;

		ProxiedPlayer player = getPlayer();

		if (player != null) {
			player.resetTabHeader();
		}
	}

	private String[] fill(String[] array, List<String> list) {
		int size = list.size();

		if (size == 0) {
			return null;
		}

		if (array == null) {
			array = new String[size];
		}

		for (int a = 0; a < size; a++) {
			array[a] = list.get(a);
		}

		return array;
	}

	public void loadTabList() {
		clearTab();

		final ProxiedPlayer player = getPlayer();
		final String pName = player.getName();
		final String server = player.getServer() != null ? player.getServer().getInfo().getName() : "";

		for (java.util.Map.Entry<String, ConfigConstants.TabSetting> one : ConfigConstants.TAB_SETTINGS.entrySet()) {
			for (String split : one.getValue().names) {
				if (server.equalsIgnoreCase(split)) {
					header = fill(header, ConfigConstants.getPerServerSection()
							.getStringList(one.getKey() + ".per-player." + pName + ".header"));

					if (header == null)
						header = fill(header, one.getValue().header);

					footer = fill(footer, ConfigConstants.getPerServerSection()
							.getStringList(one.getKey() + ".per-player." + pName + ".footer"));

					if (footer == null)
						footer = fill(footer, one.getValue().footer);

					break;
				}
			}
		}

		for (ConfigConstants.TabSetting setting : ConfigConstants.TAB_SETTINGS.values()) {
			for (String split : setting.names) {
				if (pName.equalsIgnoreCase(split)) {
					header = fill(header, setting.header);
					footer = fill(footer, setting.footer);
					break;
				}
			}
		}

		if (header != null && footer != null) {
			return;
		}

		final Configuration conf = plugin.getConf();

		if (header == null)
			header = fill(header, conf.getStringList("tablist.per-server." + server + ".per-player." + pName + ".header"));

		if (header == null)
			header = fill(header, conf.getStringList("tablist.per-server." + server + ".header"));

		if (header == null)
			header = fill(header, conf.getStringList("tablist.per-player." + pName + ".header"));

		if (header == null)
			header = fill(header, ConfigConstants.getDefaultHeader());

		if (footer == null)
			footer = fill(footer, conf.getStringList("tablist.per-server." + server + ".per-player." + pName + ".footer"));

		if (footer == null)
			footer = fill(footer, conf.getStringList("tablist.per-server." + server + ".footer"));

		if (footer == null)
			footer = fill(footer, conf.getStringList("tablist.per-player." + pName + ".footer"));

		if (footer == null)
			footer = fill(footer, ConfigConstants.getDefaultFooter());

		if (header != null) {
			for (int i = 0; i < header.length; i++) {
				header[i] = Global.setSymbols(header[i]);
			}
		}

		if (footer != null) {
			for (int i = 0; i < footer.length; i++) {
				footer[i] = Global.setSymbols(footer[i]);
			}
		}
	}

	public void update() {
		if (header == null && footer == null) {
			return;
		}

		final ProxiedPlayer player = getPlayer();

		if (player == null) {
			return;
		}

		if (ConfigConstants.getRestrictedPlayers().contains(player.getName())) {
			player.resetTabHeader();
			return;
		}

		net.md_5.bungee.api.connection.Server playerServer = player.getServer();

		if (playerServer != null && ConfigConstants.getDisabledServers().contains(playerServer.getInfo().getName())) {
			player.resetTabHeader();
		} else {
			player.setTabHeader(getNextHeader(player), getNextFooter(player));
		}
	}
}
