package hu.montlikadani.tablist.bukkit.tablist.fakeplayers;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.reflection.ClazzContainer;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

public class FakePlayers implements IFakePlayers {

	public String displayName = "";

	private final TabList tablist = org.bukkit.plugin.java.JavaPlugin.getPlugin(TabList.class);

	private String name = "", headId = "";
	private int ping = -1;

	private Object fakeEntityPlayer;
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
		if (fakeEntityPlayer == null) {
			return;
		}

		if (displayName == null) {
			displayName = "";
		} else {
			displayName = Util.colorMsg(displayName);
		}

		try {
			for (TabListUser user : tablist.getUsers()) {
				Player player = user.getPlayer();

				if (player == null) {
					continue;
				}

				String dName = tablist.getPlaceholders().replaceVariables(player, displayName);

				Field listName = fakeEntityPlayer.getClass().getDeclaredField("listName");
				listName.setAccessible(true);
				listName.set(fakeEntityPlayer, ReflectionUtils.getAsIChatBaseComponent(dName));

				Object entityPlayerArray = Array.newInstance(fakeEntityPlayer.getClass(), 1);
				Array.set(entityPlayerArray, 0, fakeEntityPlayer);

				Object packetPlayOutPlayerInfo = ClazzContainer.getPlayOutPlayerInfoConstructor()
						.newInstance(ClazzContainer.getUpdateDisplayName(), entityPlayerArray);

				ReflectionUtils.sendPacket(player, packetPlayOutPlayerInfo);
			}
		} catch (Exception e) {
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

			fakeEntityPlayer = ReflectionUtils.Classes.getNewEntityPlayer(profile);

			Object entityPlayerArray = Array.newInstance(fakeEntityPlayer.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakeEntityPlayer);

			Object packetPlayOutPlayerInfo = ClazzContainer.getPlayOutPlayerInfoConstructor()
					.newInstance(ClazzContainer.getAddPlayer(), entityPlayerArray);

			for (Player aOnline : tablist.getServer().getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}

			if (!displayName.isEmpty()) {
				setDisplayName(displayName);
			}

			// Setting ping should be in this place, after the player added
			setPing(pingLatency);
		} catch (Exception e) {
			e.printStackTrace();
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

		try {
			Object entityPlayerArray = Array.newInstance(fakeEntityPlayer.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakeEntityPlayer);

			Object packetPlayOutPlayerInfo = ClazzContainer.getPlayOutPlayerInfoConstructor()
					.newInstance(ClazzContainer.getUpdateLatency(), entityPlayerArray);

			Object packet = null;

			@SuppressWarnings("unchecked")
			List<Object> infoList = (List<Object>) ClazzContainer.getInfoList().get(packetPlayOutPlayerInfo);

			for (Object infoData : infoList) {
				GameProfile profile;

				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
					profile = (GameProfile) ClazzContainer.getPlayerInfoDataProfileField().get(infoData);
				} else {
					profile = (GameProfile) ClazzContainer.getPlayerInfoDataProfileMethod().invoke(infoData);
				}

				if (profile.getId().equals(this.profile.getId())) {
					Constructor<?> playerInfoDataConstr = ClazzContainer.getPlayerInfoDataConstructor();
					Object gameMode = ClazzContainer.getPlayerInfoDataGameMode().get(infoData);

					if (playerInfoDataConstr.getParameterCount() == 5) {
						packet = playerInfoDataConstr.newInstance(packetPlayOutPlayerInfo, profile, ping, gameMode,
								ReflectionUtils.getAsIChatBaseComponent(name));
					} else if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
						packet = playerInfoDataConstr.newInstance(profile, ping, gameMode,
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

			ClazzContainer.getInfoList().set(packetPlayOutPlayerInfo, java.util.Arrays.asList(packet));

			for (Player aOnline : tablist.getServer().getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
				ReflectionUtils.sendPacket(aOnline, packet);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setSkin(UUID headId) {
		if (profile == null || headId == null || ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
			return;
		}

		if (!tablist.getServer().getOnlineMode()) {
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
		if (fakeEntityPlayer == null) {
			return;
		}

		try {
			Object entityPlayerArray = Array.newInstance(fakeEntityPlayer.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakeEntityPlayer);

			Object packetPlayOutPlayerInfo = ClazzContainer.getPlayOutPlayerInfoConstructor()
					.newInstance(ClazzContainer.getRemovePlayer(), entityPlayerArray);

			for (Player aOnline : tablist.getServer().getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}