package hu.montlikadani.tablist.tablist.fakeplayers;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.Util;
import hu.montlikadani.tablist.utils.reflection.ClazzContainer;
import hu.montlikadani.tablist.utils.reflection.ReflectionUtils;

public final class FakePlayer implements IFakePlayer {

	private static Field listNameField;
	private static Class<?> entityPlayerClass;

	private final TabList tablist;

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
		profile = new GameProfile(opt.orElse(UUID.randomUUID()), name);

		this.headId = profile.getId();

		tablist = org.bukkit.plugin.java.JavaPlugin.getPlugin(TabList.class);
	}

	protected FakePlayer(TabList tablist, String name, String displayName, String headId, int ping) {
		setName(name);

		this.tablist = tablist;
		this.displayName = displayName;
		this.ping = ping;

		java.util.Optional<UUID> opt = Util.tryParseId(headId);
		profile = new GameProfile(opt.orElse(UUID.randomUUID()), name);

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

		try {
			chatBaseComponentName = this.name.isEmpty() ? ReflectionUtils.EMPTY_COMPONENT
					: ReflectionUtils.getAsIChatBaseComponent(this.name);
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

		try {
			if (listNameField == null) {
				(listNameField = entityPlayerClass.getDeclaredField("listName")).setAccessible(true);
			}

			for (TabListUser user : tablist.getUsers()) {
				if (!user.isFakePlayerVisible(this)) {
					continue;
				}

				Player player = user.getPlayer();

				if (player == null) {
					continue;
				}

				listNameField.set(fakeEntityPlayer,
						displayName.isEmpty() ? ReflectionUtils.EMPTY_COMPONENT
								: ReflectionUtils.getAsIChatBaseComponent(
										tablist.getPlaceholders().replaceVariables(player, displayName)));

				Object entityPlayerArray = Array.newInstance(entityPlayerClass, 1);
				Array.set(entityPlayerArray, 0, fakeEntityPlayer);

				ReflectionUtils.sendPacket(player, ClazzContainer.getPlayOutPlayerInfoConstructor()
						.newInstance(ClazzContainer.getUpdateDisplayName(), entityPlayerArray));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void show() {
		if (fakeEntityPlayer == null) {
			try {
				putTextureProperty(headId, false);

				fakeEntityPlayer = ReflectionUtils.getNewEntityPlayer(profile);

				if (entityPlayerClass == null) {
					entityPlayerClass = fakeEntityPlayer.getClass();
				}

				Object entityPlayerArray = Array.newInstance(entityPlayerClass, 1);
				Array.set(entityPlayerArray, 0, fakeEntityPlayer);

				Object packetPlayOutPlayerInfo = ClazzContainer.getPlayOutPlayerInfoConstructor()
						.newInstance(ClazzContainer.getAddPlayer(), entityPlayerArray);

				for (TabListUser user : tablist.getUsers()) {
					ReflectionUtils.sendPacket(user.getPlayer(), packetPlayOutPlayerInfo);
					user.setCanSeeFakePlayer(this);
				}

				if (!displayName.isEmpty()) {
					setDisplayName(displayName);
				}

				setPing(ping);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		try {
			Object entityPlayerArray = Array.newInstance(entityPlayerClass, 1);
			Array.set(entityPlayerArray, 0, fakeEntityPlayer);

			Object packetPlayOutPlayerInfo = ClazzContainer.getPlayOutPlayerInfoConstructor()
					.newInstance(ClazzContainer.getAddPlayer(), entityPlayerArray);

			setPingFrom(packetPlayOutPlayerInfo);

			for (TabListUser user : tablist.getUsers()) {
				if (!user.isFakePlayerVisible(this)) {
					ReflectionUtils.sendPacket(user.getPlayer(), packetPlayOutPlayerInfo);
					user.setCanSeeFakePlayer(this);
				}
			}
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
			Object entityPlayerArray = Array.newInstance(entityPlayerClass, 1);
			Array.set(entityPlayerArray, 0, fakeEntityPlayer);

			Object packetPlayOutPlayerInfo = ClazzContainer.getPlayOutPlayerInfoConstructor()
					.newInstance(ClazzContainer.getUpdateLatency(), entityPlayerArray);

			setPingFrom(packetPlayOutPlayerInfo);

			for (TabListUser user : tablist.getUsers()) {
				if (user.isFakePlayerVisible(this)) {
					ReflectionUtils.sendPacket(user.getPlayer(), packetPlayOutPlayerInfo);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private void setPingFrom(Object packetPlayOutPlayerInfo) throws Exception {
		for (Object infoData : (List<Object>) ClazzContainer.getInfoList().get(packetPlayOutPlayerInfo)) {
			GameProfile profile;

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
				profile = (GameProfile) ClazzContainer.getPlayerInfoDataProfileField().get(infoData);
			} else {
				profile = (GameProfile) ClazzContainer.getPlayerInfoDataProfileMethod().invoke(infoData);
			}

			if (!profile.getId().equals(this.profile.getId())) {
				continue;
			}

			Constructor<?> playerInfoDataConstr = ClazzContainer.getPlayerInfoDataConstructor();
			Object gameMode = ClazzContainer.getPlayerInfoDataGameMode().get(infoData);
			Object packet;

			switch (playerInfoDataConstr.getParameterCount()) {
			case 5:
				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1)) {
					packet = playerInfoDataConstr.newInstance(profile, ping, gameMode, chatBaseComponentName, null);
				} else {
					packet = playerInfoDataConstr.newInstance(packetPlayOutPlayerInfo, profile, ping, gameMode,
							chatBaseComponentName);
				}

				break;
			default:
				packet = playerInfoDataConstr.newInstance(profile, ping, gameMode, chatBaseComponentName);
				break;
			}

			ClazzContainer.getInfoList().set(packetPlayOutPlayerInfo, java.util.Collections.singletonList(packet));
			break;
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

		if (!tablist.getServer().getOnlineMode()) {
			if (debug) {
				Util.logConsole(java.util.logging.Level.WARNING, "Can't set skin for offline servers.");
			}

			return;
		}

		ReflectionUtils.getJsonComponent().getSkinValue(headId.toString()).thenAcceptAsync(map -> {
			java.util.Map.Entry<String, String> e = map.pollFirstEntry();

			profile.getProperties().get("textures").clear();
			profile.getProperties().put("textures",
					new com.mojang.authlib.properties.Property("textures", e.getKey(), e.getValue()));
		});

		for (TabListUser user : tablist.getUsers()) {
			if (user.isFakePlayerVisible(this)) {
				user.setCanSeeFakePlayer(this);
			}
		}
	}

	@Override
	public void remove() {
		if (fakeEntityPlayer == null) {
			return;
		}

		try {
			Object entityPlayerArray = Array.newInstance(entityPlayerClass, 1);
			Array.set(entityPlayerArray, 0, fakeEntityPlayer);

			Object packetPlayOutPlayerInfo = ClazzContainer.getPlayOutPlayerInfoConstructor()
					.newInstance(ClazzContainer.getRemovePlayer(), entityPlayerArray);

			for (TabListUser user : tablist.getUsers()) {
				Player player = user.getPlayer();

				if (player != null) {
					ReflectionUtils.sendPacket(player, packetPlayOutPlayerInfo);
					user.setCanSeeFakePlayer(this);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		fakeEntityPlayer = null;
	}
}