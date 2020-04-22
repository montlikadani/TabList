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
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;
import me.clip.placeholderapi.PlaceholderAPI;

public class Variables {

	private TabList plugin;

	public Variables(TabList plugin) {
		this.plugin = plugin;
	}

	public String replaceVariables(Player pl, String str) {
		org.bukkit.configuration.file.FileConfiguration conf = plugin.getC();
		java.util.Collection<? extends Player> oPls = Bukkit.getOnlinePlayers();

		if (conf.contains("custom-variables")) {
			for (String custom : conf.getConfigurationSection("custom-variables").getKeys(true)) {
				if (custom != null && str.contains(custom)) {
					str = str.replace(custom, conf.getString("custom-variables." + custom));
				}
			}
		}

		int staffs = 0;
		if (str.contains("%staff-online%")) {
			for (Player all : oPls) {
				if (!all.hasPermission("tablist.onlinestaff")) {
					continue;
				}

				if (!conf.getBoolean("count-vanished-staffs")
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
		if (conf.getBoolean("ignore-vanished-players-in-online-players")) {
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
			String path = "placeholder-format.time.";
			DateTimeFormatter form = !conf.getString(path + "time-format.format").isEmpty()
					? DateTimeFormatter.ofPattern(conf.getString(path + "time-format.format"))
					: null;

			DateTimeFormatter form2 = !conf.getString(path + "date-format.format").isEmpty()
					? DateTimeFormatter.ofPattern(conf.getString(path + "date-format.format"))
					: null;

			TimeZone zone = conf.getBoolean(path + "use-system-zone", false)
					? TimeZone.getTimeZone(java.time.ZoneId.systemDefault())
					: TimeZone.getTimeZone(conf.getString(path + "time-zone", "GMT0"));
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
		if (plugin.hasPapi() && plugin.isPluginEnabled("PlaceholderAPI") && PlaceholderAPI.containsPlaceholders(s)) {
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
		String path = "placeholder-format.ping.";

		StringBuilder sb = new StringBuilder();

		if (plugin.getC().getBoolean(path + "enable")) {
			if (ping <= plugin.getC().getInt(path + "good-ping.amount")) {
				ret = sb.append(plugin.getC().getString(path + "good-ping.color").replace('&', '\u00a7')).append(ping)
						.append(ChatColor.RESET);
			} else if (ping <= plugin.getC().getInt(path + "medium-ping.amount")) {
				ret = sb.append(plugin.getC().getString(path + "medium-ping.color").replace('&', '\u00a7')).append(ping)
						.append(ChatColor.RESET);
			} else {
				ret = sb.append(plugin.getC().getString(path + "bad-ping").replace('&', '\u00a7')).append(ping)
						.append(ChatColor.RESET);
			}
		} else {
			ret = sb.append(ping);
		}

		return ret.toString();
	}

	private String formatTPS(double tps) {
		StringBuilder ret;
		String path = "placeholder-format.tps.";

		StringBuilder sb = new StringBuilder();

		if (plugin.getC().getBoolean(path + "enable")) {
			if (tps > plugin.getC().getDouble(path + "good-tps.amount")) {
				ret = sb.append(plugin.getC().getString(path + "good-tps.color").replace('&', '\u00a7')).append(tps)
						.append(ChatColor.RESET);
			} else if (tps > plugin.getC().getDouble(path + "medium-tps.amount")) {
				ret = sb.append(plugin.getC().getString(path + "medium-tps.color").replace('&', '\u00a7')).append(tps)
						.append(ChatColor.RESET);
			} else {
				ret = sb.append(plugin.getC().getString(path + "bad-tps").replace('&', '\u00a7')).append(tps)
						.append(ChatColor.RESET);
			}
		} else {
			ret = sb.append(tps);
		}

		return ret.toString();
	}
}