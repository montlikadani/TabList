package hu.montlikadani.tablist;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import hu.montlikadani.tablist.config.ConfigConstants;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

public final class Misc {

	public static final BaseComponent EMPTY_COMPONENT = new net.md_5.bungee.api.chat.TextComponent();
	public static final BaseComponent[] EMPTY_COMPONENT_ARRAY = {};

	private static final String MAX_PLAYERS;
	private static final net.md_5.bungee.api.plugin.Plugin PREMIUM_VANISH;

	static {
		java.util.Collection<net.md_5.bungee.api.config.ListenerInfo> coll = ProxyServer.getInstance().getConfigurationAdapter().getListeners();

		MAX_PLAYERS = coll.isEmpty() ? "0" : Integer.toString(coll.iterator().next().getMaxPlayers());
		PREMIUM_VANISH = ProxyServer.getInstance().getPluginManager().getPlugin("PremiumVanish");
	}

	public static String colorMsg(String s) {
		if (s.indexOf('#') != -1) {
			s = Global.matchHexColour(s);
		}

		return ChatColor.translateAlternateColorCodes('&', s);
	}

	public static void sendMessage(net.md_5.bungee.api.CommandSender sender, ConfigConstants.MessageKeys key) {
		BaseComponent component = ConfigConstants.getMessage(key);

		if (component != EMPTY_COMPONENT) {
			sender.sendMessage(component);
		}
	}

	public static BaseComponent getComponentOfText(String s) {
		return new ComponentBuilder(s).getComponent(0);
	}

	private static int countVanishedPlayers(ServerInfo serverInfo, int playersCount) {
		if (!ConfigConstants.isIgnoreVanishedPlayers() || PREMIUM_VANISH == null) {
			return playersCount;
		}

		int vanishedPlayers = 0;

		if (serverInfo == null) {
			vanishedPlayers = de.myzelyam.api.vanish.BungeeVanishAPI.getInvisiblePlayers().size();
		} else {
			for (java.util.UUID id : de.myzelyam.api.vanish.BungeeVanishAPI.getInvisiblePlayers()) {
				ProxiedPlayer player = ProxyServer.getInstance().getPlayer(id);

				if (player == null) {
					continue;
				}

				Server server = player.getServer();

				if (server != null && serverInfo.equals(server.getInfo())) {
					vanishedPlayers++;
				}
			}
		}

		return vanishedPlayers == 0 ? playersCount : playersCount - vanishedPlayers;
	}

	private final static long MB = 1024 * 1024;

	@SuppressWarnings("deprecation")
	public static String replaceVariables(String str, ProxiedPlayer p) {
		// TODO Remove or make more customisable variables
		for (java.util.Map.Entry<String, String> map : ConfigConstants.CUSTOM_VARIABLES.entrySet()) {
			str = str.replace(map.getKey(), map.getValue());
		}

		if (ConfigConstants.getTimeFormat() != null) {
			str = Global.replace(str, "%time%", () -> getTimeAsString(ConfigConstants.getTimeFormat()));
		}

		if (ConfigConstants.getDateFormat() != null) {
			str = Global.replace(str, "%date%", () -> getTimeAsString(ConfigConstants.getDateFormat()));
		}

		Server server = p.getServer();
		ServerInfo info = server != null ? server.getInfo() : null;

		if (info != null) {
			str = str.replace("%server%", info.getName());
			str = str.replace("%bungee-motd%", info.getMotd());
			str = Global.replace(str, "%player-server-online%", () -> Integer.toString(countVanishedPlayers(info, info.getPlayers().size())));
		}

		str = Global.replace(str, "%ip%", () -> {
			InetSocketAddress address = null;
			SocketAddress sAddress = null;

			try {
				address = p.getAddress();
			} catch (Exception e) {
				sAddress = p.getSocketAddress();
			}

			return address != null ? address.getAddress().getHostAddress() : sAddress != null ? sAddress.toString() : "";
		});

		str = str.replace("%max-players%", MAX_PLAYERS);
		str = str.replace("%player-name%", p.getName());
		str = str.replace("%display-name%", p.getDisplayName());

		str = Global.replace(str, "%bungee-online%", () -> Integer.toString(countVanishedPlayers(null, ProxyServer.getInstance().getOnlineCount())));
		str = Global.replace(str, "%ping%", () -> ConfigConstants.formatPing(p.getPing()));

		str = Global.replace(str, "%ram-used%", () -> {
			Runtime runtime = Runtime.getRuntime();
			return Long.toString((runtime.totalMemory() - runtime.freeMemory()) / MB);
		});

		str = Global.replace(str, "%ram-max%", () -> Long.toString(Runtime.getRuntime().maxMemory() / MB));
		str = Global.replace(str, "%ram-free%", () -> Long.toString(Runtime.getRuntime().freeMemory() / MB));
		str = Global.replace(str, "%player-uuid%", () -> p.getUniqueId().toString());

		str = Global.replace(str, "%player-language%", () -> {
			Locale locale = p.getLocale();
			return locale == null ? "unknown" : locale.getDisplayLanguage();
		});

		str = Global.replace(str, "%player-country%", () -> {
			Locale locale = p.getLocale();
			return locale == null ? "unknown" : locale.getDisplayCountry();
		});

		return str;
	}

	private static String getTimeAsString(DateTimeFormatter formatterPattern) {
		return (ConfigConstants.getTimeZone() == null ? LocalDateTime.now() : LocalDateTime.now(ConfigConstants.getTimeZone().toZoneId())).format(formatterPattern);
	}
}
