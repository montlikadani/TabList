package hu.montlikadani.tablist;

import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

import hu.montlikadani.tablist.api.TabListAPI;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.StrUtil;
import hu.montlikadani.tablist.utils.reflection.ClazzContainer;
import hu.montlikadani.tablist.utils.reflection.ReflectionUtils;
import hu.montlikadani.tablist.utils.task.Tasks;
import hu.montlikadani.tablist.utils.variables.simplePlaceholder.SimplePluginPlaceholder;

@SuppressWarnings("deprecation")
public final class Objects {

	private final TabList plugin;
	private final AtomicInteger objectScore = new AtomicInteger();

	private BukkitTask task;

	Objects(TabList plugin) {
		this.plugin = plugin;
	}

	void registerHealthTab(Player pl) {
		if (ConfigValues.getObjectsDisabledWorlds().contains(pl.getWorld().getName())
				|| ConfigValues.getHealthObjectRestricted().contains(pl.getName())) {
			unregisterHealthObjective(pl);
			return;
		}

		// TODO Fix not show correctly the health after reload

		final Scoreboard board = pl.getScoreboard();
		final String objectName = ConfigValues.getObjectType().objectName;

		if (board.getObjective(objectName) != null) {
			return;
		}

		Tasks.submitSync(() -> {
			Objective objective;

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
				objective = plugin.getComplement().registerNewObjective(board, objectName, "health", objectName,
						RenderType.HEARTS);
			} else {
				objective = board.registerNewObjective(objectName, "health");
				plugin.getComplement().setDisplayName(objective, org.bukkit.ChatColor.RED + "\u2665");
			}

			objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
			return 1;
		});
	}

	private SimplePluginPlaceholder customPlaceholder;

	void startTask() {
		cancelTask();

		if (ConfigValues.getObjectType() == ObjectTypes.CUSTOM) {
			customPlaceholder = SimplePluginPlaceholder.findOne(ConfigValues.getCustomObjectSetting());
		}

		task = Tasks.submitAsync(() -> {
			if (plugin.getUsers().isEmpty()) {
				cancelTask();
				return;
			}

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
					try {
						if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
							Object objectiveInstance = ClazzContainer.getFirstScoreboardObjectiveConstructor().newInstance(null,
									type.objectName, ClazzContainer.getiScoreboardCriteriaDummy(),
									ReflectionUtils.getAsIChatBaseComponent(type.objectName),
									ClazzContainer.getEnumScoreboardHealthDisplayInteger());

							// Create objective
							ReflectionUtils.sendPacket(player, ClazzContainer.getPacketPlayOutScoreboardObjectiveConstructor()
									.newInstance(objectiveInstance, 0));

							// Where to display, 0 - PlayerList
							ReflectionUtils.sendPacket(player, ClazzContainer
									.getPacketPlayOutScoreboardDisplayObjectiveConstructor().newInstance(0, objectiveInstance));
						} else {
							Scoreboard board = player.getScoreboard();
							Objective object = board.getObjective(type.objectName);

							if (object == null) {
								object = board.registerNewObjective(type.objectName, "dummy");
							}

							object.setDisplaySlot(DisplaySlot.PLAYER_LIST);

							if (type == ObjectTypes.PING) {
								object.setDisplayName("ms");
							}

							/*Object packet = ClazzContainer.getPacketPlayOutScoreboardObjectiveConstructor()
									.newInstance();

							ClazzContainer.getPacketPlayOutScoreboardObjectiveNameField().set(packet, type.objectName);
							ClazzContainer.getPacketPlayOutScoreboardObjectiveDisplayNameField().set(packet,
									type.objectName);
							ClazzContainer.getPacketPlayOutScoreboardObjectiveRenderType().set(packet,
									ClazzContainer.getEnumScoreboardHealthDisplayInteger());
							ClazzContainer.getScoreboardObjectiveMethod().set(packet, 1);

							ReflectionUtils.sendPacket(player, packet);

							ReflectionUtils.sendPacket(player,
									ClazzContainer.getPacketPlayOutScoreboardDisplayObjectiveConstructor().newInstance(
											0, ClazzContainer.getFirstScoreboardObjectiveConstructor().newInstance(null,
													type.objectName, ClazzContainer.getiScoreboardCriteriaDummy())));*/
						}
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}

					user.getPlayerScore().setObjectiveCreated();
				}

				if (type == ObjectTypes.PING) {
					objectScore.set(TabListAPI.getPing(player));
				} else if (type == ObjectTypes.CUSTOM) {
					objectScore.set(getValue(player, ConfigValues.getCustomObjectSetting()));
				}

				// Update objective value

				int lastScore = objectScore.get();

				if (lastScore != user.getPlayerScore().getLastScore()) {
					user.getPlayerScore().setLastScore(lastScore);

					for (TabListUser us : plugin.getUsers()) {
						Player pl = us.getPlayer();

						if (pl == null) {
							continue;
						}

						try {
							if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
								ReflectionUtils.sendPacket(pl,
										ClazzContainer.getPacketPlayOutScoreboardScoreConstructor().newInstance(
												ClazzContainer.getEnumScoreboardActionChange(), type.objectName,
												user.getPlayerScore().getScoreName(), lastScore));
							} else {
								// Packets does not really want to work for old versions so we uses that the API
								// provided

								Objective objective = pl.getScoreboard().getObjective(type.objectName);

								if (objective != null) {
									objective.getScore(user.getPlayerScore().getScoreName()).setScore(lastScore);
								}

								/*Object scoreObject = ClazzContainer.getScoreboardScoreConstructor().newInstance(
										ClazzContainer.getScoreboardConstructor().newInstance(),
										ClazzContainer.getFirstScoreboardObjectiveConstructor().newInstance(null,
												type.objectName, ClazzContainer.getiScoreboardCriteriaDummy()),
										user.getPlayerScore().getScoreName());

								ClazzContainer.getSetScoreboardScoreMethod().invoke(scoreObject, lastScore);

								ReflectionUtils.sendPacket(pl, ClazzContainer
										.getPacketPlayOutScoreboardScoreSbScoreConstructor().newInstance(scoreObject));*/
							}
						} catch (Exception e) {
							e.printStackTrace();
							return;
						}
					}
				}
			}
		}, ConfigValues.getObjectRefreshInterval(), ConfigValues.getObjectRefreshInterval());
	}

	private int getValue(Player player, final String text) {
		if (customPlaceholder == null) {
			return parsePapi(player, text);
		}

		switch (customPlaceholder.placeholder) {
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
			String result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, from);
			result = StrUtil.getNumberEscapeSequence().matcher(result).replaceAll("");

			try {
				return Integer.parseInt(result);
			} catch (NumberFormatException e) {
				hu.montlikadani.tablist.utils.Util
						.logConsole("Invalid custom objective with " + ConfigValues.getCustomObjectSetting() + " value.");
			}
		}

		return 0;
	}

	public void cancelTask() {
		if (!isCancelled()) {
			task.cancel();
			task = null;
		}
	}

	public boolean isCancelled() {
		// Do NOT use #isCancelled method, version-dependent
		return task == null;
	}

	public void unregisterHealthObjective(Player player) {
		Objective obj = player.getScoreboard().getObjective(ObjectTypes.HEALTH.objectName);

		if (obj != null) {
			obj.unregister();
		}
	}

	public void unregisterObjective(ObjectTypes type, TabListUser source) {
		Player player = source.getPlayer();

		if (player == null) {
			return;
		}

		if (type == ObjectTypes.HEALTH) {
			unregisterHealthObjective(player);
			return;
		}

		if (!source.getPlayerScore().isObjectiveCreated()) {
			return;
		}

		source.getPlayerScore().setObjectiveCreated();

		try {
			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {

				// Send remove action
				ReflectionUtils.sendPacket(player,
						ClazzContainer.getPacketPlayOutScoreboardScoreConstructor().newInstance(
								ClazzContainer.getEnumScoreboardActionRemove(), type.objectName,
								source.getPlayerScore().getScoreName(), 0));

				// Unregister objective
				ReflectionUtils.sendPacket(player,
						ClazzContainer.getPacketPlayOutScoreboardObjectiveConstructor()
								.newInstance(ClazzContainer.getFirstScoreboardObjectiveConstructor().newInstance(null,
										type.objectName, ClazzContainer.getiScoreboardCriteriaDummy(),
										ReflectionUtils.getAsIChatBaseComponent(type.objectName),
										ClazzContainer.getEnumScoreboardHealthDisplayInteger()), 1));
			} else {
				/*ReflectionUtils.sendPacket(player, ClazzContainer.getPacketPlayOutScoreboardScoreConstructor()
						.newInstance(source.getPlayerScore().getScoreName()));*/

				Objective object = player.getScoreboard().getObjective(type.objectName);

				if (object != null) {
					object.unregister();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public enum ObjectTypes {
		HEALTH("showhealth"), PING("PingTab"), CUSTOM("customObj"), NONE("");

		public final String loweredName;

		private final String objectName;

		ObjectTypes(String objectName) {
			if (!objectName.isEmpty()) {
				loweredName = name().toLowerCase(java.util.Locale.ENGLISH);
			} else {
				loweredName = "";
			}

			this.objectName = objectName;
		}

		public String getObjectName() {
			return objectName;
		}
	}
}