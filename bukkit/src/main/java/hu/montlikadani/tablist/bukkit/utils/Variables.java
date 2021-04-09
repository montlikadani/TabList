package hu.montlikadani.tablist.bukkit.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.utils.operators.ExpressionNode;
import hu.montlikadani.tablist.bukkit.utils.operators.OperatorNodes;
import hu.montlikadani.tablist.bukkit.utils.operators.OperatorNodes.NodeType;
import me.clip.placeholderapi.PlaceholderAPI;

public class Variables {

	private TabList plugin;

	private final List<ExpressionNode> nodes = new ArrayList<>();

	public Variables(TabList plugin) {
		this.plugin = plugin;
	}

	public void loadExpressions() {
		nodes.clear();

		if (ConfigValues.isPingFormatEnabled()) {
			for (String f : ConfigValues.getPingColorFormats()) {
				ExpressionNode node = new OperatorNodes(NodeType.PING);
				node.setParseExpression(f);
				nodes.add(node);
			}
		}

		if (ConfigValues.isTpsFormatEnabled()) {
			for (String f : ConfigValues.getTpsColorFormats()) {
				ExpressionNode node = new OperatorNodes(NodeType.TPS);
				node.setParseExpression(f);
				nodes.add(node);
			}
		}

		// Sort
		// ping in descending order
		// tps in ascending order
		for (int i = 0; i < nodes.size(); i++) {
			for (int j = nodes.size() - 1; j > i; j--) {
				ExpressionNode node = nodes.get(i), node2 = nodes.get(j);
				if ((node.getType() == NodeType.PING && node2.getType() == NodeType.PING
						&& node.getCondition().getSecondCondition() < node2.getCondition().getSecondCondition())
						|| (node.getType() == NodeType.TPS && node2.getType() == NodeType.TPS && node.getCondition()
								.getSecondCondition() > node2.getCondition().getSecondCondition())) {
					nodes.set(i, node2);
					nodes.set(j, node);
				}
			}
		}
	}

	public String replaceVariables(Player pl, String str) {
		if (str.isEmpty()) {
			return str;
		}

		Runtime r = Runtime.getRuntime();

		if (!ConfigValues.getMemoryBarChar().isEmpty() && str.indexOf("%memory_bar%") >= 0) {
			StringBuilder builder = new StringBuilder();

			int barSize = ConfigValues.getMemoryBarSize(),
					totalMemory = (int) (r.totalMemory() / 1048576),
					usedMemory = totalMemory - (int) (r.freeMemory() / 1048576),
					maxMemory = (int) (r.maxMemory() / 1048576);

			float usedMem = (float) usedMemory / maxMemory;
			float totalMem = (float) totalMemory / maxMemory;

			String barChar = ConfigValues.getMemoryBarChar();

			builder.append(
					usedMem < 0.8 ? ConfigValues.getMemoryBarUsedColor() : ConfigValues.getMemoryBarAllocationColor());

			for (int i = 0; i < (int) (barSize * usedMem); i++) {
				builder.append(barChar);
			}

			builder.append(ConfigValues.getMemoryBarFreeColor());

			for (int i = 0; i < (int) (barSize * (totalMem - usedMem)); i++) {
				builder.append(barChar);
			}

			builder.append(ConfigValues.getMemoryBarReleasedColor());

			for (int i = 0; i < (int) (barSize * (1 - totalMem)); i++) {
				builder.append(barChar);
			}

			str = StringUtils.replace(str, "%memory_bar%", builder.toString());
		}

		// TODO Remove or make more customisable variables
		for (java.util.Map.Entry<String, String> map : TabConfigValues.CUSTOM_VARIABLES.entrySet()) {
			if (str.indexOf(map.getKey()) >= 0) {
				str = StringUtils.replace(str, map.getKey(), map.getValue());
			}
		}

		int staffs = 0;
		if (str.indexOf("%staff-online%") >= 0) {
			for (TabListUser user : plugin.getUsers()) {
				Player player = user.getPlayer();

				if (!player.hasPermission("tablist.onlinestaff")
						|| (!ConfigValues.isCountVanishedStaff() && PluginUtils.isVanished(player))) {
					continue;
				}

				staffs++;
			}
		}

		String time = str.indexOf("%server-time%") >= 0 ? getTimeAsString(ConfigValues.getTimeFormat()) : "";
		String date = str.indexOf("%date%") >= 0 ? getTimeAsString(ConfigValues.getDateFormat()) : "";

		Long fram = r.freeMemory() / 1048576L,
				mram = r.maxMemory() / 1048576L,
				uram = (r.totalMemory() - r.freeMemory()) / 1048576L;

		if (pl != null) {
			str = setPlaceholders(pl, str);
		}

		str = Global.setSymbols(str);

		if (!time.isEmpty())
			str = StringUtils.replace(str, "%server-time%", time);

		if (!date.isEmpty())
			str = StringUtils.replace(str, "%date%", date);

		if (str.contains("%server-ram-free%"))
			str = StringUtils.replace(str, "%server-ram-free%", Long.toString(fram));

		if (str.contains("%server-ram-max%"))
			str = StringUtils.replace(str, "%server-ram-max%", Long.toString(mram));

		if (str.contains("%server-ram-used%"))
			str = StringUtils.replace(str, "%server-ram-used%", Long.toString(uram));

		if (str.contains("%online-players%"))
			str = StringUtils.replace(str, "%online-players%", Integer.toString(PluginUtils.countVanishedPlayers()));

		if (str.contains("%max-players%"))
			str = StringUtils.replace(str, "%max-players%", Integer.toString(Bukkit.getMaxPlayers()));

		if (str.contains("%servertype%"))
			str = StringUtils.replace(str, "%servertype%", Bukkit.getServer().getName());

		if (str.contains("%vanished-players%"))
			str = StringUtils.replace(str, "%vanished-players%", Integer.toString(PluginUtils.getVanishedPlayers()));

		if (str.contains("%staff-online%")) {
			str = StringUtils.replace(str, "%staff-online%", Integer.toString(staffs));
		}

		if (str.contains("%mc-version%"))
			str = StringUtils.replace(str, "%mc-version%", Bukkit.getBukkitVersion());

		if (str.contains("%motd%"))
			str = StringUtils.replace(str, "%motd%", plugin.getComplement().getMotd());

		if (str.contains("%tps%")) {
			double tps = TabListAPI.getTPS();

			if (!ConfigValues.isTpsCanBeHigher() && tps > 20D) {
				tps = 20D;
			}

			str = StringUtils.replace(str, "%tps%", tpsDot(tps));
		}

		if (str.contains("\n")) {
			str = StringUtils.replace(str, "\n", "\n");
		}

		// Don't use here colors because of some issues with hex
		return str;
	}

