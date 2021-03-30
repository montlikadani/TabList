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
import hu.montlikadani.tablist.bukkit.utils.reflection.NMSContainer;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

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
			// Retrieves the packetPlayOutPlayerInfo object
			// I guess, this returns the packetPlayOutPlayerInfo constructor or a new instance
			// or probably an existing one which recently modified using observable?
			Object packetPlayOutPlayerInfo = event.getPacket().getHandle();

			// Retrieve playerInfoAction field class from packetPlayOutPlayerInfo
			Class<?> enumPlayerInfoAction = ReflectionUtils.Classes
					.getEnumPlayerInfoAction(packetPlayOutPlayerInfo.getClass());

			// Retrieve the current set of playerInfoAction field object
			Object action = ReflectionUtils.getField(packetPlayOutPlayerInfo, "a").get(packetPlayOutPlayerInfo);

			// Check if the action object is in the state of update_game_mode or add_player
			if (action == ReflectionUtils.getField(enumPlayerInfoAction, "UPDATE_GAME_MODE").get(enumPlayerInfoAction)
					|| action == ReflectionUtils.getField(enumPlayerInfoAction, "ADD_PLAYER")
							.get(enumPlayerInfoAction)) {
				// Retrieve the infoList field object from packetPlayOutPlayerInfo, explicitly
				// casted to list
				@SuppressWarnings("unchecked")
				List<Object> infoList = (List<Object>) ReflectionUtils.getField(packetPlayOutPlayerInfo, "b")
						.get(packetPlayOutPlayerInfo);
				for (Object infoData : infoList) {
					// Retrieves the game mode field from infoData object
					Field c = ReflectionUtils.getField(infoData, "c");

					// Retrieves the game profile field object from infoData object
					Object profile = ReflectionUtils.invokeMethod(infoData, "a");
					// Invokes the getId method from profile class object
					Object id = ReflectionUtils.invokeMethod(profile, "getId");

					// Get the gameMode class
					Class<?> enumGameMode = NMSContainer.getEnumGameMode();

					// Checks if the current infoData game mode is spectator and the player UUID is
					// not equal according to game profile id
					if (c.get(infoData).equals(ReflectionUtils.getField(enumGameMode, "SPECTATOR").get(enumGameMode))
							&& !id.equals(event.getPlayer().getUniqueId())) {
						// Modifies the infoData object "c" game mode field into survival object
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
					hp.addPlayerToTab(userPlayer, eventPlayer);
					hp.addPlayerToTab(eventPlayer, userPlayer);
					hp.removePlayerFromTab(eventPlayer, userPlayer);
					hp.removePlayerFromTab(userPlayer, eventPlayer);
				}
			}
		}
	}
}
