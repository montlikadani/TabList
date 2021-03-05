package hu.montlikadani.tablist.bukkit;

import java.lang.reflect.Field;
import java.util.List;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.tablist.playerlist.HidePlayers;
import hu.montlikadani.tablist.bukkit.user.TabListPlayer;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;

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
			Object packetPlayOutPlayerInfo = event.getPacket().getHandle();
			Class<?> enumPlayerInfoAction = ReflectionUtils.Classes.getEnumPlayerInfoAction(packetPlayOutPlayerInfo.getClass());
			Class<?> enumGameMode = ReflectionUtils.getNMSClass("EnumGamemode");
			if (enumGameMode == null) {
				enumGameMode = ReflectionUtils.getNMSClass("WorldSettings$EnumGamemode");
			}

			Object action = ReflectionUtils.getField(packetPlayOutPlayerInfo, "a").get(packetPlayOutPlayerInfo);

			if (action == ReflectionUtils.getField(enumPlayerInfoAction, "UPDATE_GAME_MODE").get(enumPlayerInfoAction)
					|| action == ReflectionUtils.getField(enumPlayerInfoAction, "ADD_PLAYER")
							.get(enumPlayerInfoAction)) {
				@SuppressWarnings("unchecked")
				List<Object> infoList = (List<Object>) ReflectionUtils.getField(packetPlayOutPlayerInfo, "b")
						.get(packetPlayOutPlayerInfo);
				for (Object infoData : infoList) {
					Field c = ReflectionUtils.getField(infoData, "c");
					Object profile = ReflectionUtils.invokeMethod(infoData, "a");
					Object id = ReflectionUtils.invokeMethod(profile, "getId");
					if (c.get(infoData).equals(ReflectionUtils.getField(enumGameMode, "SPECTATOR").get(enumGameMode))
							&& !(id.equals(event.getPlayer().getUniqueId()))) {
						ReflectionUtils.modifyFinalField(c, infoData,
								ReflectionUtils.getField(enumGameMode, "SURVIVAL").get(enumGameMode));
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private class EntitySpawn extends PacketAdapter {

		EntitySpawn() {
			super(PLUGIN, ListenerPriority.HIGH, PacketType.Play.Server.NAMED_ENTITY_SPAWN);
		}

		@Override
		public void onPacketSending(PacketEvent event) {
			for (TabListUser user : PLUGIN.getUsers()) {
				org.bukkit.entity.Player userPlayer = user.getPlayer(), eventPlayer = event.getPlayer();
				HidePlayers hp = ((TabListPlayer) user).getHidePlayers();

				if (hp != null) {
					hp.addPlayerToTab(userPlayer);
					hp.addPlayerToTab(eventPlayer);
					hp.removePlayerFromTab(eventPlayer, userPlayer);
					hp.removePlayerFromTab(userPlayer, eventPlayer);
				}
			}
		}
	}
}
