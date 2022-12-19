package hu.montlikadani.tablist.tablist.playerlist;

import java.util.UUID;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.packets.PacketNM;

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
			PacketNM.NMS_PACKET.addPlayerToTab(player, pl);
			PacketNM.NMS_PACKET.addPlayerToTab(pl, player);
		}
	}

	public void removePlayerFromTab() {
		Player player = plugin.getServer().getPlayer(to);

		if (player != null) {
			PacketNM.NMS_PACKET.removePlayersFromTab(player, plugin.getServer().getOnlinePlayers());
		}
	}
}
