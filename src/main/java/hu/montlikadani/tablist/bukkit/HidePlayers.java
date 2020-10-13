package hu.montlikadani.tablist.bukkit;

import java.lang.reflect.Array;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;

public final class HidePlayers {

	private Class<?> enumPlayerInfoAction;
	private Object entityPlayerArray;

	private final Player player;

	public HidePlayers(Player player) {
		this.player = player;
	}

	public Player getPlayer() {
		return player;
	}

	public void addPlayerToTab() {
		Bukkit.getOnlinePlayers().forEach(this::addPlayerToTab);
	}

	public void removePlayerFromTab() {
		for (Player pl : Bukkit.getOnlinePlayers()) {
			removePlayerFromTab(player, pl);
			removePlayerFromTab(pl, player);
		}
	}

	public void removePlayerFromTab(Player p, Player to) {
		Bukkit.getScheduler().runTaskLater(TabList.getInstance(), () -> r(p, to), 4L);
	}

	public void addPlayerToTab(Player to) {
		if (enumPlayerInfoAction == null || entityPlayerArray == null) {
			return;
		}

		try {
			Object packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo")
					.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass()).newInstance(ReflectionUtils
							.getFieldObject(enumPlayerInfoAction, enumPlayerInfoAction.getDeclaredField("ADD_PLAYER")),
							entityPlayerArray);

			ReflectionUtils.sendPacket(to, packetPlayOutPlayerInfo);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void r(Player p, Player to) {
		try {
			Object playerConst = ReflectionUtils.getHandle(p);
			Class<?> packetPlayOutPlayerInfoClass = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo");
			enumPlayerInfoAction = ReflectionUtils.Classes.getEnumPlayerInfoAction(packetPlayOutPlayerInfoClass);

			ReflectionUtils.setField(playerConst, "listName", ReflectionUtils.getAsIChatBaseComponent(p.getName()));

			entityPlayerArray = Array.newInstance(playerConst.getClass(), 1);
			Array.set(entityPlayerArray, 0, playerConst);

			Object packetPlayOutPlayerInfo = packetPlayOutPlayerInfoClass
					.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass())
					.newInstance(ReflectionUtils.getFieldObject(enumPlayerInfoAction,
							enumPlayerInfoAction.getDeclaredField("REMOVE_PLAYER")), entityPlayerArray);

			ReflectionUtils.sendPacket(to, packetPlayOutPlayerInfo);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
