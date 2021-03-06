package hu.montlikadani.tablist.bungee;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;

import hu.montlikadani.tablist.Global;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

public class Misc {

	public static String colorMsg(String s) {
		if (s == null) {
			return "";
		}

		if (s.contains("#")) {
			s = Global.matchColorRegex(s);
		}

		return ChatColor.translateAlternateColorCodes('&', s);
	}

	public static void sendMessage(CommandSender s, String path) {
		if (s != null && path != null && !path.trim().isEmpty()) {
			s.sendMessage(getComponentBuilder(colorMsg(path)));
		}
	}

	public static BaseComponent[] getComponentBuilder(String s) {
		return new ComponentBuilder(s).create();
	}

	@SuppressWarnings("deprecation")
	public static String replaceVariables(String str, ProxiedPlayer p) {
		Runtime r = Runtime.getRuntime();
		Long fram = r.freeMemory() / 1048576L,
				mram = r.maxMemory() / 1048576L,
				uram = (r.totalMemory() - r.freeMemory()) / 1048576L;

		Configuration conf = TabList.getInstance().getConf();

		if (conf.contains("custom-variables")) {
			for (String custom : conf.getSection("custom-variables").getKeys()) {
				if (str.contains(custom)) {
					str = str.replace(custom, conf.getString("custom-variables." + custom));
				}
			}
		}

		String t = "", dt = "";
		if (str.contains("%time%") || str.contains("%date%")) {
			String path = "placeholder-format.time.";
			DateTimeFormatter form = !conf.getString(path + "time-format", "").isEmpty()
					? DateTimeFormatter.ofPattern(conf.getString(path + "time-format"))
					: null;

			DateTimeFormatter form2 = !conf.getString(path + "date-format", "").isEmpty()
					? DateTimeFormatter.ofPattern(conf.getString(path + "date-format"))
					: null;

			TimeZone zone = conf.getBoolean(path + "use-system-zone", false)
					? TimeZone.getTimeZone(java.time.ZoneId.systemDefault())
					: TimeZone.getTimeZone(conf.getString(path + "time-zone", "GMT0"));
			LocalDateTime now = zone == null ? LocalDateTime.now() : LocalDateTime.now(zone.toZoneId());

			Calendar cal = Calendar.getInstance();

			t = form != null ? now.format(form) : cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);
			dt = form2 != null ? now.format(form2) : cal.get(Calendar.YEAR) + "/" + cal.get(Calendar.DATE);
		}

		ServerInfo info = p.getServer() != null ? p.getServer().getInfo() : null;
		String online = info != null ? Integer.toString(info.getPlayers().size()) : "0";

		if (!t.isEmpty())
			str = str.replace("%time%", t);

		if (!dt.isEmpty())
			str = str.replace("%date%", dt);

		if (info != null && str.contains("%server%"))
			str = str.replace("%server%", info.getName());

		if (str.contains("%server-online%"))
			str = str.replace("%server-online%", online);

		try {
			if (str.contains("%max-players%")) {
				str = str.replace("%max-players%", Integer.toString(BungeeCord.getInstance().getConfigurationAdapter()
						.getListeners().iterator().next().getMaxPlayers()));
			}
		} catch (Exception e) {
			// Ignore unknown errors
		}

		if (str.contains("%ip%")) {
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

		if (str.contains("%player-language%"))
			str = str.replace("%player-language%",
					p.getLocale() == null ? "unknown" : p.getLocale().getDisplayLanguage());

		if (str.contains("%player-name%"))
			str = str.replace("%player-name%", p.getName());

		if (str.contains("%display-name%"))
			str = str.replace("%display-name%", p.getDisplayName());

		if (str.contains("%ping%"))
			str = str.replace("%ping%", Integer.toString(p.getPing()));

		if (str.contains("%ram-used%"))
			str = str.replace("%ram-used%", Long.toString(uram));

		if (str.contains("%ram-max%"))
			str = str.replace("%ram-max%", Long.toString(mram));

		if (str.contains("%ram-free%"))
			str = str.replace("%ram-free%", Long.toString(fram));

		if (str.contains("%player-uuid%"))
			str = str.replace("%player-uuid%", p.getUniqueId().toString());

		if (str.contains("%game-version%"))
			str = str.replace("%game-version%", BungeeCord.getInstance().getGameVersion());

		if (str.contains("%bungee-online%"))
			str = str.replace("%bungee-online%", Integer.toString(BungeeCord.getInstance().getOnlineCount()));

		if (info != null && str.contains("%bungee-motd%"))
			str = str.replace("%bungee-motd%", info.getMotd());

		if (str.contains("%player-country%"))
			str = str.replace("%player-country%",
					p.getLocale() == null ? "unknown" : p.getLocale().getDisplayCountry());

		str = str.replace("\\n", "\n");
		str = Global.setSymbols(str);

		return colorMsg(str);
	}
}
