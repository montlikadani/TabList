package hu.montlikadani.tablist.Sponge;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import hu.montlikadani.tablist.Global;

public class Variables {

	private TabList plugin;

	public Variables(TabList plugin) {
		this.plugin = plugin;
	}

	public Text replaceVariables(Player p, String str) {
		ConfigManager conf = plugin.getConfig().getConfig();
		java.util.Collection<? extends Player> oPls = Sponge.getServer().getOnlinePlayers();

		if (conf.contains("custom-variables")) {
			for (String custom : conf.getStringList("custom-variables")) {
				if (custom != null && str.contains(custom)) {
					str = str.replace(custom, conf.getString("custom-variables", custom));
				}
			}
		}

		int staffs = 0;
		if (str.contains("%staff-online%")) {
			for (Player all : oPls) {
				if (all.hasPermission("tablist.onlinestaff")) {
					staffs++;
				}
			}
		}

		String address = null;
		if (str.contains("%ip-address%")) {
			address = p.getConnection().getAddress().getAddress().toString();
			address = address.replaceAll("/", "");
		}

		String t = null;
		String dt = null;
		if (str.contains("%server-time%") || str.contains("%date%")) {
			Object path = new Object[] { "placeholder-format", "time" };
			DateTimeFormatter form = conf.isStringExistsAndNotEmpty(path, "time-format", "format")
					? DateTimeFormatter.ofPattern(Config.getTimeFormat())
					: null;

			DateTimeFormatter form2 = conf.isStringExistsAndNotEmpty(path, "date-format", "format")
					? DateTimeFormatter.ofPattern(Config.getDateFormat())
					: null;

			TimeZone zone = Config.isUsingSystemTimeZone() ? TimeZone.getTimeZone(java.time.ZoneId.systemDefault())
					: TimeZone.getTimeZone(Config.getTimeZone());
			LocalDateTime now = zone == null ? LocalDateTime.now() : LocalDateTime.now(zone.toZoneId());

			Calendar cal = Calendar.getInstance();

			t = form != null ? now.format(form) : cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);
			dt = form2 != null ? now.format(form2) : cal.get(Calendar.YEAR) + "/" + cal.get(Calendar.DATE);
		}

		Runtime r = Runtime.getRuntime();
		Long fram = Long.valueOf(r.freeMemory() / 1048576L);
		Long mram = Long.valueOf(r.maxMemory() / 1048576L);
		Long uram = Long.valueOf((r.totalMemory() - r.freeMemory()) / 1048576L);

		str = Global.setSymbols(str);

		if (str.contains("%player%")) {
			str = str.replace("%player%", p.getName());
		}

		if (str.contains("%player-uuid%")) {
			str = str.replace("%player-uuid%", p.getUniqueId().toString());
		}

		if (str.contains("%world%")) {
			str = str.replace("%world%", p.getWorld().getName());
		}

		if (str.contains("%player-health%")) {
			str = str.replace("%player-health%", String.valueOf(p.getHealthData().health()));
		}

		if (str.contains("%player-max-health%")) {
			str = str.replace("%player-max-health%", String.valueOf(p.getHealthData().maxHealth()));
		}

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
			str = str.replace("%online-players%", Integer.toString(oPls.size()));

		if (str.contains("%max-players%"))
			str = str.replace("%max-players%", Integer.toString(Sponge.getServer().getMaxPlayers()));

		if (str.contains("%servertype%"))
			str = str.replace("%servertype%", Sponge.getGame().getPlatform().getType().name());

		if (str.contains("%staff-online%")) {
			str = str.replace("%staff-online%", Integer.toString(staffs));
		}

		if (address != null)
			str = str.replace("%ip-address%", address);

		if (str.contains("%mc-version%"))
			str = str.replace("%mc-version%", Sponge.getGame().getPlatform().getMinecraftVersion().getName());

		if (str.contains("%motd%"))
			str = str.replace("%motd%", Sponge.getServer().getMotd().toPlain());

		str = str.replace("\n", "\n");
		return TextSerializers.FORMATTING_CODE.deserialize(str);
	}
}