	@SuppressWarnings("deprecation")
	public String setPlaceholders(Player p, String s) {
		if (ConfigValues.isPlaceholderAPI() && plugin.isPluginEnabled("PlaceholderAPI")) {
			try {
				s = PlaceholderAPI.setPlaceholders(p, s);
			} catch (NullPointerException e) {
				// Some placeholders returns null, so we ignore that
			}
		}

		if (s.contains("%player%")) {
			s = StringUtils.replace(s, "%player%", p.getName());
		}

		if (s.contains("%player-displayname%")) {
			s = StringUtils.replace(s, "%player-displayname%", plugin.getComplement().getDisplayName(p));
		}

		if (s.contains("%player-uuid%")) {
			s = StringUtils.replace(s, "%player-uuid%", p.getUniqueId().toString());
		}

		if (s.contains("%world%")) {
			s = StringUtils.replace(s, "%world%", p.getWorld().getName());
		}

		if (s.contains("%player-gamemode%")) {
			s = StringUtils.replace(s, "%player-gamemode%", p.getGameMode().name());
		}

		if (s.contains("%player-health%")) {
			s = StringUtils.replace(s, "%player-health%", Double.toString(p.getHealth()));
		}

		if (s.contains("%player-max-health%")) {
			s = StringUtils.replace(s, "%player-max-health%",
					Double.toString(ServerVersion.isCurrentLower(ServerVersion.v1_9_R1) ? p.getMaxHealth()
							: p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue()));
		}

		if (s.contains("%ping%"))
			s = StringUtils.replace(s, "%ping%", formatPing(TabListAPI.getPing(p)));

		if (s.contains("%exp-to-level%"))
			s = StringUtils.replace(s, "%exp-to-level%", Integer.toString(p.getExpToLevel()));

		if (s.contains("%level%"))
			s = StringUtils.replace(s, "%level%", Integer.toString(p.getLevel()));

		if (s.contains("%xp%"))
			s = StringUtils.replace(s, "%xp%", Float.toString(p.getExp()));

		if (s.contains("%light-level%")) {
			s = StringUtils.replace(s, "%light-level%", Byte.toString(p.getLocation().getBlock().getLightLevel()));
		}

		if (s.indexOf("%ip-address%") >= 0) {
			java.net.InetSocketAddress address = p.getAddress();
			if (address != null && address.getAddress() != null) {
				s = StringUtils.replace(s, "%ip-address%", address.getAddress().toString().replace("/", ""));
			}
		}

		return s;
	}

	private String tpsDot(double d) {
		if (!ConfigValues.isTpsFormatEnabled() || ConfigValues.getTpsColorFormats().isEmpty()) {
			return "" + d;
		}

		String ds = parseExpression(d, NodeType.TPS);
		if (ds.contains(".")) {
			int tpsSize = ConfigValues.getTpsSize();
			int size = (tpsSize == 1 ? 3 : ds.indexOf('.')) + (tpsSize < 1 ? 2 : tpsSize);
			if (size > ds.length()) {
				size = ds.length();
			}

			ds = ds.substring(0, size);
		}

		return ds;
	}

	private String formatPing(int ping) {
		if (!ConfigValues.isPingFormatEnabled() || ConfigValues.getPingColorFormats().isEmpty()) {
			return "" + ping;
		}

		return parseExpression(ping, NodeType.PING);
	}

	private String parseExpression(double value, int type) {
		String color = "";

		for (ExpressionNode node : nodes) {
			if (node.getType() == type && node.parse(value)) {
				color = node.getCondition().getColor();
			}
		}

		color = color.trim();

		StringBuilder builder = new StringBuilder();
		if (!color.isEmpty()) {
			builder.append(color.replaceAll("%tps%|%ping%", "").replace('&', '\u00a7'));
		}

		return (type == NodeType.PING ? builder.append((int) value) : builder.append(value)).toString();
	}

	private String getTimeAsString(String pattern) {
		if (pattern.isEmpty()) {
			return pattern;
		}

		TimeZone zone = ConfigValues.isUseSystemZone() ? TimeZone.getTimeZone(java.time.ZoneId.systemDefault())
				: TimeZone.getTimeZone(ConfigValues.getTimeZone());
		LocalDateTime now = zone == null ? LocalDateTime.now() : LocalDateTime.now(zone.toZoneId());

		return now.format(DateTimeFormatter.ofPattern(pattern));
	}
}