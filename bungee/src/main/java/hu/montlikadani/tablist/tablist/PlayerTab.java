package hu.montlikadani.tablist.tablist;

import java.util.List;
import java.util.UUID;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.Misc;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.ConfigConstants;
import hu.montlikadani.tablist.tablist.text.LegacyTextConverter;
import java.util.concurrent.atomic.AtomicInteger;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

public class PlayerTab {

	private final TabList plugin;
	public final UUID playerId;

	private final AtomicInteger headerIndex = new AtomicInteger();
	private final AtomicInteger footerIndex = new AtomicInteger();

	private String[] header, footer;

	public PlayerTab(TabList plugin, UUID playerId) {
		this.plugin = plugin;
		this.playerId = playerId;
	}

	public ProxiedPlayer getPlayer() {
		return plugin.getProxy().getPlayer(playerId);
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
		final String playerName = player.getName();
		final String serverName = player.getServer() != null ? player.getServer().getInfo().getName() : "";

		if (!serverName.isEmpty()) {
			for (java.util.Map.Entry<String, ConfigConstants.TabSetting> map : ConfigConstants.TAB_SETTINGS.entrySet()) {
				for (String one : map.getValue().names) {
					if (!serverName.equalsIgnoreCase(one)) {
						continue;
					}

					header = fill(header, ConfigConstants.getPerServerSection()
							.getStringList(map.getKey() + ".per-player." + playerName + ".header"));

					if (header == null)
						header = fill(null, map.getValue().header);

					footer = fill(footer, ConfigConstants.getPerServerSection()
							.getStringList(map.getKey() + ".per-player." + playerName + ".footer"));

					if (footer == null)
						footer = fill(null, map.getValue().footer);

					break;
				}
			}
		}

		for (ConfigConstants.TabSetting setting : ConfigConstants.TAB_SETTINGS.values()) {
			for (String one : setting.names) {
				if (playerName.equalsIgnoreCase(one)) {
					header = fill(header, setting.header);
					footer = fill(footer, setting.footer);
					break;
				}
			}
		}

		final Configuration conf = plugin.getConf();

		if (header == null)
			header = fill(null, conf.getStringList("tablist.per-server." + serverName + ".per-player." + playerName + ".header"));

		if (header == null)
			header = fill(null, conf.getStringList("tablist.per-server." + serverName + ".header"));

		if (header == null)
			header = fill(null, conf.getStringList("tablist.per-player." + playerName + ".header"));

		if (header == null)
			header = fill(null, ConfigConstants.getDefaultHeader());

		if (footer == null)
			footer = fill(null, conf.getStringList("tablist.per-server." + serverName + ".per-player." + playerName + ".footer"));

		if (footer == null)
			footer = fill(null, conf.getStringList("tablist.per-server." + serverName + ".footer"));

		if (footer == null)
			footer = fill(null, conf.getStringList("tablist.per-player." + playerName + ".footer"));

		if (footer == null)
			footer = fill(null, ConfigConstants.getDefaultFooter());

		if (header != null) {
			for (int i = 0; i < header.length; i++) {
				header[i] = Global.replaceToUnicodeSymbol(header[i]);
			}
		}

		if (footer != null) {
			for (int i = 0; i < footer.length; i++) {
				footer[i] = Global.replaceToUnicodeSymbol(footer[i]);
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
			player.setTabHeader(next(player, header, headerIndex), next(player, footer, footerIndex));
		}
	}

	private BaseComponent next(ProxiedPlayer source, String[] arr, AtomicInteger index) {
		if (arr == null || arr.length == 0) {
			return Misc.EMPTY_COMPONENT;
		}

		if (index.get() >= arr.length) {
			index.set(0);
		}

		String foot = arr[index.get()];

		if (foot.isEmpty()) {
			return Misc.EMPTY_COMPONENT;
		}

		if (index.get() < arr.length - 1) {
			index.incrementAndGet();
		} else {
			index.set(0);
		}

		return LegacyTextConverter.toBaseComponent(Misc.replaceVariables(foot, source));
	}
}
