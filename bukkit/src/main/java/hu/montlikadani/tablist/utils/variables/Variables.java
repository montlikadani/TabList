package hu.montlikadani.tablist.utils.variables;

import java.time.LocalDateTime;

import io.papermc.paper.threadedregions.TickRegions;
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

	private final boolean entityAttributeSupported;

	public Variables(TabList plugin) {
		this.plugin = plugin;

		boolean att;
		try {
			org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH.name();
			att = true;
		} catch (Error err) {
			att = false;
		}
		entityAttributeSupported = att;
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
			variables.add(new Variable("date", 3, (v, str) -> str = str.replace(v.fullName,
					v.remainingValue(getTimeAsString(ConfigValues.getDateFormat())))));
		}

		variables.add(new Variable("online-players", 2, (v, str) -> {
			int players = PluginUtils.countVanishedPlayers();

			if (ConfigValues.isCountFakePlayersToOnlinePlayers()) {
				players += plugin.getFakePlayerHandler().fakePlayers.size();
			}

			str = str.replace(v.fullName, v.remainingValue(Integer.toString(players)));
		}));

		variables.add(new Variable("max-players", 20, (v, str) -> str = str.replace(v.fullName,
				v.remainingValue(Integer.toString(plugin.getServer().getMaxPlayers())))));

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

				if (player == null || !PluginUtils.hasPermission(player, "tablist.onlinestaff") || (!ConfigValues.isCountVanishedStaff()
						&& PluginUtils.isVanished(player))) {
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
			text.updateText(replaceVariables(pl, text.getPlainText()));
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

		str = Global.replace(str, "%tps-overflow%", () -> roundTpsDigits(TabListAPI.getTPS()[0]));
		str = Global.replace(str, "%tps%", () -> tpsDigits(TabListAPI.getTPS()[0]));

		for (final TicksPerSecond one : TicksPerSecond.VALUES) {
			str = Global.replace(str, "%tps-overflow-" + one.dur + "%", () -> {
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

			str = Global.replace(str, "%tps-" + one.dur + "%", () -> {
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
				str = Global.replace(str, "%folia-current-region-average-tps-" + one.dur + "%", () -> {
					io.papermc.paper.threadedregions.ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData,
							TickRegions.TickRegionSectionData> currentRegion = io.papermc.paper.threadedregions.TickRegionScheduler.getCurrentRegion();

					if (currentRegion == null) {
						return "no current region";
					}

					if (currentRegion.getData() == null) {
						return "no region data available";
					}

					io.papermc.paper.threadedregions.TickRegionScheduler.RegionScheduleHandle scheduleHandle = currentRegion.getData().getRegionSchedulingHandle();
					io.papermc.paper.threadedregions.TickData.TickReportData tickReportData;

					switch (one) {
						case SECONDS_5:
							tickReportData = scheduleHandle.getTickReport5s(System.nanoTime());
							break;
						case SECONDS_15:
							tickReportData = scheduleHandle.getTickReport15s(System.nanoTime());
							break;
						case MINUTES_1:
							tickReportData = scheduleHandle.getTickReport1m(System.nanoTime());
							break;
						case MINUTES_5:
							tickReportData = scheduleHandle.getTickReport5m(System.nanoTime());
							break;
						case MINUTES_15:
							tickReportData = scheduleHandle.getTickReport15m(System.nanoTime());
							break;
						default:
							return "-1";
					}

					if (tickReportData == null) {
						return "no tick report generated";
					}

					return tpsDigits(tickReportData.tpsData().segmentAll().average());
				});
			}
		}

		return str;
	}

	private String tpsDigits(double tps) {
		return tps >= 21.0 ? (fixedTpsString == null ? fixedTpsString = roundTpsDigits(20.0) : fixedTpsString) : roundTpsDigits(tps);
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
			if (entityAttributeSupported) {
				org.bukkit.attribute.AttributeInstance attr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);

				if (attr != null) {
					text = text.replace("%player-max-health%", Double.toString(attr.getDefaultValue()));
				}
			} else {
				text = text.replace("%player-max-health%", Double.toString(player.getMaxHealth()));
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

	private enum TicksPerSecond {

		SECONDS_5("5sec"), SECONDS_15("15sec"), MINUTES_1("1min"), MINUTES_5("5min"), MINUTES_15("15min");

		public static final TicksPerSecond[] VALUES = values();

		final String dur;

		TicksPerSecond(String dur) {
			this.dur = dur;
		}
	}
}
