package hu.montlikadani.tablist.tablist.fakeplayers;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.Objects.ObjectTypes;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.packets.PacketNM;
import hu.montlikadani.tablist.utils.Pair;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.Util;
import hu.montlikadani.tablist.utils.reflection.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class FakePlayer implements IFakePlayer {

	private final String displayName;
	private String name;
	private UUID headId;
	private int ping = -1;

	private Object fakeEntityPlayer;
	private GameProfile profile;

	public FakePlayer(String name, String displayName, String headId, int ping) {
		setNameWithoutRenamingProfile(name);

		this.displayName = displayName == null ? "" : displayName;
		this.ping = ping;

		profile = new GameProfile(Util.tryParseId(headId).orElseGet(UUID::randomUUID), this.name);

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
		return displayName.isEmpty() ? ReflectionUtils.EMPTY_COMPONENT : ReflectionUtils.asComponent(Util.colorText(Global.setSymbols(displayName)));
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

		Object packet = PacketNM.NMS_PACKET.updateDisplayNamePacket(fakeEntityPlayer, ReflectionUtils.asComponent(displayName), true);

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendPacket(player, packet);
		}
	}

	@Override
	public void show() {
		if (fakeEntityPlayer == null) {
			putTextureProperty(headId, false);
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
	public void setSkin(UUID headId) {
		if (headId != null) {
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

		getSkinProperties(headId.toString()).thenAcceptAsync(pair -> {
			if (pair == null) {
				return;
			}

			profile.getProperties().removeAll("textures");
			profile.getProperties().put("textures", new com.mojang.authlib.properties.Property("textures", pair.key, pair.value));

			if (fakeEntityPlayer == null) {
				return;
			}

			//fakeEntityPlayer = PacketNM.NMS_PACKET.getNewEntityPlayer(profile);

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

	@SuppressWarnings("deprecation")
	private CompletableFuture<Pair<String, String>> getSkinProperties(String uuid) {
		return CompletableFuture.supplyAsync(() -> {
			try (InputStreamReader content = new InputStreamReader(
					new java.net.URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid).openStream())) {
				com.google.gson.JsonObject json;

				try {
					json = JsonParser.parseReader(content).getAsJsonObject();
				} catch (NoSuchMethodError e) {
					json = new JsonParser().parse(content).getAsJsonObject();
				}

				com.google.gson.JsonArray jsonArray = json.get("properties").getAsJsonArray();

				if (jsonArray.isEmpty()) {
					return null;
				}

				String value = jsonArray.get(0).getAsJsonObject().get("value").getAsString();
				String decodedValue = new String(java.util.Base64.getDecoder().decode(value));

				try {
					json = JsonParser.parseString(decodedValue).getAsJsonObject();
				} catch (NoSuchMethodError e) {
					json = new JsonParser().parse(decodedValue).getAsJsonObject();
				}

				return new Pair<>(value, json.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").getAsString());
			} catch (java.io.IOException e1) {
				e1.printStackTrace();
			}

			return null;
		});
	}
}
