package hu.montlikadani.tablist.utils.variables;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.logicalOperators.LogicalNode;
import hu.montlikadani.tablist.tablist.TabText;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.api.TabListAPI;
import hu.montlikadani.tablist.utils.PluginUtils;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.Util;
import hu.montlikadani.tablist.utils.operators.OverriddenOperatorNodes;
import me.clip.placeholderapi.PlaceholderAPI;

public final class Variables {

	private final TabList plugin;

	private final List<LogicalNode> nodes = new ArrayList<>();
	private final java.util.Set<Variable> variables = new java.util.HashSet<>(6);

	private String roundedMaxTps;

	public Variables(TabList plugin) {
		this.plugin = plugin;
	}

	public void load() {
		nodes.clear();
		variables.clear();

		if (ConfigValues.isPingFormatEnabled()) {
			for (String f : ConfigValues.getPingColorFormats()) {
				if (f.isEmpty()) {
					continue;
				}

				LogicalNode node = LogicalNode.newNode(LogicalNode.NodeType.PING).parseInput(f);

				if (node.getCondition() != null) {
					nodes.add(node);
				}
			}
		}

		if (ConfigValues.isTpsFormatEnabled()) {
			for (String f : ConfigValues.getTpsColorFormats()) {
				if (f.isEmpty()) {
					continue;
				}

				LogicalNode node = new OverriddenOperatorNodes(LogicalNode.NodeType.TPS).parseInput(f);

				if (node.getCondition() != null) {
					nodes.add(node);
				}
			}
		}

		LogicalNode.reverseOrderOfArray(nodes);
		roundedMaxTps = roundTpsDigits(20.0);

		if (ConfigValues.getDateFormat() != null) {
			variables.add(new Variable("date", 3, (v, str) -> str = str.replace(v.fullName, v.remainingValue(getTimeAsString(ConfigValues.getDateFormat())))));
		}

		variables.add(new Variable("online-players", 2, (v, str) -> {
			int players = PluginUtils.countVanishedPlayers();

			if (ConfigValues.isCountFakePlayersToOnlinePlayers()) {
				players += plugin.getFakePlayerHandler().fakePlayers.size();
			}

			str = str.replace(v.fullName, v.remainingValue(Integer.toString(players)));
		}));

		variables.add(new Variable("max-players", 20, (v, str) -> str = str.replace(v.fullName, v.remainingValue(Integer.toString(plugin.getServer().getMaxPlayers())))));

		variables.add(new Variable("vanished-players", 2, (v, str) -> {
			int vanishedPlayers = PluginUtils.getVanishedPlayers();

			str = str.replace(v.fullName, v.remainingValue(vanishedPlayers == 0 ? "0" : Integer.toString(vanishedPlayers)));
		}));

		variables.add(new Variable("motd", 10, (v, str) -> str = str.replace(v.fullName, v.remainingValue(plugin.getComplement().motd()))));

		variables.add(new Variable("fake-players", 3, (v, str) -> {
			int pls = plugin.getFakePlayerHandler().fakePlayers.size();

			str = str.replace(v.fullName, v.remainingValue(pls == 0 ? "0" : Integer.toString(pls)));
		}));

		variables.add(new Variable("staff-online", 3, (v, str) -> {
			int staffs = 0;

			for (TabListUser user : plugin.getUsers()) {
				Player player = user.getPlayer();

				if (player == null || !PluginUtils.hasPermission(player, "tablist.onlinestaff") || (!ConfigValues.isCountVanishedStaff() && PluginUtils.isVanished(player))) {
					continue;
				}

				staffs++;
			}

			str = str.replace(v.fullName, v.remainingValue(staffs == 0 ? "0" : Integer.toString(staffs)));
		}));
	}

	// These are the variables that will be replaced once
	public String replaceMiscVariables(String str) {
		str = str.replace("%servertype%", plugin.getServer().getName());
		str = str.replace("%mc-version%", plugin.getServer().getBukkitVersion());
		str = hu.montlikadani.tablist.Global.setSymbols(str);

		return str;
	}

