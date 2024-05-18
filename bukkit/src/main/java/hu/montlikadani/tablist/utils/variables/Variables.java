package hu.montlikadani.tablist.utils.variables;

import hu.montlikadani.api.Pair;
import hu.montlikadani.api.TicksPerSecondType;
import hu.montlikadani.tablist.FoliaPack;
import java.time.LocalDateTime;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.logicalOperators.LogicalNode;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.api.TabListAPI;
import hu.montlikadani.tablist.utils.PluginUtils;
import hu.montlikadani.tablist.utils.Util;
import hu.montlikadani.tablist.utils.operators.OverriddenOperatorNodes;

public final class Variables {

	private final TabList plugin;

	private final java.util.List<LogicalNode> nodes = new java.util.ArrayList<>();
	private final java.util.Set<TimedVariable> timedVariables = new java.util.HashSet<>(8);

	private final boolean entityAttributeSupported;

	public Variables(TabList plugin) {
		this.plugin = plugin;

		boolean att;
		try {
			Attribute.GENERIC_MAX_HEALTH.name();
			att = true;
		} catch (Error err) {
			att = false;
		}
		entityAttributeSupported = att;
	}

	public void load() {
		nodes.clear();
		timedVariables.clear();

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
			timedVariables.add(new TimedVariable("date", 3,
					variable -> getTimeAsString(ConfigValues.getDateFormat())));
		}

		timedVariables.add(new TimedVariable("online-players", 2, variable -> {
			int players = PluginUtils.countVanishedPlayers();

			if (ConfigValues.isCountFakePlayersToOnlinePlayers()) {
				players += plugin.getFakePlayerHandler().fakePlayers.size();
			}

			return Integer.toString(players);
		}));

		timedVariables.add(new TimedVariable("max-players", 20,
				variable -> Integer.toString(plugin.getServer().getMaxPlayers())));

		timedVariables.add(new TimedVariable("vanished-players", 2, variable -> {
			int vanishedPlayers = PluginUtils.getVanishedPlayers();

			return vanishedPlayers == 0 ? "0" : Integer.toString(vanishedPlayers);
		}));

		timedVariables.add(new TimedVariable("motd", 10, variable -> plugin.getComplement().motd()));

		timedVariables.add(new TimedVariable("fake-players", 3, variable -> {
			int size = plugin.getFakePlayerHandler().fakePlayers.size();

			return size == 0 ? "0" : Integer.toString(size);
		}));

		timedVariables.add(new TimedVariable("staff-online", 3, variable -> {
			int staffs = 0;

			for (TabListUser user : plugin.getUsers()) {
				Player player = user.getPlayer();

				if (player == null || !PluginUtils.hasPermission(player, "tablist.onlinestaff")
						|| (!ConfigValues.isCountVanishedStaff() && PluginUtils.isVanished(player))) {
					continue;
				}

				staffs++;
			}

			return staffs == 0 ? "0" : Integer.toString(staffs);
		}));

		timedVariables.add(new TimedVariable("tps-overflow", 3,
				variable -> roundTpsDigits(TabListAPI.getTPS()[0])));
		timedVariables.add(new TimedVariable("tps", 3,
				variable -> tpsDigits(TabListAPI.getTPS()[0])));

