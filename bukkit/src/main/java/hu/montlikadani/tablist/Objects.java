package hu.montlikadani.tablist;

import hu.montlikadani.tablist.utils.scheduler.TLScheduler;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

import hu.montlikadani.tablist.api.TabListAPI;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.packets.PacketNM;
import hu.montlikadani.tablist.tablist.fakeplayers.IFakePlayer;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.StrUtil;

import java.util.Locale;

public final class Objects {

	private transient final TabList plugin;

	private TLScheduler scheduler;

	Objects(TabList plugin) {
		this.plugin = plugin;
	}

	@SuppressWarnings("deprecation")
	void registerHealthTab(Player pl) {
		if (ConfigValues.getObjectsDisabledWorlds().contains(pl.getWorld().getName()) || ConfigValues.getHealthObjectRestricted().contains(pl.getName())) {
			unregisterHealthObjective(pl);
			return;
		}

		final Scoreboard board = pl.getScoreboard();
		final String objectName = ConfigValues.getObjectType().objectName;

		if (board.getObjective(objectName) != null) {
			return;
		}

		TLScheduler tlsched = plugin.newTLScheduler();

		tlsched.submitSync(() -> {
			Objective objective;

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
				objective = plugin.getComplement().registerNewObjective(board, objectName, "health", objectName, RenderType.HEARTS);
			} else {
				objective = board.registerNewObjective(objectName, "health");
				plugin.getComplement().displayName(objective, org.bukkit.ChatColor.RED + "\u2665");
			}

			objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);

			adjustMaxHealth(pl, 1);
			pl.setHealth(pl.getHealth() + 1);

			tlsched.runDelayed(() -> {
				adjustMaxHealth(pl, -1);
				pl.setHealth(pl.getHealth() - 1);
			}, pl.getLocation(), 2);

			return 1;
		});
	}

	@SuppressWarnings("deprecation")
	private void adjustMaxHealth(Player player, double value) {
		try {
			org.bukkit.attribute.AttributeInstance maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);

			if (maxHealth != null) {
				maxHealth.setBaseValue(maxHealth.getBaseValue() + value);
			}
		} catch (Error err) {
			player.setMaxHealth(player.getMaxHealth() + value);
		}
	}

	private PluginPlaceholders customPlaceholder;

	void startTask() {

		// Load object setting in every case (even if the task is running)
		if (ConfigValues.getObjectType() == ObjectTypes.CUSTOM) {
			for (PluginPlaceholders placeholder : PluginPlaceholders.values()) {
				if (ConfigValues.getCustomObjectSetting().indexOf(placeholder.name) != -1) {
					customPlaceholder = placeholder;
					break;
				}
			}
		}

		if (!isCancelled()) {
			return;
		}

		if (ConfigValues.getObjectRefreshInterval() == 0) {
			update();
			return;
		}

		scheduler = plugin.newTLScheduler().submitAsync(() -> {
			if (plugin.performanceIsUnderValue() || plugin.getUsers().isEmpty()) {
				cancelTask();
			} else {
				update();
			}
		}, 0, ConfigValues.getObjectRefreshInterval());
	}

	private void update() {
		for (TabListUser user : plugin.getUsers()) {
			if (user.getPlayerScore().getScoreName().isEmpty()) {
				continue;
			}

			Player player = user.getPlayer();

			if (player == null || ConfigValues.getObjectsDisabledWorlds().contains(player.getWorld().getName())) {
				continue;
			}

			ObjectTypes type = ConfigValues.getObjectType();

			if (!user.getPlayerScore().isObjectiveCreated()) {
				Object objectiveInstance = PacketNM.NMS_PACKET.createObjectivePacket(type.objectName, type.chatBaseComponent);

				// Create objective, 0 - create
				PacketNM.NMS_PACKET.sendPacket(player, PacketNM.NMS_PACKET.scoreboardObjectivePacket(objectiveInstance, 0));

				// Where to display, 0 - PlayerList
				PacketNM.NMS_PACKET.sendPacket(player, PacketNM.NMS_PACKET.scoreboardDisplayObjectivePacket(objectiveInstance, 0));

				// Update ping score for fake players if set
				if (type == ObjectTypes.PING) {
					for (IFakePlayer fakePlayer : plugin.getFakePlayerHandler().fakePlayers) {
						if (fakePlayer.getPingLatency() > 0) {
							PacketNM.NMS_PACKET.sendPacket(player,
									PacketNM.NMS_PACKET.changeScoreboardScorePacket(type.objectName, fakePlayer.getName(), fakePlayer.getPingLatency()));
						}
					}
				}

				user.getPlayerScore().setObjectiveCreated();
			}

			int lastScore = 0;

			if (type == ObjectTypes.PING) {
				lastScore = TabListAPI.getPing(player);
			} else if (type == ObjectTypes.CUSTOM) {
				lastScore = getValue(player, ConfigValues.getCustomObjectSetting());
			}

			// Update objective value

			if (lastScore != user.getPlayerScore().getLastScore()) {
				user.getPlayerScore().setLastScore(lastScore);

				for (Player pl : plugin.getServer().getOnlinePlayers()) {
					PacketNM.NMS_PACKET.sendPacket(pl, PacketNM.NMS_PACKET.changeScoreboardScorePacket(type.objectName, user.getPlayerScore().getScoreName(), lastScore));
				}
			}
		}
	}

	private int getValue(Player player, final String text) {
		if (customPlaceholder == null) {
			return parsePapi(player, text);
		}

		switch (customPlaceholder) {
			case EXP_TO_LEVEL:
				return player.getExpToLevel();
			case LEVEL:
				return player.getLevel();
			case PING:
				return TabListAPI.getPing(player);
			case LIGHT_LEVEL:
				return player.getLocation().getBlock().getLightLevel();
			default:
				return parsePapi(player, text);
		}
	}

	private int parsePapi(Player player, String from) {
		if (plugin.hasPapi()) {
			try {
				return Integer.parseInt(StrUtil.getNumberEscapeSequence().reset(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, from)).replaceAll(""));
			} catch (NumberFormatException e) {
				hu.montlikadani.tablist.utils.Util.logConsole("Invalid custom objective with " + ConfigValues.getCustomObjectSetting() + " value.");
			}
		}

		return 0;
	}

	public void cancelTask() {
		if (!isCancelled()) {
			scheduler.cancelTask();
			scheduler = null;
		}
	}

	public boolean isCancelled() {
		return scheduler == null;
	}

	public void unregisterHealthObjective(Player player) {
		Objective obj = player.getScoreboard().getObjective(ObjectTypes.HEALTH.objectName);

		if (obj != null) {
			obj.unregister();
		}
	}

	public void unregisterObjective(ObjectTypes type, TabListUser source) {
		if (type != ObjectTypes.HEALTH && !source.getPlayerScore().isObjectiveCreated()) {
			return;
		}

		Player player = source.getPlayer();

		if (player == null) {
			return;
		}

		if (type == ObjectTypes.HEALTH) {
			unregisterHealthObjective(player);
			return;
		}

		source.getPlayerScore().setObjectiveCreated();

		// Send remove action
		PacketNM.NMS_PACKET.sendPacket(player, PacketNM.NMS_PACKET.removeScoreboardScorePacket(type.objectName, source.getPlayerScore().getScoreName(), 0));

		// Unregister objective
		PacketNM.NMS_PACKET.sendPacket(player,
				PacketNM.NMS_PACKET.scoreboardObjectivePacket(PacketNM.NMS_PACKET.createScoreboardHealthObjectivePacket(type.objectName, type.chatBaseComponent), 1));
	}

	public enum ObjectTypes {
		HEALTH("showhealth", false), PING("PingTab", true), CUSTOM("customObj", true), NONE("", false);

		public final String loweredName;

		private final String objectName;
		private Object chatBaseComponent;

		ObjectTypes(String objectName, boolean needChatBaseComponent) {
			if (!objectName.isEmpty()) {
				loweredName = name().toLowerCase(java.util.Locale.ENGLISH);
			} else {
				loweredName = "";
			}

			if (needChatBaseComponent) {
				chatBaseComponent = hu.montlikadani.tablist.utils.reflection.ReflectionUtils.asComponent(objectName);
			}

			this.objectName = objectName;
		}

		public String getObjectName() {
			return objectName;
		}
	}

	private enum PluginPlaceholders {

		// Player placeholders
		PING, EXP_TO_LEVEL, LEVEL, LIGHT_LEVEL;

		public final String name;

		PluginPlaceholders() {
			name = '%' + name().replace('_', '-').toLowerCase(Locale.ENGLISH) + '%';
		}
	}
}