	public TabText replaceVariables(Player pl, TabText text) {
		if (text != null) {
			String t = replaceVariables(pl, text.getPlainText());

			if (ServerVersion.isCurrentEqualOrLower(ServerVersion.v1_15_R2)) {
				t = Util.colorText(t);
			}

			text.updateText(t);
		}

		return text;
	}

	private final long MB = 1024 * 1024;

	public String replaceVariables(Player pl, String str) {
		if (str.isEmpty()) {
			return str;
		}

		// TODO marked for removal, no one uses this
		if (!ConfigValues.getMemoryBarChar().isEmpty()) {
			str = Global.replace(str, "%memory_bar%", () -> {
				Runtime runtime = Runtime.getRuntime();

				int barSize = ConfigValues.getMemoryBarSize(), totalMemory = (int) (runtime.totalMemory() / MB), usedMemory = totalMemory - (int) (runtime.freeMemory() / MB),
						maxMemory = (int) (runtime.maxMemory() / MB);

				float usedMem = (float) usedMemory / maxMemory;
				float totalMem = (float) totalMemory / maxMemory;

				String barChar = ConfigValues.getMemoryBarChar();
				StringBuilder builder = new StringBuilder(usedMem < 0.8 ? ConfigValues.getMemoryBarUsedColor() : ConfigValues.getMemoryBarAllocationColor());

				int i = 0;
				int totalBarSize = (int) (barSize * usedMem);
				for (; i < totalBarSize; i++) {
					builder.append(barChar);
				}

				builder.append(ConfigValues.getMemoryBarFreeColor());

				totalBarSize = (int) (barSize * (totalMem - usedMem));
				for (i = 0; i < totalBarSize; i++) {
					builder.append(barChar);
				}

				builder.append(ConfigValues.getMemoryBarReleasedColor());

				totalBarSize = (int) (barSize * (1 - totalMem));
				for (i = 0; i < totalBarSize; i++) {
					builder.append(barChar);
				}

				return builder.toString();
			});
		}

		if (pl != null) {
			str = setPlayerPlaceholders(pl, str);
		}

		for (Variable variable : variables) {
			if (variable.canReplace(str)) {
				variable.consumer.accept(variable, str);
			}

			if (variable.getRemainingValue() != null) {
				str = str.replace(variable.fullName, variable.getRemainingValue());
			}
		}

		if (ConfigValues.getTimeFormat() != null) {
			str = Global.replace(str, "%server-time%", () -> getTimeAsString(ConfigValues.getTimeFormat()));
		}

		str = Global.replace(str, "%server-ram-free%", () -> Long.toString(Runtime.getRuntime().freeMemory() / MB));
		str = Global.replace(str, "%server-ram-max%", () -> Long.toString(Runtime.getRuntime().maxMemory() / MB));

		str = Global.replace(str, "%server-ram-used%", () -> {
			Runtime runtime = Runtime.getRuntime();
			return Long.toString((runtime.totalMemory() - runtime.freeMemory()) / MB);
		});

		str = Global.replace(str, "%tps-overflow%", () -> roundTpsDigits(TabListAPI.getTPS()));

		str = Global.replace(str, "%tps%", () -> {
			double tps = TabListAPI.getTPS();
			return tps > 20.0 ? roundedMaxTps : roundTpsDigits(tps);
		});

		return str;
	}

