package hu.montlikadani.tablist.bungee.tablist;

import java.util.List;
import java.util.UUID;

import hu.montlikadani.tablist.bungee.Misc;
import hu.montlikadani.tablist.bungee.TabList;
import hu.montlikadani.tablist.bungee.config.ConfigConstants;
import hu.montlikadani.tablist.bungee.config.ConfigConstants.TabSetting;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

public class PlayerTab {

	private final TabList plugin;
	private final UUID playerUUID;

	private int i = 0, i2 = 0;

	private String[] header, footer;

	public PlayerTab(TabList plugin, UUID playerUUID) {
		this.plugin = plugin;
		this.playerUUID = playerUUID;
	}

	public UUID getUniqueId() {
		return playerUUID;
	}

	public ProxiedPlayer getPlayer() {
		return plugin.getProxy().getPlayer(playerUUID);
	}

	public String[] getHeader() {
		return header;
	}

	public String[] getFooter() {
		return footer;
	}

	public String getNextHeader() {
		if (i < header.length - 1) {
			i++;
		} else {
			i = 0;
		}

		return (i < 0 || i >= header.length) ? "" : header[i];
	}

	public String getNextFooter() {
		if (i2 < footer.length - 1) {
			i2++;
		} else {
			i2 = 0;
		}

		return (i2 < 0 || i2 >= footer.length) ? "" : footer[i2];
	}

	public void clearAll() {
		header = footer = null;
		getPlayer().resetTabHeader();
	}

	private String[] fill(String[] array, List<String> list) {
		if (list.isEmpty()) {
			return null;
		}

		int size = list.size();

		if (array == null) {
			array = new String[size];
		}

		for (int a = 0; a < size; a++) {
			array[a] = list.get(a);
		}

		return array;
	}

	public void loadTabList() {
		clearAll();

		final ProxiedPlayer player = getPlayer();
		final String pName = player.getName();
		final String server = player.getServer() != null ? player.getServer().getInfo().getName() : "";

		if (!ConfigConstants.TAB_SETTINGS.isEmpty()) {
			for (String serverName : ConfigConstants.getPerServerColl()) {
				TabSetting setting = ConfigConstants.TAB_SETTINGS.get(serverName);

				if (setting == null) {
					continue;
				}

				per: for (String split : setting.getNames()) {
					if (server.equalsIgnoreCase(split)) {
						header = fill(header, ConfigConstants.getPerServerSection()
								.getStringList(serverName + ".per-player." + pName + ".header"));

						if (header == null)
							header = fill(header, setting.getHeader());

						footer = fill(footer, ConfigConstants.getPerServerSection()
								.getStringList(serverName + ".per-player." + pName + ".footer"));

						if (footer == null)
							footer = fill(footer, setting.getFooter());

						break per;
					}
				}
			}

			pl: for (String one : ConfigConstants.getPerPlayerColl()) {
				TabSetting setting = ConfigConstants.TAB_SETTINGS.get(one);

				if (setting == null) {
					continue;
				}

				for (String split : setting.getNames()) {
					if (pName.equalsIgnoreCase(split)) {
						header = fill(header, setting.getHeader());
						footer = fill(footer, setting.getFooter());
						break pl;
					}
				}
			}
		}

		if (header != null && footer != null) {
			return;
		}

		final Configuration conf = plugin.getConf();

		if (header == null)
			header = fill(header,
					conf.getStringList("tablist.per-server." + server + ".per-player." + pName + ".header"));

		if (header == null)
			header = fill(header, conf.getStringList("tablist.per-server." + server + ".header"));

		if (header == null)
			header = fill(header, conf.getStringList("tablist.per-player." + pName + ".header"));

		if (header == null)
			header = fill(header, ConfigConstants.getDefaultHeader());

		if (footer == null)
			footer = fill(footer,
					conf.getStringList("tablist.per-server." + server + ".per-player." + pName + ".footer"));

		if (footer == null)
			footer = fill(footer, conf.getStringList("tablist.per-server." + server + ".footer"));

		if (footer == null)
			footer = fill(footer, conf.getStringList("tablist.per-player." + pName + ".footer"));

		if (footer == null)
			footer = fill(footer, ConfigConstants.getDefaultFooter());
	}

	public void update() {
		final ProxiedPlayer player = getPlayer();

		if ((player.getServer() != null
				&& ConfigConstants.getDisabledServers().contains(player.getServer().getInfo().getName()))
				|| ConfigConstants.getRestrictedPlayers().contains(player.getName())) {
			player.resetTabHeader();
			return;
		}

		player.setTabHeader(Misc.getComponentBuilder(Misc.replaceVariables(getNextHeader(), player)),
				Misc.getComponentBuilder(Misc.replaceVariables(getNextFooter(), player)));
	}
}
