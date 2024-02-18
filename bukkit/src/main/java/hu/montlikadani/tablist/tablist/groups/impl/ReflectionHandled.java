package hu.montlikadani.tablist.tablist.groups.impl;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.packets.PacketNM;
import hu.montlikadani.tablist.tablist.groups.GroupPlayer;
import org.bukkit.entity.Player;

public class ReflectionHandled {

	private final TabList tl;
	private final GroupPlayer groupPlayer;

	public ReflectionHandled(TabList tl, GroupPlayer groupPlayer) {
		this.tl = tl;
		this.groupPlayer = groupPlayer;
	}

	public void createAndUpdateTeam() {
		Player player = groupPlayer.tabListUser.getPlayer();

		if (player == null) {
			return;
		}

		if (tl.getGroups().isToSort()) {
			unregisterTeam(false);

			PacketNM.NMS_PACKET.createBoardTeam(groupPlayer.getFullGroupTeamName(), player,
					ConfigValues.isFollowNameTagVisibility());
		}

		Object updatePacket = PacketNM.NMS_PACKET.updateDisplayNamePacket(PacketNM.NMS_PACKET.getPlayerHandle(player),
				null, false);

		PacketNM.NMS_PACKET.setInfoData(updatePacket, groupPlayer.tabListUser.getUniqueId(), -2,
				groupPlayer.getTabNameWithPrefixSuffix().toComponent());

		for (Player one : tl.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(one, updatePacket);
		}
	}

	public void unregisterTeam() {
		unregisterTeam(true);
	}

	private void unregisterTeam(boolean clearName) {
		Object removeTeamPacket = PacketNM.NMS_PACKET.unregisterBoardTeamPacket(groupPlayer.getFullGroupTeamName());

		if (removeTeamPacket == null) {
			return;
		}

		Object updateNamePacket = null;

		if (clearName) {
			Player player = groupPlayer.tabListUser.getPlayer();

			if (player != null) {
				updateNamePacket = PacketNM.NMS_PACKET.updateDisplayNamePacket(PacketNM.NMS_PACKET.getPlayerHandle(player),
						null, false);
			}
		}

		for (Player player : tl.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(player, removeTeamPacket);

			if (updateNamePacket != null) {
				PacketNM.NMS_PACKET.sendPacket(player, updateNamePacket);
			}
		}
	}
}
