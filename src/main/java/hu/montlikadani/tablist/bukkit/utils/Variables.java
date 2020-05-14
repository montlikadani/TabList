package hu.montlikadani.tablist.bukkit.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;

import de.myzelyam.api.vanish.VanishAPI;
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
		java.util.Collection<? extends Player> oPls = Bukkit.getOnlinePlayers();

		if (plugin.getC().contains("custom-variables")) {
			for (String custom : plugin.getC().getConfigurationSection("custom-variables").getKeys(true)) {
				if (custom != null && str.contains(custom)) {
					str = str.replace(custom, plugin.getC().getString("custom-variables." + custom));
				}
			}
		}

		int staffs = 0;
		if (str.contains("%staff-online%")) {
			for (Player all : oPls) {
				if (!all.hasPermission("tablist.onlinestaff")) {
					continue;
				}

				if (!ConfigValues.isCountVanishedStaff()
						&& ((plugin.isPluginEnabled("SuperVanish") && VanishAPI.isInvisible(all))
								|| (plugin.isPluginEnabled("Essentials")
										&& JavaPlugin.getPlugin(Essentials.class).getUser(all).isVanished()))) {
					continue;
				}

				staffs++;
			}
		}

		final int plSize = oPls.size();

		int abc = 0;
		if (ConfigValues.isIgnoreVanishedPlayers()) {
			if (plugin.isPluginEnabled("SuperVanish")) {
				abc = VanishAPI.getInvisiblePlayers().isEmpty() ? plSize
						: plSize - VanishAPI.getInvisiblePlayers().size();
			} else if (plugin.isPluginEnabled("Essentials")) {
				Essentials ess = JavaPlugin.getPlugin(Essentials.class);
				abc = ess.getVanishedPlayers().isEmpty() ? ess.getOnlinePlayers().size()
						: ess.getOnlinePlayers().size() - ess.getVanishedPlayers().size();
			} else {
				abc = plSize;
			}
		} else {
			abc = plSize;
		}

		String address = null;
		if (str.contains("%ip-address%")) {
			address = pl.getAddress().getAddress().toString();
			address = address.replaceAll("/", "");
		}

		String t = null;
		String dt = null;
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
		Long fram = Long.valueOf(r.freeMemory() / 1048576L);
		Long mram = Long.valueOf(r.maxMemory() / 1048576L);
		Long uram = Long.valueOf((r.totalMemory() - r.freeMemory()) / 1048576L);

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
			str = str.replace("%online-players%", Integer.toString(abc));

		if (str.contains("%max-players%"))
			str = str.replace("%max-players%", Integer.toString(Bukkit.getMaxPlayers()));

		if (str.contains("%servertype%"))
			str = str.replace("%servertype%", Bukkit.getServer().getName());

		if (plugin.isPluginEnabled("Essentials") && str.contains("%vanished-players%"))
			str = str.replace("%vanished-players%",
					Integer.toString(JavaPlugin.getPlugin(Essentials.class).getVanishedPlayers().size()));

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
			str = str.replace("%exp-to-level%", pl.getExpToLevel() + "");

		if (str.contains("%level%"))
			str = str.replace("%level%", pl.getLevel() + "");

		if (str.contains("%xp%"))
			str = str.replace("%xp%", pl.getExp() + "");

		if (str.contains("%tps%"))
			str = str.replace("%tps%", tpsDot(TabListAPI.getTPS()));

		if (str.contains("%light-level%")) {
			str = str.replace("%light-level%", Byte.toString(pl.getLocation().getBlock().getLightLevel()));
		}

		str = str.replace("\n", "\n");
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
			s = s.replace("%player-health%", String.valueOf(p.getHealth()));
		}

		if (s.contains("%player-max-health%")) {
			s = s.replace("%player-max-health%",
					Version.isCurrentLower(Version.v1_9_R1) ? String.valueOf(p.getMaxHealth())
							: String.valueOf(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue()));
		}

		return s;
	}

	private String tpsDot(double d) {
		String ds = formatTPS(d);
		if (ds.contains(".")) {
			// TODO: Do we need to configure the tps size?
			ds = ds.substring(0, ds.indexOf(".") + 2);
		}

		return ds;
	}

	private String formatPing(int ping) {
		StringBuilder ret;
		StringBuilder sb = new StringBuilder();

		if (ConfigValues.isPingFormatEnabled()) {
			if (ping <= ConfigValues.getGoodPingAmount()) {
				ret = sb.append(ConfigValues.getGoodPingColor().replace('&', '\u00a7')).append(ping)
						.append(ChatColor.RESET);
			} else if (ping <= ConfigValues.getMediumPingAmount()) {
				ret = sb.append(ConfigValues.getMediumPingColor().replace('&', '\u00a7')).append(ping)
						.append(ChatColor.RESET);
			} else {
				ret = sb.append(ConfigValues.getBadPingColor().replace('&', '\u00a7')).append(ping)
						.append(ChatColor.RESET);
			}
		} else {
			ret = sb.append(ping);
		}

		return ret.toString();
	}

	private String formatTPS(double tps) {
		StringBuilder ret;
		StringBuilder sb = new StringBuilder();

		if (ConfigValues.isTpsFormatEnabled()) {
			if (tps > ConfigValues.getGoodTpsAmount()) {
				ret = sb.append(ConfigValues.getGoodTpsColor().replace('&', '\u00a7')).append(tps)
						.append(ChatColor.RESET);
			} else if (tps > ConfigValues.getMediumTpsAmount()) {
				ret = sb.append(ConfigValues.getMediumTpsColor().replace('&', '\u00a7')).append(tps)
						.append(ChatColor.RESET);
			} else {
				ret = sb.append(ConfigValues.getBadTpsColor().replace('&', '\u00a7')).append(tps)
						.append(ChatColor.RESET);
			}
		} else {
			ret = sb.append(tps);
		}

		return ret.toString();
	}
}