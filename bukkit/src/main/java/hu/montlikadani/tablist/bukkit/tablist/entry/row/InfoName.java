package hu.montlikadani.tablist.bukkit.tablist.entry.row;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.reflection.NMSContainer;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

@SuppressWarnings("unchecked")
public final class InfoName {

	private TabList plugin;
	private int ping;

	private String currentPlayerInfoAction;
	private Object packet, rowPlayer, entityPlayer;
	private GameProfile gameProfile;

	private final Constructor<?> playOutPlayerInfoConstr = NMSContainer.getPlayOutPlayerInfoConstructor(),
			playerInfoDataConstr = NMSContainer.getPlayerInfoDataConstructor();
	private final Field infoList = NMSContainer.getInfoList();

	public InfoName(TabList plugin) {
		this.plugin = plugin;
	}

	public int getPing() {
		return ping;
	}

	public void setPing(int ping) {
		this.ping = ping >= -1 ? ping : 20;
	}

	public String getCurrentPlayerInfoAction() {
		return currentPlayerInfoAction;
	}

	public Object getPacket() {
		return packet;
	}

	public Object getRowPlayer() {
		return rowPlayer;
	}

	public Object getEntityPlayer() {
		return entityPlayer;
	}

	public GameProfile getGameProfile() {
		return gameProfile;
	}

	public void addPlayer(Player player, String text, Player forWho) {
		currentPlayerInfoAction = "ADD_PLAYER";

		try {
			if (player != null && entityPlayer != null) {
				Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
				Array.set(entityPlayerArray, 0, entityPlayer);

				packet = playOutPlayerInfoConstr.newInstance(NMSContainer.getAddPlayer(), entityPlayerArray);
			} else {
				packet = playOutPlayerInfoConstr.newInstance(NMSContainer.getAddPlayer(),
						Array.newInstance(NMSContainer.getEntityPlayerClass(), 0));
			}

			((List<Object>) infoList.get(packet)).add(rowPlayer = newPlayerInfoData(text));

			if (forWho != null) {
				ReflectionUtils.sendPacket(forWho, packet);
				ReflectionUtils.sendPacket(forWho, rowPlayer);
			} else {
				sendPacketForEveryone();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void create(String name, UUID skinId, String text) {
		if (rowPlayer != null) {
			return;
		}

		if (gameProfile == null) {
			if (name.length() > 16) {
				name = name.substring(0, 16);
			}

			gameProfile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name);
		}

		CompletableFuture<Void> comp = new CompletableFuture<>();
		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_8_R2) && Bukkit.getServer().getOnlineMode()
				&& skinId != null) {
			comp = ReflectionUtils.getJsonComponent().getSkinValue(skinId.toString()).thenAcceptAsync(map -> {
				java.util.Map.Entry<String, String> first = map.pollFirstEntry();
				gameProfile.getProperties().get("textures").clear();
				gameProfile.getProperties().put("textures", new Property("textures", first.getKey(), first.getValue()));
			});
		} else {
			comp.complete(null);
		}

		comp.thenAccept(v -> {
			addPlayer(null, text, null);
			updateDisplayName(null, text, null);
		});
	}

