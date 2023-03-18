package hu.montlikadani.tablist.tablist.groups.impl;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
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

	private void registerTeam() {
		if (packetTeam != null && !tl.getGroups().isToSort()) {
			return;
		}

		unregisterTeam();

		Player player = groupPlayer.getUser().getPlayer();

		if (player == null) {
			return;
		}

		String teamName = groupPlayer.getFullGroupTeamName();
		packetTeam = PacketNM.NMS_PACKET.createBoardTeam(ReflectionUtils.asComponent(teamName), teamName, player, ConfigValues.isFollowNameTagVisibility());

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

		Player pl = groupPlayer.getUser().getPlayer();
		Object removeTeamPacket = PacketNM.NMS_PACKET.unregisterBoardTeam(team);
		Object updateNamePacket = null;

		if (pl != null) {
			updateNamePacket = PacketNM.NMS_PACKET.updateDisplayNamePacket(PacketNM.NMS_PACKET.getPlayerHandle(pl), null, false);
		}

		packetTeam = null;

		for (Player player : tl.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(player, removeTeamPacket);

			if (updateNamePacket != null) {
				PacketNM.NMS_PACKET.sendPacket(player, updateNamePacket);
			}
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

		PacketNM.NMS_PACKET.setInfoData(updatePacket, groupPlayer.getUser().getUniqueId(), -2, groupPlayer.getTabNameWithPrefixSuffix().toComponent());

		for (Player pl : tl.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(pl, updatePacket);
		}
	}
}
