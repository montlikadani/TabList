package hu.montlikadani.tablist.bukkit.tablist.groups.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

public final class TabScoreboardReflection {

	private Constructor<?> scoreboardTeamConstructor;

	private Field scoreboardTeamName, scoreboardTeamDisplayName, scoreboardTeamPrefix, scoreboardTeamSuffix,
			/*scoreboardTeamColor, */scoreboardTeamNames, scoreboardTeamMode, scoreboardPlayers, nameTagVisibility;

	//private Object teamColor;

	public void init() throws Exception {
		Class<?> packetPlayOutScoreboardTeam = ReflectionUtils.getNMSClass("net.minecraft.network.protocol.game",
				"PacketPlayOutScoreboardTeam");

		scoreboardTeamConstructor = packetPlayOutScoreboardTeam.getConstructor();
		scoreboardTeamName = ReflectionUtils.getField(packetPlayOutScoreboardTeam, "a");
		scoreboardTeamDisplayName = ReflectionUtils.getField(packetPlayOutScoreboardTeam, "b");
		scoreboardTeamPrefix = ReflectionUtils.getField(packetPlayOutScoreboardTeam, "c");
		scoreboardTeamSuffix = ReflectionUtils.getField(packetPlayOutScoreboardTeam, "d");
		nameTagVisibility = ReflectionUtils.getField(packetPlayOutScoreboardTeam, "e");

		/*if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R1)) {
			scoreboardTeamColor = ReflectionUtils.getField(packetPlayOutScoreboardTeam, "g");
			teamColor = Enum.valueOf(ReflectionUtils.getNMSClass("EnumChatFormat").asSubclass(Enum.class), "WHITE");
		}*/

		scoreboardTeamNames = ReflectionUtils.getField(packetPlayOutScoreboardTeam,
				ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_9_R1) ? "h" : "g");
		scoreboardTeamMode = ReflectionUtils.getField(packetPlayOutScoreboardTeam,
				ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_9_R1) ? "i" : "h");
		scoreboardPlayers = ReflectionUtils.getField(packetPlayOutScoreboardTeam,
				ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_9_R1) ? "h" : "e");
	}

	public Constructor<?> getScoreboardTeamConstructor() {
		return scoreboardTeamConstructor;
	}

	public Field getScoreboardTeamName() {
		return scoreboardTeamName;
	}

	public Field getScoreboardTeamDisplayName() {
		return scoreboardTeamDisplayName;
	}

	public Field getScoreboardTeamPrefix() {
		return scoreboardTeamPrefix;
	}

	public Field getScoreboardTeamSuffix() {
		return scoreboardTeamSuffix;
	}

	/*public Field getScoreboardTeamColor() {
		return scoreboardTeamColor;
	}*/

	public Field getScoreboardTeamNames() {
		return scoreboardTeamNames;
	}

	public Field getScoreboardTeamMode() {
		return scoreboardTeamMode;
	}

	public Field getScoreboardPlayers() {
		return scoreboardPlayers;
	}

	public Field getNameTagVisibility() {
		return nameTagVisibility;
	}

	/*public Object getTeamColor() {
		return teamColor;
	}*/
}
