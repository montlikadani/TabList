package hu.montlikadani.tablist;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.PlayerInfoData;

import hu.montlikadani.tablist.api.TabListAPI;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.tablist.playerlist.HidePlayers;
import hu.montlikadani.tablist.user.TabListPlayer;
import hu.montlikadani.tablist.user.TabListUser;

import com.comphenix.protocol.wrappers.EnumWrappers;

// TODO Get rid from ProtocolLib entirely
public class ProtocolPackets extends PacketAdapter {

	private static final TabList PLUGIN = TabListAPI.getPlugin();

	static void onSpectatorChange() {
		ProtocolLibrary.getProtocolManager().removePacketListeners(PLUGIN);

		ProtocolPackets s = new ProtocolPackets();

		if (ConfigValues.isRemoveGrayColorFromTabInSpec()) {
			ProtocolLibrary.getProtocolManager().addPacketListener(s);
		}

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
			PacketContainer packet = event.getPacket().shallowClone();
			EnumWrappers.PlayerInfoAction action = packet.getPlayerInfoAction().read(0);

			if (action == EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE || action == EnumWrappers.PlayerInfoAction.ADD_PLAYER) {
				java.util.List<PlayerInfoData> dataList = packet.getPlayerInfoDataLists().read(0);
				java.util.ListIterator<PlayerInfoData> dataListIt = dataList.listIterator();
				java.util.UUID playerId = event.getPlayer().getUniqueId();

				while (dataListIt.hasNext()) {
					PlayerInfoData infoData = dataListIt.next();

					if (infoData.getGameMode() == EnumWrappers.NativeGameMode.SPECTATOR
							&& !infoData.getProfile().getUUID().equals(playerId)) {
						dataListIt.set(new PlayerInfoData(infoData.getProfile(), infoData.getLatency(),
								EnumWrappers.NativeGameMode.SURVIVAL, infoData.getDisplayName()));
					}
				}

				packet.getPlayerInfoDataLists().write(0, dataList);
				event.setPacket(packet);
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
