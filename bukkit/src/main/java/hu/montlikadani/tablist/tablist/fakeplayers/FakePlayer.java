package hu.montlikadani.tablist.tablist.fakeplayers;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.Objects.ObjectTypes;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.packets.PacketNM;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.Util;
import hu.montlikadani.tablist.utils.reflection.ReflectionUtils;

public final class FakePlayer implements IFakePlayer {

	private String displayName;
	private String name;
	private UUID headId;
	private int ping = -1;

	private Object chatBaseComponentName;
	private Object fakeEntityPlayer;
	private GameProfile profile;

	public FakePlayer(String name, String displayName, String headId, int ping) {
		setName(name);

		this.displayName = displayName == null ? "" : displayName;
		this.ping = ping;

		java.util.Optional<UUID> opt = Util.tryParseId(headId);
		profile = new GameProfile(opt.orElse(UUID.randomUUID()), this.name);

		this.headId = profile.getId();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public UUID getHeadId() {
		return headId;
	}

	@Override
	public int getPingLatency() {
		return ping;
	}

	@Override
	public GameProfile getProfile() {
		return profile;
	}

	@Override
	public void setName(String name) {
		this.name = name == null ? "" : name;

		if (this.name.length() > 16) {
			this.name = this.name.substring(0, 16);
		}

		try {
			chatBaseComponentName = this.name.isEmpty() ? ReflectionUtils.EMPTY_COMPONENT : ReflectionUtils.asComponent(this.name);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public void setDisplayName(String displayName) {
		if (fakeEntityPlayer == null) {
			return;
		}

		if (displayName == null) {
			displayName = "";
		} else if (!displayName.isEmpty()) {
			displayName = Util.colorText(Global.setSymbols(displayName));
		}

		Object packet = PacketNM.NMS_PACKET.updateDisplayNamePacket(fakeEntityPlayer, displayName, true);

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(player, packet);
		}
	}

	@Override
	public void show() {
		if (fakeEntityPlayer == null) {
			putTextureProperty(headId, false);

			fakeEntityPlayer = PacketNM.NMS_PACKET.getNewEntityPlayer(profile);

			PacketNM.NMS_PACKET.setListName(fakeEntityPlayer, displayName.isEmpty() ? displayName : Util.colorText(Global.setSymbols(displayName)));
		}

		Object packetAdd = PacketNM.NMS_PACKET.newPlayerInfoUpdatePacketAdd(fakeEntityPlayer);
		PacketNM.NMS_PACKET.setInfoData(packetAdd, profile.getId(), ping, chatBaseComponentName);

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(player, packetAdd);
		}
	}

	@Override
	public void setPing(int ping) {
		if (fakeEntityPlayer == null) {
			return;
		}

		if (ping < -1) {
			ping = -1;
		}

		this.ping = ping;

		Object info = PacketNM.NMS_PACKET.updateLatency(fakeEntityPlayer);
		Object packetUpdateScore = null;
		ObjectTypes objectType = ConfigValues.getObjectType();

		if (objectType == ObjectTypes.PING) {
			packetUpdateScore = PacketNM.NMS_PACKET.changeScoreboardScorePacket(objectType.getObjectName(), name, ping);
		}

		PacketNM.NMS_PACKET.setInfoData(info, profile.getId(), ping, chatBaseComponentName);

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(player, info);

			if (packetUpdateScore != null) {
				PacketNM.NMS_PACKET.sendPacket(player, packetUpdateScore);
			}
		}
	}

	@Override
	public void setSkin(UUID headId) {
		if (profile != null && headId != null) {
			putTextureProperty(this.headId = headId, true);
		}
	}

	private void putTextureProperty(UUID headId, boolean debug) {
		if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
			return;
		}

		if (!Bukkit.getServer().getOnlineMode()) {
			if (debug) {
				Util.logConsole(java.util.logging.Level.WARNING, "Can't set skin for offline servers.");
			}

			return;
		}

		ReflectionUtils.getJsonComponent().getSkinValue(headId.toString()).thenAcceptAsync(pair -> {
			if (pair != null) {
				profile.getProperties().removeAll("textures");
				profile.getProperties().put("textures", new com.mojang.authlib.properties.Property("textures", pair.key, pair.value));
			}
		}).thenAccept(v -> {
			if (fakeEntityPlayer != null) {
				fakeEntityPlayer = PacketNM.NMS_PACKET.getNewEntityPlayer(profile);

				Object removeInfo = PacketNM.NMS_PACKET.removeEntityPlayer(fakeEntityPlayer);
				Object addInfo = PacketNM.NMS_PACKET.newPlayerInfoUpdatePacketAdd(fakeEntityPlayer);

				for (Player player : Bukkit.getServer().getOnlinePlayers()) {
					PacketNM.NMS_PACKET.sendPacket(player, removeInfo);
					PacketNM.NMS_PACKET.sendPacket(player, addInfo);
				}
			}
		});
	}

	@Override
	public void remove() {
		if (fakeEntityPlayer == null) {
			return;
		}

		Object info = PacketNM.NMS_PACKET.removeEntityPlayer(fakeEntityPlayer);

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(player, info);
		}

		fakeEntityPlayer = null;
	}
}