	public void movePlayer(Player player, int rowIndex) {
		remove(player);

		try {
			String name = String.format("%03d", rowIndex);
			if (name.length() > 16) {
				name = name.substring(0, 16);
			}

			gameProfile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name);
			entityPlayer = ReflectionUtils.getHandle(player);

			GameProfile currentProfile = (GameProfile) ReflectionUtils.invokeMethod(entityPlayer, "getProfile", true,
					true);

			gameProfile.getProperties().clear();
			gameProfile.getProperties().putAll(currentProfile.getProperties());

			Field profile = null;
			for (Field gp : entityPlayer.getClass().getSuperclass().getDeclaredFields()) {
				if (gp.getType().equals(currentProfile.getClass())) {
					profile = gp;
					ReflectionUtils.modifyFinalField(gp, entityPlayer, gameProfile);
					break;
				}
			}

			if (profile == null) {
				return;
			}

			profile.setAccessible(true);

			gameProfile = (GameProfile) profile.get(entityPlayer);

			addPlayer(player, player.getName(), null);

			ReflectionUtils.modifyFinalField(profile, entityPlayer, currentProfile);
			currentProfile = (GameProfile) ReflectionUtils.invokeMethod(entityPlayer, "getProfile", true, true);

			Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
			Array.set(entityPlayerArray, 0, entityPlayer);

			Object packet = playOutPlayerInfoConstr.newInstance(NMSContainer.getUpdateDisplayName(), entityPlayerArray);

			Object rowPlayer;
			if (playerInfoDataConstr.getParameterCount() == 5) {
				rowPlayer = playerInfoDataConstr.newInstance(packet, currentProfile, ping, NMSContainer.getGameMode(),
						ReflectionUtils.getAsIChatBaseComponent(player.getName()));
			} else {
				rowPlayer = playerInfoDataConstr.newInstance(currentProfile, ping, NMSContainer.getGameMode(),
						ReflectionUtils.getAsIChatBaseComponent(player.getName()));
			}

			infoList.set(packet, Arrays.asList(rowPlayer));

			final Object rp = rowPlayer;

			// Need some tick delay to show player
			Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
				for (TabListUser user : plugin.getUsers()) {
					Player pl = user.getPlayer();

					ReflectionUtils.sendPacket(pl, packet);
					ReflectionUtils.sendPacket(pl, rp);
				}
			}, 5L);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void remove(Player player) {
		try {
			currentPlayerInfoAction = "REMOVE_PLAYER";

			packet = playOutPlayerInfoConstr.newInstance(NMSContainer.getRemovePlayer(),
					Array.newInstance(NMSContainer.getEntityPlayerClass(), 0));

			infoList.set(packet, Arrays.asList(rowPlayer = newPlayerInfoData("")));

			sendPacketForEveryone();

			if (player != null) {
				entityPlayer = ReflectionUtils.getHandle(player);

				Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
				Array.set(entityPlayerArray, 0, entityPlayer);

				currentPlayerInfoAction = "REMOVE_PLAYER";

				packet = playOutPlayerInfoConstr.newInstance(NMSContainer.getRemovePlayer(), entityPlayerArray);

				for (TabListUser user : plugin.getUsers()) {
					ReflectionUtils.sendPacket(user.getPlayer(), packet);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateDisplayName(TabListUser user, String name, Player forWho) {
		if (user != null) {
			for (TabListUser u : plugin.getUsers()) {
				try {
					Player player = u.getPlayer();

					if (u.equals(user) && !"UPDATE_DISPLAY_NAME".equals(currentPlayerInfoAction)) {
						if (entityPlayer == null) {
							entityPlayer = ReflectionUtils.getHandle(player);
						}

						Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
						Array.set(entityPlayerArray, 0, entityPlayer);

						currentPlayerInfoAction = "UPDATE_DISPLAY_NAME";

						packet = NMSContainer.getPacketPlayOutPlayerInfo()
								.getConstructor(NMSContainer.getEnumPlayerInfoAction(), entityPlayerArray.getClass())
								.newInstance(NMSContainer.getUpdateDisplayName(), entityPlayerArray);
					}

					// ((List<Object>) infoList.get(packet)).add(rowPlayer = newPlayerInfoData(name));
					infoList.set(packet, Arrays.asList(rowPlayer = newPlayerInfoData(name)));

					ReflectionUtils.sendPacket(player, packet);
					ReflectionUtils.sendPacket(player, rowPlayer);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			try {
				// Send a new packet for empty rows
				if (!"UPDATE_DISPLAY_NAME".equals(currentPlayerInfoAction)) {
					currentPlayerInfoAction = "UPDATE_DISPLAY_NAME";

					packet = playOutPlayerInfoConstr.newInstance(NMSContainer.getUpdateDisplayName(),
							Array.newInstance(NMSContainer.getEntityPlayerClass(), 0));
				}/* else {
					ReflectionUtils.modifyFinalField(ReflectionUtils.getField(rowPlayer, "e"), rowPlayer,
					ReflectionUtils.getAsIChatBaseComponent(name));
				}*/

				infoList.set(packet, Arrays.asList(rowPlayer = newPlayerInfoData(name)));

				if (forWho != null) {
					ReflectionUtils.sendPacket(forWho, packet);
					ReflectionUtils.sendPacket(forWho, rowPlayer);
				} else {
					sendPacketForEveryone();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void setSkin(String text, UUID skinId) {
		ReflectionUtils.getJsonComponent().getSkinValue(skinId.toString()).thenAcceptAsync(map -> {
			java.util.Map.Entry<String, String> first = map.pollFirstEntry();
			gameProfile.getProperties().get("textures").clear();
			gameProfile.getProperties().put("textures", new Property("textures", first.getKey(), first.getValue()));

			try {
				currentPlayerInfoAction = "UPDATE_DISPLAY_NAME";

				packet = playOutPlayerInfoConstr.newInstance(NMSContainer.getUpdateDisplayName(),
						Array.newInstance(NMSContainer.getEntityPlayerClass(), 0));

				infoList.set(packet, Arrays.asList(rowPlayer = newPlayerInfoData(text)));

				sendPacketForEveryone();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public void sendPacketForEveryone() {
		for (TabListUser user : plugin.getUsers()) {
			Player player = user.getPlayer();

			ReflectionUtils.sendPacket(player, packet);
			ReflectionUtils.sendPacket(player, rowPlayer);
		}
	}

	private Object newPlayerInfoData(String text) throws Exception {
		if (playerInfoDataConstr.getParameterCount() == 5) {
			return playerInfoDataConstr.newInstance(packet, gameProfile, ping, NMSContainer.getGameMode(),
					ReflectionUtils.getAsIChatBaseComponent(text));
		}

		return playerInfoDataConstr.newInstance(gameProfile, ping, NMSContainer.getGameMode(),
				ReflectionUtils.getAsIChatBaseComponent(text));
	}

	public static void removePlayer(final TabList plugin, Player player) {
		if (player == null) {
			return;
		}

		try {
			Object entityPlayer = ReflectionUtils.getHandle(player);

			Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
			Array.set(entityPlayerArray, 0, entityPlayer);

			Object packet = NMSContainer.getPacketPlayOutPlayerInfo()
					.getConstructor(NMSContainer.getEnumPlayerInfoAction(), entityPlayerArray.getClass())
					.newInstance(NMSContainer.getRemovePlayer(), entityPlayerArray);

			Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
				for (TabListUser user : plugin.getUsers()) {
					ReflectionUtils.sendPacket(user.getPlayer(), packet);
				}
			}, 20L); // 1 second to remove? lmao
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addPlayer(final TabList plugin, Player player) {
		if (player == null) {
			return;
		}

		try {
			Object entityPlayer = ReflectionUtils.getHandle(player);

			Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
			Array.set(entityPlayerArray, 0, entityPlayer);

			Object packet = NMSContainer.getPacketPlayOutPlayerInfo()
					.getConstructor(NMSContainer.getEnumPlayerInfoAction(), entityPlayerArray.getClass())
					.newInstance(NMSContainer.getAddPlayer(), entityPlayerArray);

			Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
				for (TabListUser user : plugin.getUsers()) {
					ReflectionUtils.sendPacket(user.getPlayer(), packet);
				}
			}, 5L);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