		// A temporal solution for non async-safe PAPI placeholders
		if (plugin.hasPapi()) {
			timedVariables.add(new TimedVariable("server_total_chunks", 30,
					variable -> Integer.toString(getWorldChunks())));

			timedVariables.add(new TimedVariable("server_total_living_entities", 10,
					variable -> Integer.toString(getLivingEntities())));

			timedVariables.add(new TimedVariable("server_total_entities", 10,
					variable -> Integer.toString(getTotalEntities())));
		}
	}

	// These are the variables that will be replaced once
	public String replaceMiscVariables(String str) {
		str = str.replace("%servertype%", plugin.getServer().getName());
		str = str.replace("%mc-version%", plugin.getServer().getBukkitVersion());
		str = Global.replaceToUnicodeSymbol(str);

		return Util.applyTextFormat(str);
	}

	public String replaceVariables(Player player, String str) {
		if (str.isEmpty()) {
			return str;
		}

		for (TimedVariable timedVariable : timedVariables) {
			if (timedVariable.canReplace(str)) {
				str = str.replace(timedVariable.fullName, timedVariable.keptValue(timedVariable.function.apply(timedVariable)));
			} else if (timedVariable.getKeptValue() != null) {
				str = str.replace(timedVariable.fullName, timedVariable.getKeptValue());
			}
		}

		if (player != null) {
			str = setPlayerPlaceholders(player, str);
		}

		if (ConfigValues.getTimeFormat() != null) {
			str = Global.replace(str, "%server-time%", () -> getTimeAsString(ConfigValues.getTimeFormat()));
		}

		final long megaBytes = 1048576L;
		final Runtime runtime = Runtime.getRuntime();

		str = Global.replace(str, "%server-ram-free%", () -> Long.toString(runtime.freeMemory() / megaBytes));
		str = Global.replace(str, "%server-ram-max%", () -> Long.toString(runtime.maxMemory() / megaBytes));
		str = Global.replace(str, "%server-ram-used%", () ->
				Long.toString((runtime.totalMemory() - runtime.freeMemory()) / megaBytes));

		for (final TicksPerSecondType one : TicksPerSecondType.VALUES) {
			str = Global.replace(str, "%tps-overflow-" + one.type + "%", () -> {
				switch (one) {
					case MINUTES_1:
						return roundTpsDigits(TabListAPI.getTPS()[0]);
					case MINUTES_5:
						return roundTpsDigits(TabListAPI.getTPS()[1]);
					case MINUTES_15:
						return roundTpsDigits(TabListAPI.getTPS()[2]);
					default:
						return "no value by this type";
				}
			});

			str = Global.replace(str, "%tps-" + one.type + "%", () -> {
				switch (one) {
					case MINUTES_1:
						return tpsDigits(TabListAPI.getTPS()[0]);
					case MINUTES_5:
						return tpsDigits(TabListAPI.getTPS()[1]);
					case MINUTES_15:
						return tpsDigits(TabListAPI.getTPS()[2]);
					default:
						return "no value by this type";
				}
			});

			if (plugin.isFoliaServer()) {
				str = Global.replace(str, "%folia-current-region-average-tps-" + one.type + "%", () -> {
					Pair<Double, String> tickReportData = FoliaPack.tickReportDataByType(one);

					return tickReportData.key == -1.0 ? tickReportData.value : tpsDigits(tickReportData.key);
				});
			}
		}

		return str;
	}

	private String fixedTpsString;

	private String tpsDigits(double tps) {
		return tps >= 21.0 ? (fixedTpsString == null ? fixedTpsString = roundTpsDigits(20.0) : fixedTpsString) : roundTpsDigits(tps);
	}

	String setPlayerPlaceholders(Player player, String text) {
		if (plugin.hasPapi()) {
			text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
		}

		text = Global.replace(text, "%player%", player::getName);
		text = Global.replace(text, "%world%", () -> player.getWorld().getName());
		text = Global.replace(text, "%player-gamemode%", () -> player.getGameMode().name());
		text = Global.replace(text, "%player-displayname%", () -> plugin.getComplement().displayName(player));
		text = Global.replace(text, "%player-health%", () -> Double.toString(player.getHealth()));
		text = Global.replace(text, "%ping%", () -> formatPing(TabListAPI.getPing(player)));
		text = Global.replace(text, "%exp-to-level%", () -> Integer.toString(player.getExpToLevel()));
		text = Global.replace(text, "%level%", () -> Integer.toString(player.getLevel()));
		text = Global.replace(text, "%xp%", () -> Float.toString(player.getExp()));
		text = Global.replace(text, "%light-level%", () -> Integer.toString(player.getLocation().getBlock().getLightLevel()));

		text = Global.replace(text, "%player-max-health%", () -> {
			if (entityAttributeSupported) {
				org.bukkit.attribute.AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
				return attribute == null ? "" : Double.toString(attribute.getDefaultValue());
			}

			return Double.toString(player.getMaxHealth());
		});

		text = Global.replace(text, "%ip-address%", () -> {
			java.net.InetSocketAddress address = player.getAddress();

			if (address != null) {
				java.net.InetAddress inetAddress = address.getAddress();

				if (inetAddress != null) {
					String hostAddress = inetAddress.getHostAddress();

					if (hostAddress != null) {
						return hostAddress;
					}
				}
			}

			return "";
		});

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
		return (ConfigValues.getTimeZone() == null ? LocalDateTime.now() : LocalDateTime.now(ConfigValues.getTimeZone()
				.toZoneId())).format(formatterPattern);
	}

	private int getWorldChunks() {
		return callSyncMethod(() -> {
			int loadedChunks = 0;

			for (World world : plugin.getServer().getWorlds()) {
				loadedChunks += world.getLoadedChunks().length;
			}

			return loadedChunks;
		});
	}

	private int getLivingEntities() {
		return callSyncMethod(() -> {
			int livingEntities = 0;

			for (World world : plugin.getServer().getWorlds()) {
				livingEntities += world.getLivingEntities().size();
			}

			return livingEntities;
		});
	}

	private int getTotalEntities() {
		return callSyncMethod(() -> {
			int allEntities = 0;

			for (World world : plugin.getServer().getWorlds()) {
				allEntities += world.getEntities().size();
			}

			return allEntities;
		});
	}

	private <V> V callSyncMethod(java.util.function.Supplier<V> supplier) {
		try {
			return plugin.getServer().getScheduler().callSyncMethod(plugin, supplier::get).get();
		} catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
			ex.printStackTrace();
		}

		return supplier.get();
	}
}
