package hu.montlikadani.tablist.bukkit.tablist.groups;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import hu.montlikadani.tablist.bukkit.TabListPlayer;
import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public class ReflectionHandled implements ITabScoreboard {

	private final TabScoreboardReflection scoreRef = new TabScoreboardReflection();

	private Object packet, playerConst, entityPlayerArray, packetPlayOutPlayerInfo;

	private TabListPlayer tabPlayer;

	public ReflectionHandled(TabListPlayer tabPlayer) {
		this.tabPlayer = tabPlayer;
	}

	@Override
	public TabListPlayer getTabPlayer() {
		return tabPlayer;
	}

	@Override
	public void registerTeam(String teamName) {
		if (packet != null) {
			return;
		}

		final Player player = tabPlayer.getPlayer();

		try {
			playerConst = ReflectionUtils.getHandle(player);

			scoreRef.init();

			packet = scoreRef.getScoreboardTeamConstructor().newInstance();

			scoreRef.getScoreboardTeamName().set(packet, teamName);
			scoreRef.getScoreboardTeamMode().set(packet, 0);
			scoreRef.getScoreboardTeamDisplayName().set(packet,
					Version.isCurrentEqualOrHigher(Version.v1_13_R1) ? ReflectionUtils.getAsIChatBaseComponent(teamName)
							: teamName);

			scoreRef.getScoreboardTeamNames().set(packet, Collections.singletonList(player.getName()));

			entityPlayerArray = Array.newInstance(playerConst.getClass(), 1);
			Array.set(entityPlayerArray, 0, playerConst);

			Class<?> packetPlayOutPlayerInfoClass = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo");
			Class<?> enumPlayerInfoAction = ReflectionUtils.Classes
					.getEnumPlayerInfoAction(packetPlayOutPlayerInfoClass);
			packetPlayOutPlayerInfo = packetPlayOutPlayerInfoClass
					.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass())
					.newInstance(ReflectionUtils.getFieldObject(enumPlayerInfoAction,
							enumPlayerInfoAction.getDeclaredField("UPDATE_DISPLAY_NAME")), entityPlayerArray);

			for (Player p : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(p, packet);
				ReflectionUtils.sendPacket(p, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setTeam(String teamName) {
		registerTeam(teamName);

		try {
			scoreRef.getScoreboardTeamName().set(packet, teamName);
			scoreRef.getScoreboardTeamMode().set(packet, 2);
			scoreRef.getScoreboardTeamDisplayName().set(packet,
					Version.isCurrentEqualOrHigher(Version.v1_13_R1) ? ReflectionUtils.getAsIChatBaseComponent(teamName)
							: teamName);

			updateName(tabPlayer.getPrefix() + tabPlayer.getPlayerName() + tabPlayer.getSuffix());

			for (Player p : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(p, packet);
				ReflectionUtils.sendPacket(p, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unregisterTeam(String teamName) {
		if (packet == null) {
			return;
		}

		try {
			scoreRef.getScoreboardTeamMode().set(packet, 1);

			updateName(tabPlayer.getPlayer().getName());

			for (Player p : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(p, packet);
				ReflectionUtils.sendPacket(p, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void updateName(String name) throws Throwable {
		if (packetPlayOutPlayerInfo == null) {
			return;
		}

		// Colorize '&' codes before hex
		if (!name.contains("#")) {
			name = hu.montlikadani.tablist.bukkit.utils.Util.colorMsg(name, true);
		}

		Object iChatBaseComponentName = ReflectionUtils.getAsIChatBaseComponent(name);
		//ReflectionUtils.setField(playerConst, "listName", iChatBaseComponentName);

		@SuppressWarnings("unchecked")
		List<Object> infoList = (List<Object>) ReflectionUtils.getField(packetPlayOutPlayerInfo, "b")
				.get(packetPlayOutPlayerInfo);
		for (Object infoData : infoList) {
			Object profile = ReflectionUtils.invokeMethod(infoData, "a");
			Object id = ReflectionUtils.invokeMethod(profile, "getId");
			if (id.equals(tabPlayer.getPlayer().getUniqueId())) {
				ReflectionUtils.modifyFinalField(ReflectionUtils.getField(infoData, "e"), infoData,
						iChatBaseComponentName);
				break;
			}
		}
	}

	@Override
	public Scoreboard getScoreboard() {
		return null;
	}

	@Override
	public void setScoreboard(Scoreboard board) {
	}
}
