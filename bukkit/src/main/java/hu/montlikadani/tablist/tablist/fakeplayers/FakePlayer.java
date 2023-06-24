package hu.montlikadani.tablist.tablist.fakeplayers;

import com.mojang.authlib.GameProfile;
import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.Objects.ObjectTypes;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.packets.PacketNM;
import hu.montlikadani.tablist.utils.PlayerSkinProperties;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.Util;
import hu.montlikadani.tablist.utils.reflection.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class FakePlayer implements IFakePlayer {

	private final String displayName;
	private String name;
	private PlayerSkinProperties playerSkinProperties;
	private int ping = -1;

	private Object fakeEntityPlayer;
	private GameProfile profile;

	public FakePlayer(String name, String displayName, String headId, int ping) {
		setNameWithoutRenamingProfile(name);

		this.displayName = displayName == null ? "" : displayName;
		this.ping = ping;

		profile = new GameProfile(Util.tryParseId(headId).orElseGet(UUID::randomUUID), this.name);
		playerSkinProperties = new PlayerSkinProperties(this.name, profile.getId());
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public PlayerSkinProperties profileProperties() {
		return playerSkinProperties;
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
		setNameWithoutRenamingProfile(name);

		profile = new GameProfile(profile.getId(), this.name);
	}

	private void setNameWithoutRenamingProfile(String name) {
		this.name = name == null ? "" : name;

		if (this.name.length() > 16) {
			this.name = this.name.substring(0, 16);
		}
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	private Object displayNameComponent() {
		return displayName.isEmpty() ? ReflectionUtils.EMPTY_COMPONENT : ReflectionUtils.asComponent(Util.applyMinimessageFormat(Global.setSymbols(displayName)));
	}

	@Override
	public void setDisplayName(String displayName) {
		if (fakeEntityPlayer == null) {
			return;
		}

		if (displayName == null) {
			displayName = "";
		} else if (!displayName.isEmpty()) {
			displayName = Util.applyMinimessageFormat(Global.setSymbols(displayName));
		}

		Object packet = PacketNM.NMS_PACKET.updateDisplayNamePacket(fakeEntityPlayer, ReflectionUtils.asComponent(displayName), true);

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(player, packet);
		}
	}

	@Override
	public void show() {
		if (fakeEntityPlayer == null) {
			putTextureProperty(false);
			fakeEntityPlayer = PacketNM.NMS_PACKET.getNewEntityPlayer(profile);
		}

		Object dName = displayNameComponent();
		PacketNM.NMS_PACKET.setListName(fakeEntityPlayer, dName);

		Object packetAdd = PacketNM.NMS_PACKET.newPlayerInfoUpdatePacketAdd(fakeEntityPlayer);
		PacketNM.NMS_PACKET.setInfoData(packetAdd, profile.getId(), ping, dName);

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

		PacketNM.NMS_PACKET.setInfoData(info, profile.getId(), ping, displayNameComponent());

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(player, info);

			if (packetUpdateScore != null) {
				PacketNM.NMS_PACKET.sendPacket(player, packetUpdateScore);
			}
		}
	}

	@Override
	public void setSkin(PlayerSkinProperties skinProperties) {
		playerSkinProperties = skinProperties;
		putTextureProperty(true);
	}

	private void putTextureProperty(boolean debug) {
		if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
			return;
		}

		if (!Bukkit.getServer().getOnlineMode()) {
			if (debug) {
				Util.logConsole(java.util.logging.Level.WARNING, "Can't set skin for offline servers.");
			}

			return;
		}

		playerSkinProperties.retrieveTextureData().thenAcceptAsync(v -> {
			if (playerSkinProperties.textureRawValue == null || playerSkinProperties.decodedTextureValue == null) {
				return;
			}

			profile.getProperties().removeAll("textures");
			profile.getProperties().put("textures", new com.mojang.authlib.properties.Property("textures",
					playerSkinProperties.textureRawValue, playerSkinProperties.decodedTextureValue));

			if (fakeEntityPlayer == null) {
				return;
			}

			Object removeInfo = PacketNM.NMS_PACKET.removeEntityPlayers(fakeEntityPlayer);
			Object addInfo = PacketNM.NMS_PACKET.newPlayerInfoUpdatePacketAdd(fakeEntityPlayer);

			for (Player player : Bukkit.getServer().getOnlinePlayers()) {
				PacketNM.NMS_PACKET.sendPacket(player, removeInfo);
				PacketNM.NMS_PACKET.sendPacket(player, addInfo);
			}
		});
	}

	@Override
	public void remove() {
		if (fakeEntityPlayer == null) {
			return;
		}

		Object info = PacketNM.NMS_PACKET.removeEntityPlayers(fakeEntityPlayer);

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(player, info);
		}

		fakeEntityPlayer = null;
	}
}
