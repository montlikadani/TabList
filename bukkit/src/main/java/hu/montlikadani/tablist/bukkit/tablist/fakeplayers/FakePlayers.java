package hu.montlikadani.tablist.bukkit.tablist.fakeplayers;

import java.lang.reflect.Array;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.reflection.NMSContainer;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

public class FakePlayers implements IFakePlayers {

	private String name, displayName = "";

	private static int id = 0;

	private int ping = -1;

	private Object fakePl;
	private GameProfile profile;

	public FakePlayers() {
		id++;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name == null ? "" : name;

		if (displayName == null || this.name.equals(displayName)) {
			setDisplayName(this.name);
		}
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public void setDisplayName(String displayName) {
		if (displayName == null) {
			this.displayName = name;
		} else {
			// TODO we need here DI with netty#Channel from playerConnection in order to
			// replace all player placeholders, can do it with async scheduler but we don't
			// want another running thread

			displayName = Util.colorMsg(displayName);
			this.displayName = displayName;
		}

		if (fakePl != null) {
			try {
				ReflectionUtils.setField(fakePl, "listName", ReflectionUtils.getAsIChatBaseComponent(displayName));

				Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
				Array.set(entityPlayerArray, 0, fakePl);

				Object packetPlayOutPlayerInfo = NMSContainer.getPacketPlayOutPlayerInfo()
						.getConstructor(NMSContainer.getEnumPlayerInfoAction(), entityPlayerArray.getClass())
						.newInstance(NMSContainer.getUpdateDisplayName(), entityPlayerArray);

				for (Player aOnline : Bukkit.getOnlinePlayers()) {
					ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public int getPingLatency() {
		return ping;
	}

	@Override
	public void createFakePlayer(Player p) {
		createFakePlayer(p, "", -1);
	}

	@Override
	public void createFakePlayer(Player p, int pingLatency) {
		createFakePlayer(p, "", pingLatency);
	}

	@Override
	public void createFakePlayer(Player p, String headId, int pingLatency) {
		if (profile == null) {
			profile = new GameProfile(UUID.randomUUID(), name);
		}

		try {
			Util.tryParseId(headId).ifPresent(this::setSkin);

			fakePl = ReflectionUtils.Classes.getPlayerConstructor(p, profile);

			ReflectionUtils.setField(fakePl, "listName", ReflectionUtils.getAsIChatBaseComponent(displayName));

			Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakePl);

			Object packetPlayOutPlayerInfo = NMSContainer.getPacketPlayOutPlayerInfo()
					.getConstructor(NMSContainer.getEnumPlayerInfoAction(), entityPlayerArray.getClass())
					.newInstance(NMSContainer.getAddPlayer(), entityPlayerArray);

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}

			// Setting ping should be in this place, after the player added
			setPing(pingLatency);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setPing(int pingAmount) {
		if (profile == null || pingAmount < 0) {
			return;
		}

		ping = pingAmount;

		try {
			Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakePl);

			Object packetPlayOutPlayerInfo = NMSContainer.getPacketPlayOutPlayerInfo()
					.getConstructor(NMSContainer.getEnumPlayerInfoAction(), entityPlayerArray.getClass())
					.newInstance(NMSContainer.getUpdateLatency(), entityPlayerArray);

			@SuppressWarnings("unchecked")
			List<Object> infoList = (List<Object>) ReflectionUtils.getField(packetPlayOutPlayerInfo, "b")
					.get(packetPlayOutPlayerInfo);
			for (Object infoData : infoList) {
				Object profile = ReflectionUtils.invokeMethod(infoData, "a");
				Object id = ReflectionUtils.invokeMethod(profile, "getId");
				if (id.equals(this.profile.getId())) {
					ReflectionUtils.modifyFinalField(ReflectionUtils.getField(infoData, "b"), infoData, ping);
					break;
				}
			}

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setSkin(UUID headId) {
		if (profile == null || headId == null || ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
			return;
		}

		if (!Bukkit.getServer().getOnlineMode()) {
			Util.logConsole(java.util.logging.Level.WARNING, "Can't set skin for offline servers.");
			return;
		}

		ReflectionUtils.getJsonComponent().getSkinValue(headId.toString()).thenAcceptAsync(map -> {
			java.util.Map.Entry<String, String> e = map.pollFirstEntry();
			profile.getProperties().get("textures").clear();
			profile.getProperties().put("textures", new Property("textures", e.getKey(), e.getValue()));
		});
	}

	@Override
	public void removeFakePlayer() {
		if (fakePl == null) {
			return;
		}

		try {
			Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakePl);

			Object packetPlayOutPlayerInfo = NMSContainer.getPacketPlayOutPlayerInfo()
					.getConstructor(NMSContainer.getEnumPlayerInfoAction(), entityPlayerArray.getClass())
					.newInstance(NMSContainer.getRemovePlayer(), entityPlayerArray);

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}