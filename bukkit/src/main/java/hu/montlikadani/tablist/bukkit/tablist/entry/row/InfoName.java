package hu.montlikadani.tablist.bukkit.tablist.entry.row;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

@SuppressWarnings("unchecked")
public final class InfoName {

	private TabList plugin;
	private int ping;

	private String currentPlayerInfoAction;
	private Field infoList;
	private Constructor<?> playerInfoDataConstr, playOutPlayerInfoConstr;
	private Class<?> packetPlayOutPlayerInfo, enumPlayerInfoAction, entityPlayerClass;
	private Object packet, rowPlayer, gameMode, entityPlayer;
	private GameProfile gameProfile;

	public InfoName(TabList plugin) {
		this.plugin = plugin;

		try {
			packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo");

			entityPlayerClass = ReflectionUtils.getNMSClass("EntityPlayer");
			infoList = ReflectionUtils.getField(packetPlayOutPlayerInfo, "b");
			enumPlayerInfoAction = ReflectionUtils.Classes.getEnumPlayerInfoAction(packetPlayOutPlayerInfo);

			Class<?> playerInfoData = null;
			try {
				playerInfoData = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo$PlayerInfoData");
			} catch (ClassNotFoundException e) {
				playerInfoData = ReflectionUtils.getNMSClass("PlayerInfoData");
			}

			if (playerInfoData == null) {
				return;
			}

			for (Constructor<?> constr : playerInfoData.getConstructors()) {
				if (constr.getParameterCount() == 4 || constr.getParameterCount() == 5) {
					playerInfoDataConstr = constr;
					playerInfoDataConstr.setAccessible(true);
					break;
				}
			}

			playOutPlayerInfoConstr = packetPlayOutPlayerInfo.getDeclaredConstructor(enumPlayerInfoAction,
					Array.newInstance(entityPlayerClass, 0).getClass());
			playOutPlayerInfoConstr.setAccessible(true);

			Class<?> enumGameMode = ReflectionUtils.getNMSClass("EnumGamemode");
			if (enumGameMode == null) {
				enumGameMode = ReflectionUtils.getNMSClass("WorldSettings$EnumGamemode");
			}

			gameMode = enumGameMode.getDeclaredField("NOT_SET").get(enumGameMode);
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public Field getInfoList() {
		return infoList;
	}

	public Constructor<?> getPlayerInfoDataConstr() {
		return playerInfoDataConstr;
	}

	public Constructor<?> getPlayOutPlayerInfoConstr() {
		return playOutPlayerInfoConstr;
	}

	public Class<?> getPacketPlayOutPlayerInfo() {
		return packetPlayOutPlayerInfo;
	}

	public Class<?> getEnumPlayerInfoAction() {
		return enumPlayerInfoAction;
	}

	public Class<?> getEntityPlayerClass() {
		return entityPlayerClass;
	}

	public Object getPacket() {
		return packet;
	}

	public Object getRowPlayer() {
		return rowPlayer;
	}

	public Object getGameMode() {
		return gameMode;
	}

	public Object getEntityPlayer() {
		return entityPlayer;
	}

	public GameProfile getGameProfile() {
		return gameProfile;
	}

	public void addPlayer(Player player, String text) {
		currentPlayerInfoAction = "ADD_PLAYER";

		try {
			if (player != null && entityPlayer != null) {
				Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
				Array.set(entityPlayerArray, 0, entityPlayer);

				packet = playOutPlayerInfoConstr.newInstance(
						enumPlayerInfoAction.getDeclaredField(currentPlayerInfoAction).get(enumPlayerInfoAction),
						entityPlayerArray);
			} else {
				packet = playOutPlayerInfoConstr.newInstance(
						enumPlayerInfoAction.getDeclaredField(currentPlayerInfoAction).get(enumPlayerInfoAction),
						Array.newInstance(entityPlayerClass, 0));
			}

			((List<Object>) infoList.get(packet)).add(rowPlayer = playerInfoDataConstr.newInstance(packet, gameProfile,
					ping, gameMode, ReflectionUtils.getAsIChatBaseComponent(text)));

			sendPacketForEveryone();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void create(String name, UUID skinId, String text) {
		if (rowPlayer != null || playOutPlayerInfoConstr == null) {
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
			addPlayer(null, text);
			updateDisplayName(null, text, null);
		});
	}

	public void movePlayer(Player player, int rowIndex) {
		// Need 3 tick delay to show player
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			try {
				String name = String.format("%03d", rowIndex); // 00 + index - sort by row index
				if (name.length() > 16) {
					name = name.substring(0, 16);
				}

				gameProfile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name);

				entityPlayer = ReflectionUtils.getHandle(player);

				GameProfile currentProfile = (GameProfile) ReflectionUtils.invokeMethod(entityPlayer, "getProfile",
						true, true);

				gameProfile.getProperties().clear();
				gameProfile.getProperties().putAll(currentProfile.getProperties());

				Field profile = null;
				for (Field gp : entityPlayer.getClass().getSuperclass().getDeclaredFields()) {
					if (gp.getType().equals(currentProfile.getClass())) {
						ReflectionUtils.modifyFinalField(profile = gp, entityPlayer, gameProfile);
						break;
					}
				}

				if (profile == null) {
					return;
				}

				addPlayer(player, player.getName());
				sendPacketForEveryone();

				ReflectionUtils.modifyFinalField(profile, entityPlayer, currentProfile);
				sendPacketForEveryone();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 3L);
	}

	public void remove(Player player, boolean restorePlayer, String text) {
		if (rowPlayer == null) {
			return;
		}

		try {
			if (player != null) { // Restore player to default state
				if (restorePlayer) {
					addPlayer(player, plugin.getComplement().getPlayerListName(player));
				} else if (entityPlayer != null) {
					packet = playOutPlayerInfoConstr.newInstance(enumPlayerInfoAction
							.getDeclaredField(currentPlayerInfoAction = "REMOVE_PLAYER").get(enumPlayerInfoAction),
							entityPlayer);

					((List<Object>) infoList.get(packet)).add(playerInfoDataConstr.newInstance(packet, gameProfile,
							ping, gameMode,
							ReflectionUtils.getAsIChatBaseComponent(plugin.getComplement().getPlayerListName(player))));
				}
			} else {
				packet = playOutPlayerInfoConstr.newInstance(enumPlayerInfoAction
						.getDeclaredField(currentPlayerInfoAction = "REMOVE_PLAYER").get(enumPlayerInfoAction),
						Array.newInstance(entityPlayerClass, 0));

				((List<Object>) infoList.get(packet)).add(playerInfoDataConstr.newInstance(packet, gameProfile, ping,
						gameMode, ReflectionUtils.getAsIChatBaseComponent(text)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		sendPacketForEveryone();
	}

	public void updateDisplayName(TabListUser user, String name, Player forWho) {
		if (user != null) {
			for (TabListUser u : plugin.getUsers()) {
				try {
					if (u.equals(user) && !"UPDATE_DISPLAY_NAME".equals(currentPlayerInfoAction)) {
						if (entityPlayer == null) {
							entityPlayer = ReflectionUtils.getHandle(u.getPlayer());
						}

						Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
						Array.set(entityPlayerArray, 0, entityPlayer);

						packet = packetPlayOutPlayerInfo
								.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass())
								.newInstance(enumPlayerInfoAction
										.getDeclaredField(currentPlayerInfoAction = "UPDATE_DISPLAY_NAME")
										.get(enumPlayerInfoAction), entityPlayerArray);

						((List<Object>) infoList.get(packet)).add(rowPlayer = playerInfoDataConstr.newInstance(packet,
								gameProfile, ping, gameMode, ReflectionUtils.getAsIChatBaseComponent(name)));
					}

					ReflectionUtils.modifyFinalField(ReflectionUtils.getField(rowPlayer, "e"), rowPlayer,
							ReflectionUtils.getAsIChatBaseComponent(name));

					ReflectionUtils.sendPacket(u.getPlayer(), packet);
					ReflectionUtils.sendPacket(u.getPlayer(), rowPlayer);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			try {
				// Send a new packet for empty rows
				if (!"UPDATE_DISPLAY_NAME".equals(currentPlayerInfoAction)) {
					packet = playOutPlayerInfoConstr.newInstance(
							enumPlayerInfoAction.getDeclaredField(currentPlayerInfoAction = "UPDATE_DISPLAY_NAME")
									.get(enumPlayerInfoAction),
							Array.newInstance(entityPlayerClass, 0));

					((List<Object>) infoList.get(packet)).add(rowPlayer = playerInfoDataConstr.newInstance(packet,
							gameProfile, ping, gameMode, ReflectionUtils.getAsIChatBaseComponent(name)));
				}

				ReflectionUtils.modifyFinalField(ReflectionUtils.getField(rowPlayer, "e"), rowPlayer,
						ReflectionUtils.getAsIChatBaseComponent(name));

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
				packet = playOutPlayerInfoConstr.newInstance(enumPlayerInfoAction
						.getDeclaredField(currentPlayerInfoAction = "UPDATE_DISPLAY_NAME").get(enumPlayerInfoAction),
						Array.newInstance(entityPlayerClass, 0));

				((List<Object>) infoList.get(packet)).add(rowPlayer = playerInfoDataConstr.newInstance(packet,
						gameProfile, ping, gameMode, ReflectionUtils.getAsIChatBaseComponent(text)));

				sendPacketForEveryone();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public void sendPacketForEveryone() {
		for (TabListUser user : plugin.getUsers()) {
			ReflectionUtils.sendPacket(user.getPlayer(), packet);
			ReflectionUtils.sendPacket(user.getPlayer(), rowPlayer);
		}
	}

	public static void removePlayer(Player player) {
		if (player == null) {
			return;
		}

		final TabList plugin = TabListAPI.getPlugin();

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			try {
				Object entityPlayer = ReflectionUtils.getHandle(player);

				Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
				Array.set(entityPlayerArray, 0, entityPlayer);

				Class<?> packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo");
				Class<?> enumPlayerInfoAction = ReflectionUtils.Classes
						.getEnumPlayerInfoAction(packetPlayOutPlayerInfo);

				Object packet = packetPlayOutPlayerInfo
						.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass())
						.newInstance(enumPlayerInfoAction.getDeclaredField("REMOVE_PLAYER").get(enumPlayerInfoAction),
								entityPlayerArray);

				for (TabListUser user : plugin.getUsers()) {
					ReflectionUtils.sendPacket(user.getPlayer(), packet);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 20L); // 1 second to remove? lmao
	}
}
