package hu.montlikadani.tablist.bukkit.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.ConfigValues;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;
import me.clip.placeholderapi.PlaceholderAPI;

public class Variables {

	private TabList plugin;

	public Variables(TabList plugin) {
		this.plugin = plugin;
	}

	public String replaceVariables(Player pl, String str) {
		if (plugin.getC().contains("custom-variables")) { // old
			for (String custom : plugin.getC().getConfigurationSection("custom-variables").getKeys(true)) {
				if (str.contains(custom)) {
					str = str.replace(custom, plugin.getC().getString("custom-variables." + custom));
				}
			}
		}

		if (plugin.getTabC().contains("custom-variables")) {
			for (String custom : plugin.getTabC().getConfigurationSection("custom-variables").getKeys(true)) {
				if (str.contains(custom)) {
					str = str.replace(custom, plugin.getTabC().getString("custom-variables." + custom));
				}
			}
		}

		int staffs = 0;
		if (str.contains("%staff-online%")) {
			for (Player all : Bukkit.getOnlinePlayers()) {
				if (!all.hasPermission("tablist.onlinestaff")
						|| (!ConfigValues.isCountVanishedStaff() && PluginUtils.isVanished(all))) {
					continue;
				}

				staffs++;
			}
		}

		String address = null;
		if (str.contains("%ip-address%")) {
			address = pl.getAddress().getAddress().toString();
			address = address.replaceAll("/", "");
		}

		String t = null, dt = null;
		if (str.contains("%server-time%") || str.contains("%date%")) {
			DateTimeFormatter form = !ConfigValues.getTimeFormat().isEmpty()
					? DateTimeFormatter.ofPattern(ConfigValues.getTimeFormat())
					: null;

			DateTimeFormatter form2 = !ConfigValues.getDateFormat().isEmpty()
					? DateTimeFormatter.ofPattern(ConfigValues.getDateFormat())
					: null;

			TimeZone zone = ConfigValues.isUseSystemZone() ? TimeZone.getTimeZone(java.time.ZoneId.systemDefault())
					: TimeZone.getTimeZone(ConfigValues.getTimeZone());
			LocalDateTime now = zone == null ? LocalDateTime.now() : LocalDateTime.now(zone.toZoneId());

			Calendar cal = Calendar.getInstance();

			t = form != null ? now.format(form) : cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);
			dt = form2 != null ? now.format(form2) : cal.get(Calendar.YEAR) + "/" + cal.get(Calendar.DATE);
		}

		Runtime r = Runtime.getRuntime();
		Long fram = Long.valueOf(r.freeMemory() / 1048576L),
				mram = Long.valueOf(r.maxMemory() / 1048576L),
				uram = Long.valueOf((r.totalMemory() - r.freeMemory()) / 1048576L);

		str = setPlaceholders(pl, str);
		str = Global.setSymbols(str);
		if (t != null)
			str = str.replace("%server-time%", t);

		if (dt != null)
			str = str.replace("%date%", dt);

		if (str.contains("%server-ram-free%"))
			str = str.replace("%server-ram-free%", Long.toString(fram.longValue()));

		if (str.contains("%server-ram-max%"))
			str = str.replace("%server-ram-max%", Long.toString(mram.longValue()));

		if (str.contains("%server-ram-used%"))
			str = str.replace("%server-ram-used%", Long.toString(uram.longValue()));

		if (str.contains("%online-players%"))
			str = str.replace("%online-players%", Integer.toString(PluginUtils.countVanishedPlayers()));

		if (str.contains("%max-players%"))
			str = str.replace("%max-players%", Integer.toString(Bukkit.getMaxPlayers()));

		if (str.contains("%servertype%"))
			str = str.replace("%servertype%", Bukkit.getServer().getName());

		if (str.contains("%vanished-players%"))
			str = str.replace("%vanished-players%", Integer.toString(PluginUtils.getVanishedPlayers()));

		if (str.contains("%ping%"))
			str = str.replace("%ping%", formatPing(TabListAPI.getPing(pl)));

		if (str.contains("%staff-online%")) {
			str = str.replace("%staff-online%", Integer.toString(staffs));
		}

		if (address != null)
			str = str.replace("%ip-address%", address);

