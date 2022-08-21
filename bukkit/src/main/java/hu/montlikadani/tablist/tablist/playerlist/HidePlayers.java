package hu.montlikadani.tablist.tablist.playerlist;

import java.lang.reflect.Array;
import java.util.UUID;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.utils.reflection.ClazzContainer;
import hu.montlikadani.tablist.utils.reflection.ReflectionUtils;

public final class HidePlayers {

	private transient final TabList plugin;
	private final UUID to;

	public HidePlayers(TabList plugin, UUID to) {
		this.plugin = plugin;
		this.to = to;
	}

	public void addPlayerToTab() {
		Player player = plugin.getServer().getPlayer(to);

		if (player == null) {
			return;
		}

		for (Player pl : plugin.getServer().getOnlinePlayers()) {
			addPlayerToTab(player, pl);
			addPlayerToTab(pl, player);
		}
	}

	public void removePlayerFromTab() {
		Player player = plugin.getServer().getPlayer(to);

		if (player == null) {
			return;
		}

		for (Player pl : plugin.getServer().getOnlinePlayers()) {
			removePlayerFromTab(player, pl);
			removePlayerFromTab(pl, player);
		}
	}

	public void addPlayerToTab(Player p, Player to) {
		try {
			Object entityPlayer = ReflectionUtils.getPlayerHandle(p);

			Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
			Array.set(entityPlayerArray, 0, entityPlayer);

			Object packetPlayOutPlayerInfo = ClazzContainer.getPlayOutPlayerInfoConstructor()
					.newInstance(ClazzContainer.getAddPlayer(), entityPlayerArray);

			ReflectionUtils.sendPacket(to, packetPlayOutPlayerInfo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void removePlayerFromTab(Player p, Player to) {
		try {
			Object entityPlayer = ReflectionUtils.getPlayerHandle(p);

			Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
			Array.set(entityPlayerArray, 0, entityPlayer);

			Object packetPlayOutPlayerInfo = ClazzContainer.getPlayOutPlayerInfoConstructor()
					.newInstance(ClazzContainer.getRemovePlayer(), entityPlayerArray);

			plugin.getServer().getScheduler().runTaskLater(plugin,
					() -> ReflectionUtils.sendPacket(to, packetPlayOutPlayerInfo), 6L);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
