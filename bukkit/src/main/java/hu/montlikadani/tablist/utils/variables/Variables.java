package hu.montlikadani.tablist.utils.variables;

import java.time.LocalDateTime;

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

public final class Variables {

	private final TabList plugin;

	private final java.util.List<LogicalNode> nodes = new java.util.ArrayList<>();
	private final java.util.Set<Variable> variables = new java.util.HashSet<>(6);

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
		str = hu.montlikadani.tablist.Global.replaceToUnicodeSymbol(str);

		return Util.applyTextFormat(str);
	}

	public TabText replaceVariables(Player pl, TabText text) {
		if (!text.getPlainText().isEmpty()) {
			String str = replaceVariables(pl, text.getPlainText());

			text.updateText(ServerVersion.isCurrentEqualOrLower(ServerVersion.v1_15_R2) ? Util.applyTextFormat(str) : str);
		}

		return text;
	}

	private final long MB = 1024 * 1024;
	private String fixedTpsString;

	public String replaceVariables(Player pl, String str) {
		if (str.isEmpty()) {
			return str;
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
			return tps >= 21.0 ? (fixedTpsString == null ? fixedTpsString = roundTpsDigits(20.0) : fixedTpsString) : roundTpsDigits(tps);
		});

		return str;
	}

	@SuppressWarnings("deprecation")
	String setPlayerPlaceholders(Player player, String text) {
		if (plugin.hasPapi()) {

			// A temporal solution for PAPI placeholders
			int stc = text.indexOf("%server_total_chunks%");
			int ste = text.indexOf("%server_total_entities%");
			int stl = text.indexOf("%server_total_living_entities%");

			if (stc != -1 || ste != -1 || stl != -1) {
				final String str = text;

				try {
					text = plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
						String st = str;

						if (stc != -1) {
							st = st.replace("%server_total_chunks%", Integer.toString(getChunks()));
						}

						if (stl != -1) {
							st = st.replace("%server_total_living_entities%", Integer.toString(getLivingEntities()));
						}

						if (ste != -1) {
							st = st.replace("%server_total_entities%", Integer.toString(getTotalEntities()));
						}

						return st;
					}).get();
				} catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
					ex.printStackTrace();
				}
			}

			text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
		}

		text = Global.replace(text, "%player%", player::getName);
		text = Global.replace(text, "%world%", () -> player.getWorld().getName());
		text = Global.replace(text, "%player-gamemode%", () -> player.getGameMode().name());
		text = Global.replace(text, "%player-displayname%", () -> plugin.getComplement().displayName(player));
		text = Global.replace(text, "%player-health%", () -> Double.toString(player.getHealth()));

		if (text.indexOf("%player-max-health%") != -1) {
			if (ServerVersion.isCurrentLower(ServerVersion.v1_9_R1)) {
				text = text.replace("%player-max-health%", Double.toString(player.getMaxHealth()));
			} else {
				org.bukkit.attribute.AttributeInstance attr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);

				if (attr != null) {
					text = text.replace("%player-max-health%", Double.toString(attr.getDefaultValue()));
				}
			}
		}

		text = Global.replace(text, "%ping%", () -> formatPing(TabListAPI.getPing(player)));
		text = Global.replace(text, "%exp-to-level%", () -> Integer.toString(player.getExpToLevel()));
		text = Global.replace(text, "%level%", () -> Integer.toString(player.getLevel()));
		text = Global.replace(text, "%xp%", () -> Float.toString(player.getExp()));
		text = Global.replace(text, "%light-level%", () -> Integer.toString(player.getLocation().getBlock().getLightLevel()));

		if (text.indexOf("%ip-address%") != -1) {
			java.net.InetSocketAddress address = player.getAddress();

			if (address != null) {
				java.net.InetAddress inetAddress = address.getAddress();

				if (inetAddress != null) {
					String hostAddress = inetAddress.getHostAddress();

					if (hostAddress != null) {
						text = text.replace("%ip-address%", hostAddress);
					}
				}
			}
		}

		return text;
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

	private String getTimeAsString(java.time.format.DateTimeFormatter formatterPattern) {
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
