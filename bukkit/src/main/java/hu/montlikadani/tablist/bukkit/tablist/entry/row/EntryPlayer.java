package hu.montlikadani.tablist.bukkit.tablist.entry.row;

import java.lang.reflect.Array;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;

public final class EntryPlayer {

	public static void removePlayer(Player player) {
		if (player == null) {
			return;
		}

		Bukkit.getScheduler().runTaskLater(TabListAPI.getPlugin(), () -> {
			try {
				Object entityPlayer = ReflectionUtils.getHandle(player);

				Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
				Array.set(entityPlayerArray, 0, entityPlayer);

				Class<?> packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo");
				Class<?> enumPlayerInfoAction = ReflectionUtils.Classes
						.getEnumPlayerInfoAction(packetPlayOutPlayerInfo);

				Object packet = packetPlayOutPlayerInfo
						.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass())
						.newInstance(enumPlayerInfoAction.getDeclaredField("REMOVE_PLAYER").get(enumPlayerInfoAction),
								entityPlayerArray);

				for (Player pl : Bukkit.getOnlinePlayers()) {
					ReflectionUtils.sendPacket(pl, packet);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 20L); // 1 second to remove? lmao
	}
}
