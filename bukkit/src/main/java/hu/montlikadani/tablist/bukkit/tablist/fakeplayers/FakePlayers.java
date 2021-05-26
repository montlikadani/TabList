package hu.montlikadani.tablist.bukkit.tablist.fakeplayers;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.reflection.NMSContainer;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

public class FakePlayers implements IFakePlayers {

	public String displayName = "";

	private final TabList tablist = org.bukkit.plugin.java.JavaPlugin.getPlugin(TabList.class);

	private String name = "", headId = "";
	private int ping = -1;

	private Object fakePl;
	private GameProfile profile;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getHeadId() {
		return headId;
	}

	@Override
	public void setName(String name) {
		this.name = name == null ? "" : name;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public void setDisplayName(String displayName) {
		if (fakePl == null) {
			return;
		}

		displayName = Util.colorMsg(displayName);
		this.displayName = displayName;

		try {
			for (TabListUser user : tablist.getUsers()) {
				Player player = user.getPlayer();

				if (player == null) {
					continue;
				}

				String dName = tablist.getPlaceholders().replaceVariables(player, displayName);

				ReflectionUtils.setField(fakePl, "listName", ReflectionUtils.getAsIChatBaseComponent(dName));

				Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
				Array.set(entityPlayerArray, 0, fakePl);

				Object packetPlayOutPlayerInfo = NMSContainer.getPlayOutPlayerInfoConstructor()
						.newInstance(NMSContainer.getUpdateDisplayName(), entityPlayerArray);

				ReflectionUtils.sendPacket(player, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getPingLatency() {
		return ping;
	}

	@Override
	public void createFakePlayer(String headId, int pingLatency) {
		removeFakePlayer();

		if (displayName == null) {
			displayName = "";
		}

		if (profile == null) {
			profile = new GameProfile(UUID.randomUUID(), name);
		}

		try {
			Util.tryParseId(headId).ifPresent(this::setSkin);

			fakePl = ReflectionUtils.Classes.getNewEntityPlayer(profile);

			ReflectionUtils.setField(fakePl, "listName", ReflectionUtils.getAsIChatBaseComponent(displayName));

			Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakePl);

			Object packetPlayOutPlayerInfo = NMSContainer.getPlayOutPlayerInfoConstructor()
					.newInstance(NMSContainer.getAddPlayer(), entityPlayerArray);

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}

			if (!displayName.isEmpty()) {
				setDisplayName(displayName);
			}

			// Setting ping should be in this place, after the player added
			setPing(pingLatency);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setPing(int ping) {
		if (fakePl == null || ping < -1) {
			return;
		}

		this.ping = ping;

		try {
			Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakePl);

			Object packetPlayOutPlayerInfo = NMSContainer.getPlayOutPlayerInfoConstructor()
					.newInstance(NMSContainer.getUpdateLatency(), entityPlayerArray);

			Field infoListField = ReflectionUtils.getField(packetPlayOutPlayerInfo, "b");

			Object packet = null;

			@SuppressWarnings("unchecked")
			List<Object> infoList = (List<Object>) infoListField.get(packetPlayOutPlayerInfo);
			for (Object infoData : infoList) {
				GameProfile profile = (GameProfile) ReflectionUtils.invokeMethod(infoData, "a");

				if (profile.getId().equals(this.profile.getId())) {
					Object gameMode = ReflectionUtils.getField(infoData, "c").get(infoData);

					Constructor<?> playerInfoDataConstr = NMSContainer.getPlayerInfoDataConstructor();

					if (playerInfoDataConstr.getParameterCount() == 5) {
						packet = playerInfoDataConstr.newInstance(packetPlayOutPlayerInfo, profile, ping, gameMode,
								ReflectionUtils.getAsIChatBaseComponent(name));
					} else {
						packet = playerInfoDataConstr.newInstance(profile, ping, gameMode,
								ReflectionUtils.getAsIChatBaseComponent(name));
					}

					break;
				}
			}

			if (packet == null) {
				return;
			}

			infoListField.set(packetPlayOutPlayerInfo, java.util.Arrays.asList(packet));

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
				ReflectionUtils.sendPacket(aOnline, packet);
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

		this.headId = headId.toString();

		ReflectionUtils.getJsonComponent().getSkinValue(this.headId).thenAcceptAsync(map -> {
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

			Object packetPlayOutPlayerInfo = NMSContainer.getPlayOutPlayerInfoConstructor()
					.newInstance(NMSContainer.getRemovePlayer(), entityPlayerArray);

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}