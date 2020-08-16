package hu.montlikadani.tablist.sponge;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

public class Variables {

	public Text replaceVariables(Player p, String str) {
		int staffs = 0;
		if (str.contains("%staff-online%")) {
			for (Player all : Sponge.getServer().getOnlinePlayers()) {
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
		DecimalFormat tpsformat = new DecimalFormat("#0.00");
		if (str.contains("%tps%")) {
			str = str.replace("%tps%", String.valueOf(tpsformat.format(Sponge.getServer().getTicksPerSecond())));
		}

		String t = null, dt = null;
		if (str.contains("%server-time%") || str.contains("%date%")) {
			DateTimeFormatter form = DateTimeFormatter.ofPattern(ConfigValues.getTimeFormat()),
					form2 = DateTimeFormatter.ofPattern(ConfigValues.getDateFormat());
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

		str = setSymbols(str);

		if (str.contains("%player%")) {
			str = str.replace("%player%", p.getName());
		}

		if (str.contains("%player-ping%")) {
			str = str.replace("%player-ping%", formatPing(p.getConnection().getLatency()));
		}

		if (str.contains("%player-uuid%")) {
			str = str.replace("%player-uuid%", p.getUniqueId().toString());
		}

		if (str.contains("%player-level%")) {
			str = str.replace("%player-level%", String.valueOf(p.get(Keys.EXPERIENCE_LEVEL).get()));
		}

		if (str.contains("%player-total-level%")) {
			str = str.replace("%player-total-level%", String.valueOf(p.get(Keys.TOTAL_EXPERIENCE).get()));
		}

		if (str.contains("%world%")) {
			str = str.replace("%world%", p.getWorld().getName());
		}

		if (str.contains("%player-health%")) {
			str = str.replace("%player-health%", String.valueOf(p.getHealthData().health().get()));
		}

		if (str.contains("%player-max-health%")) {
			str = str.replace("%player-max-health%", String.valueOf(p.getHealthData().maxHealth().get()));
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
			str = str.replace("%online-players%", Integer.toString(Sponge.getServer().getOnlinePlayers().size()));

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

	private String formatPing(int ping) {
		StringBuilder ret, sb = new StringBuilder();

		if (ConfigValues.isPingFormatEnabled()) {
			if (ping <= ConfigValues.getGoodPingAmount()) {
				ret = sb.append(ConfigValues.getGoodPingColor().replace('&', '\u00a7')).append(ping);
			} else if (ping <= ConfigValues.getMediumPingAmount()) {
				ret = sb.append(ConfigValues.getMediumPingColor().replace('&', '\u00a7')).append(ping);
			} else {
				ret = sb.append(ConfigValues.getBadPingColor().replace('&', '\u00a7')).append(ping);
			}
		} else {
			ret = sb.append(ping);
		}

		return ret.toString();
	}

	public String setSymbols(String s) {
		s = s.replace("<0>", "â€¢");
		s = s.replace("<1>", "âž¤");
		s = s.replace("<2>", "â„¢");
		s = s.replace("<3>", "â†‘");
		s = s.replace("<4>", "â†’");
		s = s.replace("<5>", "â†“");
		s = s.replace("<6>", "âˆž");
		s = s.replace("<7>", "â–‘");
		s = s.replace("<8>", "â–²");
		s = s.replace("<9>", "â–¶");
		s = s.replace("<10>", "â—€");
		s = s.replace("<11>", "â—�");
		s = s.replace("<12>", "â˜…");
		s = s.replace("<13>", "â˜†");
		s = s.replace("<14>", "â˜�");
		s = s.replace("<15>", "â˜‘");
		s = s.replace("<16>", "â˜ ");
		s = s.replace("<17>", "â˜¢");
		s = s.replace("<18>", "â˜£");
		s = s.replace("<19>", "â˜¹");
		s = s.replace("<20>", "â˜º");
		s = s.replace("<21>", "âœ“");
		s = s.replace("<22>", "âœ”");
		s = s.replace("<23>", "âœ˜");
		s = s.replace("<24>", "âœš");
		s = s.replace("<25>", "â„»");
		s = s.replace("<26>", "âœ ");
		s = s.replace("<27>", "âœ¡");
		s = s.replace("<28>", "âœ¦");
		s = s.replace("<29>", "âœ§");
		s = s.replace("<30>", "âœ©");
		s = s.replace("<31>", "âœª");
		s = s.replace("<32>", "âœ®");
		s = s.replace("<33>", "âœ¯");
		s = s.replace("<34>", "ã‹¡");
		s = s.replace("<35>", "â��");
		s = s.replace("<36>", "â�ž");
		s = s.replace("<37>", "ãƒ„");
		s = s.replace("<38>", "â™©");
		s = s.replace("<39>", "â™ª");
		s = s.replace("<40>", "â™«");
		s = s.replace("<41>", "â™¬");
		s = s.replace("<42>", "â™­");
		s = s.replace("<43>", "â™®");
		s = s.replace("<44>", "â™¯");
		s = s.replace("<45>", "Â¶");
		s = s.replace("<46>", "\u00A9");
		s = s.replace("<47>", "\u00AE");
		s = s.replace("<48>", "â�Ž");
		s = s.replace("<49>", "â‡§");
		s = s.replace("<50>", "â‡ª");
		s = s.replace("<51>", "á´´á´°");
		s = s.replace("<52>", "â˜’");
		s = s.replace("<53>", "â™ ");
		s = s.replace("<54>", "â™£");
		s = s.replace("<55>", "â˜»");
		s = s.replace("<56>", "â–“");
		s = s.replace("<57>", "âž¾");
		s = s.replace("<58>", "âž”");
		s = s.replace("<59>", "âž³");
		s = s.replace("<60>", "âž§");
		s = s.replace("<61>", "ã€Š");
		s = s.replace("<62>", "ã€‹");
		s = s.replace("<63>", "ï¸¾");
		s = s.replace("<64>", "ï¸½");
		s = s.replace("<65>", "â˜ƒ");
		s = s.replace("<66>", "Â¹");
		s = s.replace("<67>", "Â²");
		s = s.replace("<68>", "Â³");
		s = s.replace("<69>", "â‰ˆ");
		s = s.replace("<70>", "â„ ");
		s = s.replace("<71>", "\u2665");
		s = s.replace("<72>", "âœ¬");
		s = s.replace("<73>", "â†”");
		s = s.replace("<74>", "Â«");
		s = s.replace("<75>", "Â»");
		s = s.replace("<76>", "â˜€");
		s = s.replace("<77>", "â™¦");
		s = s.replace("<78>", "â‚½");
		s = s.replace("<79>", "â˜Ž");
		s = s.replace("<80>", "â˜‚");
		s = s.replace("<81>", "â†�");
		s = s.replace("<82>", "â†–");
		s = s.replace("<83>", "â†—");
		s = s.replace("<84>", "â†˜");
		s = s.replace("<85>", "â†™");
		s = s.replace("<86>", "âž²");
		s = s.replace("<87>", "âœ�");
		s = s.replace("<88>", "âœŽ");
		s = s.replace("<89>", "âœ�");
		s = s.replace("<90>", "âœ†");
		s = s.replace("<91>", "â—„");
		s = s.replace("<92>", "â˜¼");
		s = s.replace("<93>", "â–º");
		s = s.replace("<94>", "â†•");
		s = s.replace("<95>", "â–¼");
		s = s.replace("<96>", "â‘ ");
		s = s.replace("<97>", "â‘¡");
		s = s.replace("<98>", "â‘¢");
		s = s.replace("<99>", "â‘£");
		s = s.replace("<100>", "â‘¤");
		s = s.replace("<101>", "â‘¥");
		s = s.replace("<102>", "â‘¦");
		s = s.replace("<103>", "â‘§");
		s = s.replace("<104>", "â‘¨");
		s = s.replace("<105>", "â‘©");
		s = s.replace("<106>", "â‘ª");
		s = s.replace("<107>", "â‘«");
		s = s.replace("<108>", "â‘¬");
		s = s.replace("<109>", "â‘­");
		s = s.replace("<110>", "â‘®");
		s = s.replace("<111>", "â‘¯");
		s = s.replace("<112>", "â‘°");
		s = s.replace("<113>", "â‘±");
		s = s.replace("<114>", "â‘²");
		s = s.replace("<115>", "â‘³");
		s = s.replace("<116>", "â™¨");
		s = s.replace("<117>", "âœ‘");
		s = s.replace("<118>", "âœ–");
		s = s.replace("<119>", "âœ°");
		s = s.replace("<120>", "âœ¶");
		s = s.replace("<121>", "â•—");
		s = s.replace("<122>", "â•£");
		s = s.replace("<123>", "â—™");
		s = s.replace("<124>", "â—‹");
		s = s.replace("<125>", "â• ");
		s = s.replace("<126>", "â”¤");
		s = s.replace("<127>", "â•‘");
		s = s.replace("<128>", "â•�");
		s = s.replace("<129>", "âŒ‚");
		s = s.replace("<130>", "â”�");
		s = s.replace("<131>", "â�‰");
		s = s.replace("<132>", "âŒ²");
		s = s.replace("<133>", "Â½");
		s = s.replace("<134>", "Â¼");
		s = s.replace("<135>", "Â¾");
		s = s.replace("<136>", "â…“");
		s = s.replace("<137>", "â…”");
		s = s.replace("<138>", "â„–");
		s = s.replace("<139>", "â€ ");
		s = s.replace("<140>", "â€¡");
		s = s.replace("<141>", "Âµ");
		s = s.replace("<142>", "Â¢");
		s = s.replace("<143>", "Â£");
		s = s.replace("<144>", "âˆ…");
		s = s.replace("<145>", "â‰¤");
		s = s.replace("<146>", "â‰¥");
		s = s.replace("<147>", "â‰ ");
		s = s.replace("<148>", "âˆ§");
		s = s.replace("<149>", "âˆ¨");
		s = s.replace("<150>", "âˆ©");
		s = s.replace("<151>", "âˆª");
		s = s.replace("<152>", "âˆˆ");
		s = s.replace("<153>", "âˆ€");
		s = s.replace("<154>", "âˆƒ");
		s = s.replace("<155>", "âˆ„");
		s = s.replace("<156>", "âˆ‘");
		s = s.replace("<157>", "âˆ�");
		s = s.replace("<158>", "â†º");
		s = s.replace("<159>", "â†»");
		s = s.replace("<160>", "Î©");

		return s;
	}
}