	@SuppressWarnings("deprecation")
	String setPlayerPlaceholders(Player player, String s) {
		if (plugin.hasPapi()) {

			// Some PAPI placeholders which does not supports async threads
			int stc = s.indexOf("%server_total_chunks%");
			int ste = s.indexOf("%server_total_entities%");
			int stl = s.indexOf("%server_total_living_entities%");

			if (stc != -1 || ste != -1 || stl != -1) {
				final String st = s;

				s = hu.montlikadani.tablist.utils.task.Tasks.submitSync(() -> {
					String str = st;

					if (stc != -1) {
						str = str.replace("%server_total_chunks%", Integer.toString(getChunks()));
					}

					if (stl != -1) {
						str = str.replace("%server_total_living_entities%", Integer.toString(getLivingEntities()));
					}

					if (ste != -1) {
						str = str.replace("%server_total_entities%", Integer.toString(getTotalEntities()));
					}

					return str;
				});
			}

			s = PlaceholderAPI.setPlaceholders(player, s);
		}

		s = Global.replace(s, "%player%", () -> player.getName());
		s = Global.replace(s, "%world%", () -> player.getWorld().getName());
		s = Global.replace(s, "%player-gamemode%", () -> player.getGameMode().name());
		s = Global.replace(s, "%player-displayname%", () -> plugin.getComplement().displayName(player));
		s = Global.replace(s, "%player-health%", () -> Double.toString(player.getHealth()));

		if (s.indexOf("%player-max-health%") != -1) {
			if (ServerVersion.isCurrentLower(ServerVersion.v1_9_R1)) {
				s = s.replace("%player-max-health%", Double.toString(player.getMaxHealth()));
			} else {
				org.bukkit.attribute.AttributeInstance attr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);

				if (attr != null) {
					s = s.replace("%player-max-health%", Double.toString(attr.getDefaultValue()));
				}
			}
		}

		s = Global.replace(s, "%ping%", () -> formatPing(TabListAPI.getPing(player)));
		s = Global.replace(s, "%exp-to-level%", () -> Integer.toString(player.getExpToLevel()));
		s = Global.replace(s, "%level%", () -> Integer.toString(player.getLevel()));
		s = Global.replace(s, "%xp%", () -> Float.toString(player.getExp()));
		s = Global.replace(s, "%light-level%", () -> Integer.toString(player.getLocation().getBlock().getLightLevel()));

		if (s.indexOf("%ip-address%") != -1) {
			java.net.InetSocketAddress address = player.getAddress();

			if (address != null) {
				java.net.InetAddress inetAddress = address.getAddress();

				if (inetAddress != null) {
					String hostAddress = inetAddress.getHostAddress();

					if (hostAddress != null) {
						s = s.replace("%ip-address%", hostAddress);
					}
				}
			}
		}

		return s;
	}

	private String roundTpsDigits(double value) {
		if (!ConfigValues.isTpsFormatEnabled() || nodes.isEmpty()) {
			return Double.toString(value);
		}

		int digits = ConfigValues.getTpsDigits();

		// Making the value to be equally to secondCondition by rounding
		value = (double) Math.round(value * digits) / digits;

		return LogicalNode.parseCondition(value, LogicalNode.NodeType.TPS, nodes).toString();
	}

	private String formatPing(int ping) {
		if (!ConfigValues.isPingFormatEnabled() || nodes.isEmpty()) {
			return Integer.toString(ping);
		}

		return LogicalNode.parseCondition(ping, LogicalNode.NodeType.PING, nodes).toString();
	}

	private String getTimeAsString(DateTimeFormatter formatterPattern) {
		return (ConfigValues.getTimeZone() == null ? LocalDateTime.now() : LocalDateTime.now(ConfigValues.getTimeZone().toZoneId())).format(formatterPattern);
	}

	private int getChunks() {
		int loadedChunks = 0;

		for (World world : plugin.getServer().getWorlds()) {
			loadedChunks += world.getLoadedChunks().length;
		}

		return loadedChunks;
	}

	private int getLivingEntities() {
		int livingEntities = 0;

		for (World world : plugin.getServer().getWorlds()) {
			livingEntities += world.getLivingEntities().size();
		}

		return livingEntities;
	}

	private int getTotalEntities() {
		int allEntities = 0;

		for (World world : plugin.getServer().getWorlds()) {
			allEntities += world.getEntities().size();
		}

		return allEntities;
	}
}
