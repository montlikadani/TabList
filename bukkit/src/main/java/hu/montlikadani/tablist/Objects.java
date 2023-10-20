package hu.montlikadani.tablist;

import hu.montlikadani.tablist.user.PlayerScore;
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

import java.util.Locale;

public final class Objects {

	private transient final TabList plugin;

	private TLScheduler scheduler;
	private PluginPlaceholders customPlaceholder;
	private java.util.regex.Matcher numberEscapeSequence;

	private transient final boolean renderTypeSupported;

	Objects(TabList plugin) {
		this.plugin = plugin;

		boolean rtSupported;
		try {
			Class.forName("org.bukkit.scoreboard.RenderType");
			rtSupported = true;
		} catch (ClassNotFoundException ex) {
			rtSupported = false;
		}

		renderTypeSupported = rtSupported;
	}

	void load() {
		ObjectTypes type = ConfigValues.getObjectType();

		customPlaceholder = null;

		if (type == Objects.ObjectTypes.NONE || type == Objects.ObjectTypes.HEALTH) {
			cancelTask();
			return;
		}

		if (type == ObjectTypes.CUSTOM) {
			for (PluginPlaceholders placeholder : PluginPlaceholders.values()) {
				if (ConfigValues.getCustomObjectSetting().indexOf(placeholder.name) != -1) {
					customPlaceholder = placeholder;
					break;
				}
			}
		}
	}

	void load(Player player) {
		ObjectTypes type = ConfigValues.getObjectType();

		if (type == Objects.ObjectTypes.HEALTH) {
			cancelTask();
			registerHealthTab(player);
		} else {
			unregisterHealthObjective(player);

			if (type != ObjectTypes.NONE) {
				startTask();
			} else {
				cancelTask();
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void registerHealthTab(Player pl) {
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

			if (renderTypeSupported) {
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

	private void startTask() {
		if (ConfigValues.getObjectRefreshInterval() == 0) {
			update();
			return;
		}

		if (!isCancelled()) {
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
			PlayerScore playerScore = user.getPlayerScore(true);

			if (playerScore == null || playerScore.getScoreName().isEmpty()) {
				continue;
			}

			Player player = user.getPlayer();

			if (player == null || ConfigValues.getObjectsDisabledWorlds().contains(player.getWorld().getName())) {
				continue;
			}

			ObjectTypes type = ConfigValues.getObjectType();

			if (!playerScore.isObjectiveCreated()) {
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

				playerScore.setObjectiveCreated();
			}

			int lastScore = 0;

			if (type == ObjectTypes.PING) {
				lastScore = TabListAPI.getPing(player);
			} else if (type == ObjectTypes.CUSTOM) {
				lastScore = getValue(player);
			}

			// Update objective value

			if (lastScore != playerScore.getLastScore()) {
				playerScore.setLastScore(lastScore);

				for (Player pl : plugin.getServer().getOnlinePlayers()) {
					PacketNM.NMS_PACKET.sendPacket(pl, PacketNM.NMS_PACKET.changeScoreboardScorePacket(type.objectName, playerScore.getScoreName(), lastScore));
				}
			}
		}
	}

	private int getValue(Player player) {
		if (customPlaceholder == null) {
			return parsePapi(player);
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
				return parsePapi(player);
		}
	}

	private int parsePapi(Player player) {
		if (plugin.hasPapi()) {
			String value = ConfigValues.getCustomObjectSetting();

			if (numberEscapeSequence == null) {
				numberEscapeSequence = java.util.regex.Pattern.compile("[^\\d]").matcher("");
			}

			try {
				return Integer.parseInt(numberEscapeSequence.reset(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, value))
						.replaceAll(""));
			} catch (NumberFormatException e) {
				hu.montlikadani.tablist.utils.Util.logConsole("Invalid custom objective with " + value + " value.");
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
		PlayerScore playerScore = source.getPlayerScore();

		if (playerScore == null) {
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

		if (playerScore.isObjectiveCreated()) {
			playerScore.setObjectiveCreated();
		}

		// Send remove action
		PacketNM.NMS_PACKET.sendPacket(player, PacketNM.NMS_PACKET.removeScoreboardScorePacket(type.objectName, playerScore.getScoreName(), 0));

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