		if (str.contains("%mc-version%"))
			str = str.replace("%mc-version%", Bukkit.getBukkitVersion());

		if (str.contains("%motd%"))
			str = str.replace("%motd%", Bukkit.getServer().getMotd());

		if (str.contains("%exp-to-level%"))
			str = str.replace("%exp-to-level%", Integer.toString(pl.getExpToLevel()));

		if (str.contains("%level%"))
			str = str.replace("%level%", Integer.toString(pl.getLevel()));

		if (str.contains("%xp%"))
			str = str.replace("%xp%", Float.toString(pl.getExp()));

		if (str.contains("%tps%"))
			str = str.replace("%tps%", tpsDot(TabListAPI.getTPS()));

		if (str.contains("%light-level%")) {
			str = str.replace("%light-level%", Byte.toString(pl.getLocation().getBlock().getLightLevel()));
		}

		if (str.contains("\n")) {
			str = str.replace("\n", "\n");
		}

		return Util.colorMsg(str);
	}

	@SuppressWarnings("deprecation")
	public String setPlaceholders(Player p, String s) {
		if (ConfigValues.isPlaceholderAPI() && plugin.isPluginEnabled("PlaceholderAPI")
				&& PlaceholderAPI.containsPlaceholders(s)) {
			s = PlaceholderAPI.setPlaceholders(p, s);
		}

		if (s.contains("%player%")) {
			s = s.replace("%player%", p.getName());
		}

		if (s.contains("%player-displayname%")) {
			s = s.replace("%player-displayname%", p.getDisplayName());
		}

		if (s.contains("%player-uuid%")) {
			s = s.replace("%player-uuid%", p.getUniqueId().toString());
		}

		if (s.contains("%world%")) {
			s = s.replace("%world%", p.getWorld().getName());
		}

		if (s.contains("%player-gamemode%")) {
			s = s.replace("%player-gamemode%", p.getGameMode().name());
		}

		if (s.contains("%player-health%")) {
			s = s.replace("%player-health%", Double.toString(p.getHealth()));
		}

		if (s.contains("%player-max-health%")) {
			s = s.replace("%player-max-health%",
					Double.toString(Version.isCurrentLower(Version.v1_9_R1) ? p.getMaxHealth()
							: p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue()));
		}

		return s;
	}

	private String tpsDot(double d) {
		String ds = formatTPS(d);
		if (ds.contains(".")) {
			int size = ConfigValues.getTpsSize();
			ds = ds.substring(0, (size == 1 ? 3 : ds.indexOf(".")) + (size < 1 ? 2 : size));
		}

		return ds;
	}

	private String formatPing(int ping) {
		StringBuilder sb2 = new StringBuilder();

		if (ConfigValues.isPingFormatEnabled()) {
			if (ping <= ConfigValues.getGoodPingAmount()) {
				return sb2.append(ConfigValues.getGoodPingColor().replace('&', '\u00a7')).append(ping)
						.append(ChatColor.RESET).toString();
			} else if (ping <= ConfigValues.getMediumPingAmount()) {
				return sb2.append(ConfigValues.getMediumPingColor().replace('&', '\u00a7')).append(ping)
						.append(ChatColor.RESET).toString();
			} else {
				return sb2.append(ConfigValues.getBadPingColor().replace('&', '\u00a7')).append(ping)
						.append(ChatColor.RESET).toString();
			}
		}

		return sb2.append(ping).toString();
	}

	private String formatTPS(double tps) {
		StringBuilder sb = new StringBuilder();

		if (ConfigValues.isTpsFormatEnabled()) {
			if (tps > ConfigValues.getGoodTpsAmount()) {
				return sb.append(ConfigValues.getGoodTpsColor().replace('&', '\u00a7')).append(tps)
						.append(ChatColor.RESET).toString();
			} else if (tps > ConfigValues.getMediumTpsAmount()) {
				return sb.append(ConfigValues.getMediumTpsColor().replace('&', '\u00a7')).append(tps)
						.append(ChatColor.RESET).toString();
			} else {
				return sb.append(ConfigValues.getBadTpsColor().replace('&', '\u00a7')).append(tps)
						.append(ChatColor.RESET).toString();
			}
		}

		return sb.append(tps).toString();
	}
}