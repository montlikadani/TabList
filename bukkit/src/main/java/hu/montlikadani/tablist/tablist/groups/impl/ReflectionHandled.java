package hu.montlikadani.tablist.tablist.groups.impl;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.packets.PacketNM;
import hu.montlikadani.tablist.tablist.groups.GroupPlayer;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.reflection.ReflectionUtils;
import org.bukkit.entity.Player;

public class ReflectionHandled {

	private final TabList tl;
	private final GroupPlayer groupPlayer;

	public ReflectionHandled(TabList tl, GroupPlayer groupPlayer) {
		this.tl = tl;
		this.groupPlayer = groupPlayer;
	}

	public void createAndUpdateTeam() {
		Player player = groupPlayer.getUser().getPlayer();

		if (player == null) {
			return;
		}

		if (tl.getGroups().isToSort()) {
			unregisterTeam(false);

			String teamName = groupPlayer.getFullGroupTeamName();

			PacketNM.NMS_PACKET.createBoardTeam(ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1) ? null : ReflectionUtils.asComponent(teamName), teamName,
					player, ConfigValues.isFollowNameTagVisibility());
		}

		Object updatePacket = PacketNM.NMS_PACKET.updateDisplayNamePacket(PacketNM.NMS_PACKET.getPlayerHandle(player), null, false);

		PacketNM.NMS_PACKET.setInfoData(updatePacket, groupPlayer.getUser().getUniqueId(), -2, groupPlayer.getTabNameWithPrefixSuffix().toComponent());

		for (Player pl : tl.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(pl, updatePacket);
		}
	}

	public void unregisterTeam() {
		unregisterTeam(true);
	}

	private void unregisterTeam(boolean clearName) {
		Object team = PacketNM.NMS_PACKET.findBoardTeamByName(groupPlayer.getFullGroupTeamName());

		if (team == null) {
			return;
		}

		Object removeTeamPacket = PacketNM.NMS_PACKET.unregisterBoardTeam(team);
		Object updateNamePacket = null;

		if (clearName) {
			Player pl = groupPlayer.getUser().getPlayer();

			if (pl != null) {
				updateNamePacket = PacketNM.NMS_PACKET.updateDisplayNamePacket(PacketNM.NMS_PACKET.getPlayerHandle(pl), null, false);
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
