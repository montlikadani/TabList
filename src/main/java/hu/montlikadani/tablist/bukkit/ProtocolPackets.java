package hu.montlikadani.tablist.bukkit;

import java.lang.reflect.Field;
import java.util.List;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public class ProtocolPackets extends PacketAdapter {

	static void onSpectatorChange() {
		ProtocolLibrary.getProtocolManager().removePacketListeners(TabList.getInstance());

		ProtocolPackets s = new ProtocolPackets();

		if (ConfigValues.isRemoveGrayColorFromTabInSpec()) {
			ProtocolLibrary.getProtocolManager().addPacketListener(s);
		}

		if (ConfigValues.isHidePlayersFromTab()) {
			ProtocolLibrary.getProtocolManager().addPacketListener(s.new EntitySpawn());
		}
	}

	private ProtocolPackets() {
		super(TabList.getInstance(), PacketType.Play.Server.PLAYER_INFO);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		/*if (event.getPlayer().hasPermission(Perm.SEESPECTATOR.getPerm())) {
			return;
		}*/

		try {
			Object packetPlayOutPlayerInfo = event.getPacket().getHandle();
			Class<?> enumPlayerInfoAction = null;
			if (Version.isCurrentEqual(Version.v1_8_R1)) {
				enumPlayerInfoAction = ReflectionUtils.getNMSClass("EnumPlayerInfoAction");
			} else if (Version.isCurrentEqualOrHigher(Version.v1_11_R1)) {
				enumPlayerInfoAction = packetPlayOutPlayerInfo.getClass().getDeclaredClasses()[1];
			}

			if (enumPlayerInfoAction == null) {
				enumPlayerInfoAction = packetPlayOutPlayerInfo.getClass().getDeclaredClasses()[2];
			}

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
			super(TabList.getInstance(), ListenerPriority.HIGH, PacketType.Play.Server.NAMED_ENTITY_SPAWN);
		}

		@Override
		public void onPacketSending(PacketEvent event) {
			for (org.bukkit.entity.Player pl : org.bukkit.Bukkit.getOnlinePlayers()) {
				if (TabList.getInstance().getHidePlayers().containsKey(pl)) {
					HidePlayers hp = TabList.getInstance().getHidePlayers().get(pl);
					hp.addPlayerToTab(pl);
					hp.addPlayerToTab(event.getPlayer());
					hp.removePlayerFromTab(event.getPlayer(), pl);
					hp.removePlayerFromTab(pl, event.getPlayer());
				}
			}
		}
	}
}
