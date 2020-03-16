package hu.montlikadani.tablist.bukkit.listeners;

import java.lang.reflect.Field;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.mojang.authlib.GameProfile;

import hu.montlikadani.ragemode.ServerVersion.Version;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;

public class SpectatorVisible implements Listener {

	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent e) {
		// TODO: Add config option

		if (!e.getNewGameMode().equals(org.bukkit.GameMode.SPECTATOR)) {
			return;
		}

		// double check if plugin is enabled
		if (!TabList.getInstance().isPluginEnabled("ProtocolLib")) {
			return;
		}

		ProtocolLibrary.getProtocolManager()
				.addPacketListener(new PacketAdapter(TabList.getInstance(), PacketType.Play.Server.PLAYER_INFO) {
					@Override
					public void onPacketSending(PacketEvent ev) {
						try {
							Object packetPlayOutPlayerInfo = ev.getPacket().getHandle();
							Class<?> enumPlayerInfoAction = null;
							if (Version.isCurrentLower(Version.v1_9_R1)) {
								enumPlayerInfoAction = ReflectionUtils.getNMSClass("EnumPlayerInfoAction");
							} else {
								enumPlayerInfoAction = packetPlayOutPlayerInfo.getClass().getDeclaredClasses()[1];
							}

							if (enumPlayerInfoAction == null) {
								enumPlayerInfoAction = packetPlayOutPlayerInfo.getClass().getDeclaredClasses()[2];
							}

							Class<?> enumGameMode = ReflectionUtils.getNMSClass("EnumGamemode");
							if (enumGameMode == null) {
								enumGameMode = ReflectionUtils.getNMSClass("WorldSettings$EnumGamemode");
							}

							Object action = ReflectionUtils.getField(packetPlayOutPlayerInfo, "a")
									.get(packetPlayOutPlayerInfo);

							if (action == ReflectionUtils.getField(enumPlayerInfoAction, "UPDATE_GAME_MODE")
									.get(enumPlayerInfoAction)
									|| action == ReflectionUtils.getField(enumPlayerInfoAction, "ADD_PLAYER")
											.get(enumPlayerInfoAction)) {
								@SuppressWarnings("unchecked")
								List<Object> infoList = (List<Object>) ReflectionUtils
										.getField(packetPlayOutPlayerInfo, "b").get(packetPlayOutPlayerInfo);
								for (Object infoData : infoList) {
									// PlayerInfoData data = (PlayerInfoData) infoData;
									// TODO: need a new instance declaring of playerinfodata
									Field c = ReflectionUtils.getField(infoData, "c");
									if (c.get(infoData).equals(
											ReflectionUtils.getField(enumGameMode, "SPECTATOR").get(enumGameMode))) {
										GameProfile profile = new GameProfile(e.getPlayer().getUniqueId(),
												e.getPlayer().getName());
										Object d = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo$PlayerInfoData")
												.getConstructor(profile.getClass(), int.class, enumGameMode,
														String.class)
												.newInstance(profile, 0, enumGameMode.newInstance(), ReflectionUtils
														.getAsIChatBaseComponent(e.getPlayer().getPlayerListName()));
										ReflectionUtils.modifyFinalField(c, d,
												ReflectionUtils.getField(enumGameMode, "SURVIVAL"));
									}
								}
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				});
	}
}
