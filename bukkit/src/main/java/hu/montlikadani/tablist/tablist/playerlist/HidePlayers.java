package hu.montlikadani.tablist.tablist.playerlist;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.packets.PacketNM;

public final class HidePlayers {

	private final UUID to;

	public HidePlayers(UUID to) {
		this.to = to;
	}

	public void addPlayerToTab() {
		Player player = Bukkit.getServer().getPlayer(to);

		if (player == null) {
			return;
		}

		for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.addPlayersToTab(player, pl);
			PacketNM.NMS_PACKET.addPlayersToTab(pl, player);
		}
	}

	public void removePlayerFromTab() {
		Player player = Bukkit.getServer().getPlayer(to);

		if (player != null) {
			PacketNM.NMS_PACKET.removePlayersFromTab(player, Bukkit.getServer().getOnlinePlayers());
		}
	}
}
