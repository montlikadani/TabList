package hu.montlikadani.tablist.tablist.groups.impl;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.packets.PacketNM;
import hu.montlikadani.tablist.tablist.groups.GroupPlayer;
import hu.montlikadani.tablist.utils.reflection.ReflectionUtils;
import org.bukkit.entity.Player;

public class ReflectionHandled {

	private final TabList tl;
	private final GroupPlayer groupPlayer;

	private Object packetTeam;

	public ReflectionHandled(TabList tl, GroupPlayer groupPlayer) {
		this.tl = tl;
		this.groupPlayer = groupPlayer;
	}

	public void registerTeam() {
		if (packetTeam != null && !tl.getGroups().isToSort()) {
			return;
		}

		unregisterTeam();

		Player player = groupPlayer.getUser().getPlayer();

		if (player == null) {
			return;
		}

		packetTeam = PacketNM.NMS_PACKET.createBoardTeam(groupPlayer.getFullGroupTeamName(), player);

		for (Player pl : tl.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(pl, packetTeam);
		}
	}

	public void setTeam() {
		registerTeam();
		updateName();
	}

	public void unregisterTeam() {
		Object team = PacketNM.NMS_PACKET.findBoardTeamByName(groupPlayer.getFullGroupTeamName());

		if (team == null) {
			return;
		}

		Object removeTeamPacket = PacketNM.NMS_PACKET.unregisterBoardTeam(team);

		if (removeTeamPacket == null) {
			return;
		}

		packetTeam = null;

		for (Player player : tl.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(player, removeTeamPacket);
		}
	}

	private void updateName() {
		if (PacketNM.NMS_PACKET.findBoardTeamByName(groupPlayer.getFullGroupTeamName()) == null) {
			return;
		}

		Player player = groupPlayer.getUser().getPlayer();

		if (player == null) {
			return;
		}

		Object updatePacket = PacketNM.NMS_PACKET.updateDisplayNamePacket(PacketNM.NMS_PACKET.getPlayerHandle(player), null, false);

		try {
			PacketNM.NMS_PACKET.setInfoData(updatePacket, groupPlayer.getUser().getUniqueId(), -2, ReflectionUtils.asComponent(groupPlayer.getTabNameWithPrefixSuffix()));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		for (Player pl : tl.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(pl, updatePacket);
		}
	}
}
