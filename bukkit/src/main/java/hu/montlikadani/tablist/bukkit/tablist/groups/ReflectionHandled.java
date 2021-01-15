package hu.montlikadani.tablist.bukkit.tablist.groups;

import java.lang.reflect.Array;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.TabListPlayer;
import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;

public class ReflectionHandled implements ITabScoreboard {

	//private final TabScoreboardReflection scoreRef = new TabScoreboardReflection();

	private Object /*packet, */playerConst, entityPlayerArray, packetPlayOutPlayerInfo;

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
		if (packetPlayOutPlayerInfo != null) {
			return;
		}

		try {
			playerConst = ReflectionUtils.getHandle(tabPlayer.getPlayer());

			// TODO Fix client error when using teams
			/*scoreRef.init();

			packet = scoreRef.getScoreboardTeamConstructor().newInstance();

			scoreRef.getScoreboardTeamName().set(packet, teamName);
			scoreRef.getScoreboardTeamMode().set(packet, 0);
			scoreRef.getScoreboardTeamDisplayName().set(packet,
					Version.isCurrentEqualOrHigher(Version.v1_13_R1) ? ReflectionUtils.getAsIChatBaseComponent(teamName)
							: teamName);

			scoreRef.getScoreboardTeamNames().set(packet, Collections.singletonList(tabPlayer.getPlayer().getName()));*/

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
				//ReflectionUtils.sendPacket(p, packet);
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
			/*scoreRef.getScoreboardTeamMode().set(packet, 2);
			scoreRef.getScoreboardTeamDisplayName().set(packet,
					Version.isCurrentEqualOrHigher(Version.v1_13_R1) ? ReflectionUtils.getAsIChatBaseComponent(teamName)
							: teamName);*/

			updateName(tabPlayer.getCustomTabName());

			for (Player p : Bukkit.getOnlinePlayers()) {
				//ReflectionUtils.sendPacket(p, packet);
				ReflectionUtils.sendPacket(p, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unregisterTeam(String teamName) {
		if (packetPlayOutPlayerInfo == null) {
			return;
		}

		try {
			//scoreRef.getScoreboardTeamMode().set(packet, 1);

			updateName(tabPlayer.getPlayer().getName());

			for (Player p : Bukkit.getOnlinePlayers()) {
				//ReflectionUtils.sendPacket(p, packet);
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

		@SuppressWarnings("unchecked")
		List<Object> infoList = (List<Object>) ReflectionUtils.getField(packetPlayOutPlayerInfo, "b")
				.get(packetPlayOutPlayerInfo);
		for (Object infoData : infoList) {
			Object profile = ReflectionUtils.invokeMethod(infoData, "a");
			Object id = ReflectionUtils.invokeMethod(profile, "getId");
			if (id.equals(tabPlayer.getPlayer().getUniqueId())) {
				ReflectionUtils.modifyFinalField(ReflectionUtils.getField(infoData, "e"), infoData,
						ReflectionUtils.getAsIChatBaseComponent(name));
				break;
			}
		}
	}
}
