package hu.montlikadani.tablist.bungee;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.bungee.config.ConfigConstants;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public final class Misc {

	public static String colorMsg(String s) {
		if (s == null) {
			return "";
		}

		if (s.indexOf("#") >= 0) {
			s = Global.matchColorRegex(s);
		}

		return ChatColor.translateAlternateColorCodes('&', s);
	}

	public static void sendMessage(CommandSender s, String text) {
		if (s != null && text != null && !text.trim().isEmpty()) {
			s.sendMessage(getComponentBuilder(colorMsg(text)));
		}
	}

	public static BaseComponent[] getComponentBuilder(String s) {
		return new ComponentBuilder(s).create();
	}

	@SuppressWarnings("deprecation")
	public static String replaceVariables(String str, ProxiedPlayer p) {
		// TODO Remove or make more customisable variables
		for (java.util.Map.Entry<String, String> map : ConfigConstants.CUSTOM_VARIABLES.entrySet()) {
			str = str.replace(map.getKey(), map.getValue());
		}

		String time = str.indexOf("%time%") >= 0 ? getTimeAsString(ConfigConstants.getTimeFormat()) : "";
		String date = str.indexOf("%date%") >= 0 ? getTimeAsString(ConfigConstants.getDateFormat()) : "";

		if (!time.isEmpty())
			str = str.replace("%time%", time);

		if (!date.isEmpty())
			str = str.replace("%date%", date);

		ServerInfo info = p.getServer() != null ? p.getServer().getInfo() : null;

		if (info != null) {
			str = str.replace("%server%", info.getName());
			str = str.replace("%server-online%", Integer.toString(info.getPlayers().size()));
			str = str.replace("%bungee-motd%", info.getMotd());
		}

		if (str.indexOf("%max-players%") >= 0) {
			try {
				str = str.replace("%max-players%", Integer.toString(ProxyServer.getInstance().getConfigurationAdapter()
						.getListeners().iterator().next().getMaxPlayers()));
			} catch (Exception e) {
				// Ignore unknown errors
			}
		}

		if (str.indexOf("%ip%") >= 0) {
			InetSocketAddress address = null;
			SocketAddress sAddress = null;

			try {
				address = p.getAddress();
			} catch (Exception e) {
				sAddress = p.getSocketAddress();
			}

			str = str.replace("%ip%", address != null ? address.getAddress().getHostAddress()
					: sAddress != null ? sAddress.toString() : "");
		}

		if (str.indexOf("%player-language%") >= 0)
			str = str.replace("%player-language%",
					p.getLocale() == null ? "unknown" : p.getLocale().getDisplayLanguage());

		str = str.replace("%player-name%", p.getName());
		str = str.replace("%display-name%", p.getDisplayName());

		if (str.indexOf("%ping%") >= 0)
			str = str.replace("%ping%", Integer.toString(p.getPing()));

		Runtime r = Runtime.getRuntime();

		if (str.indexOf("%ram-used%") >= 0)
			str = str.replace("%ram-used%", Long.toString((r.totalMemory() - r.freeMemory()) / 1048576L));

		if (str.indexOf("%ram-max%") >= 0)
			str = str.replace("%ram-max%", Long.toString(r.maxMemory() / 1048576L));

		if (str.indexOf("%ram-free%") >= 0)
			str = str.replace("%ram-free%", Long.toString(r.freeMemory() / 1048576L));

		if (str.indexOf("%player-uuid%") >= 0)
			str = str.replace("%player-uuid%", p.getUniqueId().toString());

		if (str.indexOf("%bungee-online%") >= 0)
			str = str.replace("%bungee-online%", Integer.toString(ProxyServer.getInstance().getOnlineCount()));

		if (str.indexOf("%player-country%") >= 0)
			str = str.replace("%player-country%",
					p.getLocale() == null ? "unknown" : p.getLocale().getDisplayCountry());

		str = Global.setSymbols(str);

		return colorMsg(str);
	}

	private static String getTimeAsString(String pattern) {
		if (pattern.isEmpty()) {
			return pattern;
		}

		TimeZone zone = ConfigConstants.isUseSystemZone() ? TimeZone.getTimeZone(java.time.ZoneId.systemDefault())
				: TimeZone.getTimeZone(ConfigConstants.getTimeZone());
		LocalDateTime now = zone == null ? LocalDateTime.now() : LocalDateTime.now(zone.toZoneId());

		return now.format(DateTimeFormatter.ofPattern(pattern));
	}
}
