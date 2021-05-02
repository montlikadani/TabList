package hu.montlikadani.tablist.bukkit;

import java.util.List;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.EnumWrappers;

import hu.montlikadani.tablist.bukkit.api.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.tablist.playerlist.HidePlayers;
import hu.montlikadani.tablist.bukkit.user.TabListPlayer;
import hu.montlikadani.tablist.bukkit.user.TabListUser;

// TODO Get rid from ProtocolLib entirely
public class ProtocolPackets extends PacketAdapter {

	private static final TabList PLUGIN = TabListAPI.getPlugin();

	static void onSpectatorChange() {
		ProtocolLibrary.getProtocolManager().removePacketListeners(PLUGIN);

		ProtocolPackets s = new ProtocolPackets();

		/*if (ConfigValues.isRemoveGrayColorFromTabInSpec()) {
			ProtocolLibrary.getProtocolManager().addPacketListener(s);
		}*/

		if (ConfigValues.isHidePlayersFromTab()) {
			ProtocolLibrary.getProtocolManager().addPacketListener(s.new EntitySpawn());
		}
	}

	private ProtocolPackets() {
		super(PLUGIN, PacketType.Play.Server.PLAYER_INFO);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		/*if (event.getPlayer().hasPermission(Perm.SEESPECTATOR.getPerm())) {
			return;
		}*/

		try {
			EnumWrappers.PlayerInfoAction action = event.getPacket().getPlayerInfoAction().read(0);

			if (action == EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE
					|| action == EnumWrappers.PlayerInfoAction.ADD_PLAYER) {
				List<PlayerInfoData> newInfoList = new java.util.ArrayList<>();

				for (PlayerInfoData infoData : event.getPacket().getPlayerInfoDataLists().read(0)) {
					if (infoData.getGameMode() == EnumWrappers.NativeGameMode.SPECTATOR
							&& !infoData.getProfile().getUUID().equals(event.getPlayer().getUniqueId())) {
						newInfoList.add(new PlayerInfoData(infoData.getProfile(), infoData.getLatency(),
								EnumWrappers.NativeGameMode.CREATIVE, infoData.getDisplayName()));

						// Players still can't go through blocks even with the above condition
					}
				}

				if (!newInfoList.isEmpty()) {
					event.getPacket().getPlayerInfoDataLists().write(0, newInfoList);
				}
			}
		} catch (com.comphenix.protocol.reflect.FieldAccessException ex) {
			ex.printStackTrace();
		}
	}

	private class EntitySpawn extends PacketAdapter {

		EntitySpawn() {
			super(PLUGIN, ListenerPriority.HIGH, PacketType.Play.Server.NAMED_ENTITY_SPAWN);
		}

		@Override
		public void onPacketSending(PacketEvent event) {
			Player eventPlayer = event.getPlayer();

			for (TabListUser user : PLUGIN.getUsers()) {
				HidePlayers hp = ((TabListPlayer) user).getHidePlayers();

				if (hp != null) {
					Player userPlayer = user.getPlayer();

					if (userPlayer == null) {
						continue;
					}

					hp.addPlayerToTab(userPlayer, eventPlayer);
					hp.addPlayerToTab(eventPlayer, userPlayer);
					hp.removePlayerFromTab(eventPlayer, userPlayer);
					hp.removePlayerFromTab(userPlayer, eventPlayer);
				}
			}
		}
	}
}
