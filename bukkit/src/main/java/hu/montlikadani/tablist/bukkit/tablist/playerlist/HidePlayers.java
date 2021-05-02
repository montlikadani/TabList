package hu.montlikadani.tablist.bukkit.tablist.playerlist;

import java.lang.reflect.Array;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.api.TabListAPI;
import hu.montlikadani.tablist.bukkit.utils.reflection.NMSContainer;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

public final class HidePlayers {

	private Player to;

	public HidePlayers(Player to) {
		this.to = to;
	}

	public void addPlayerToTab() {
		for (Player pl : Bukkit.getOnlinePlayers()) {
			addPlayerToTab(to, pl);
			addPlayerToTab(pl, to);
		}
	}

	public void removePlayerFromTab() {
		for (Player pl : Bukkit.getOnlinePlayers()) {
			removePlayerFromTab(to, pl);
			removePlayerFromTab(pl, to);
		}
	}

	public void addPlayerToTab(Player p, Player to) {
		try {
			Object playerConst = ReflectionUtils.getHandle(p);

			Object entityPlayerArray = Array.newInstance(playerConst.getClass(), 1);
			Array.set(entityPlayerArray, 0, playerConst);

			Object packetPlayOutPlayerInfo = NMSContainer.getPacketPlayOutPlayerInfo()
					.getConstructor(NMSContainer.getEnumPlayerInfoAction(), entityPlayerArray.getClass())
					.newInstance(NMSContainer.getAddPlayer(), entityPlayerArray);

			ReflectionUtils.sendPacket(to, packetPlayOutPlayerInfo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void removePlayerFromTab(Player p, Player to) {
		try {
			Object playerConst = ReflectionUtils.getHandle(p);

			Object entityPlayerArray = Array.newInstance(playerConst.getClass(), 1);
			Array.set(entityPlayerArray, 0, playerConst);

			Object packetPlayOutPlayerInfo = NMSContainer.getPacketPlayOutPlayerInfo()
					.getConstructor(NMSContainer.getEnumPlayerInfoAction(), entityPlayerArray.getClass())
					.newInstance(NMSContainer.getRemovePlayer(), entityPlayerArray);

			Bukkit.getScheduler().runTaskLater(TabListAPI.getPlugin(),
					() -> ReflectionUtils.sendPacket(to, packetPlayOutPlayerInfo), 6L);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
