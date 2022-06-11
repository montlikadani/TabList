package hu.montlikadani.tablist.utils.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.tablist.TabText;
import hu.montlikadani.tablist.utils.ServerVersion;

public final class ReflectionUtils {

	public static final Object EMPTY_COMPONENT;

	public static Method jsonComponentMethod;

	private static JsonComponent jsonComponent;
	private static Method playerHandleMethod, sendPacketMethod, chatSerializerMethodA;
	private static Field playerConnectionField;
	private static Class<?> chatSerializer;

	private static ReentrantLock LOCK;

	static {
		try {
			Class<?>[] declaredClasses = ClazzContainer.getIChatBaseComponent().getDeclaredClasses();

			if (declaredClasses.length != 0) {
				jsonComponentMethod = declaredClasses[0].getMethod("a", String.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		EMPTY_COMPONENT = emptyComponent();

		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_16_R1)) {
			LOCK = new ReentrantLock();
		}
	}

	private ReflectionUtils() {
	}

	private static Object emptyComponent() {
		try {
			return getAsIChatBaseComponent("");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static JsonComponent getJsonComponent() {
		if (jsonComponent == null) {
			try {
				jsonComponent = new JsonComponent();
			} catch (NoClassDefFoundError e) {
			}
		}

		return jsonComponent;
	}

	public static Object getPlayerHandle(Player player) throws Exception {
		if (playerHandleMethod == null) {
			playerHandleMethod = player.getClass().getDeclaredMethod("getHandle");
		}

		return playerHandleMethod.invoke(player);
	}

	public static Object getAsIChatBaseComponent(TabText text) throws Exception {
		if (text.getJsonElements().isEmpty()) {
			return getAsIChatBaseComponent(text.getPlainText());
		}

		if (LOCK != null) {

			// JsonComponent#parseProperty takes a bit longer time than expected and in some
			// circumstances it can cause ThreadDeath (deadlock) because of the synchronized
			// method. With this lock now the current thread will be paused until the thread
			// unlocks this lock. So multiple thread can await for it to be done.
			LOCK.lock();

			Object component;
			try {
				component = getJsonComponent().parseProperty(text.getPlainText(), text.getJsonElements());
			} finally {
				LOCK.unlock();
			}

			return component;
		}

		// A bit complicated way to split json and texts from each other
		StringBuilder result = new StringBuilder();
		String strJson = text.getJsonElements().get(0).plainJson, plainText = text.getPlainText();
		int textLength = -1, from = 0, index = 0;

		int jsonIndex = plainText.indexOf(strJson);

		if (jsonIndex != -1) {
			result.append(
					"{\"text\":\"" + plainText.substring(from, jsonIndex) + "\",\"extra\":" + strJson.replace("}]}]", "}]}"));
		}

		while (jsonIndex != -1) {
			if (textLength == -1) {
				textLength = plainText.length();
			}

			int length = strJson.length();

			if ((from = jsonIndex + length) >= textLength) {
				from = length;
			}

			result.append(",");
			index++;

			if (index >= text.getJsonElements().size()) {
				break;
			}

			strJson = text.getJsonElements().get(index).plainJson;

			if ((jsonIndex = plainText.indexOf(strJson, from)) == -1) {
				break;
			}

			result.append("{\"text\":\"" + plainText.substring(from, jsonIndex) + "\",\"extra\":" + strJson + "}");
		}

		// The remaining text after last json
		if (from != 0) {
			result.append("{\"text\":\"" + plainText.substring(from) + "\"}]}");
		} else {
			result.setLength(0);
			result.append("{\"text\":\"" + plainText + "\"}");
		}

		if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
			return asChatSerializer(result.toString());
		}

		return jsonComponentMethod.invoke(ClazzContainer.getIChatBaseComponent(), result.toString());
	}

	public static Object getAsIChatBaseComponent(final String text) throws Exception {
		if (LOCK != null) {
			LOCK.lock();

			Object component;
			try {
				component = getJsonComponent().parseProperty(text, null);
			} finally {
				LOCK.unlock();
			}

			return component;
		}

		if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
			return asChatSerializer("{\"text\":\"" + text + "\"}");
		}

		return jsonComponentMethod.invoke(ClazzContainer.getIChatBaseComponent(), "{\"text\":\"" + text + "\"}");
	}

	private static Object asChatSerializer(String json) throws Exception {
		if (chatSerializer == null) {
			chatSerializer = Class.forName("net.minecraft.server." + ServerVersion.getArrayVersion()[3] + ".ChatSerializer");
		}

		if (chatSerializerMethodA == null) {
			chatSerializerMethodA = chatSerializer.getMethod("a", String.class);
		}

		return ClazzContainer.getIChatBaseComponent().cast(chatSerializerMethodA.invoke(chatSerializer, json));
	}

	public static Class<?> getCraftClass(String className) throws ClassNotFoundException {
		return Class.forName("org.bukkit.craftbukkit." + ServerVersion.getArrayVersion()[3] + "." + className);
	}

	public static void sendPacket(Player player, Object packet) {
		if (player == null) {
			return;
		}

		try {
			Object playerHandle = getPlayerHandle(player);

			if (playerConnectionField == null) {
				playerConnectionField = playerHandle.getClass().getDeclaredField(
						(ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1) ? "b" : "playerConnection"));
			}

			Object playerConnection = playerConnectionField.get(playerHandle);

			if (sendPacketMethod == null) {
				sendPacketMethod = playerConnection.getClass().getDeclaredMethod(
						ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_18_R1) ? "a" : "sendPacket",
						ClazzContainer.getPacket());
			}

			sendPacketMethod.invoke(playerConnection, packet);
		} catch (Exception e) {
		}
	}

	private static Class<?> minecraftServer, interactManager;
	private static Method getHandleWorldMethod;
	private static Constructor<?> entityPlayerConstructor, interactManagerConstructor;

	public static Object getNewEntityPlayer(Object profile) {
		if (minecraftServer == null) {
			try {
				minecraftServer = ClazzContainer.classByName("net.minecraft.server", "MinecraftServer");
			} catch (ClassNotFoundException c) {
				try {
					minecraftServer = ClazzContainer.classByName("net.minecraft.server.dedicated", "DedicatedServer");
				} catch (ClassNotFoundException e) {
				}
			}
		}

		try {
			// Only get the first world
			org.bukkit.World world = org.bukkit.Bukkit.getServer().getWorlds().get(0);

			if (getHandleWorldMethod == null) {
				getHandleWorldMethod = world.getClass().getDeclaredMethod("getHandle");
			}

			Object worldServer = getHandleWorldMethod.invoke(world);

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
				if (entityPlayerConstructor == null) {
					if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1)) {
						entityPlayerConstructor = ClazzContainer.classByName("net.minecraft.server.level", "EntityPlayer")
								.getConstructor(minecraftServer, worldServer.getClass(), profile.getClass(),
										ClazzContainer.classByName("net.minecraft.world.entity.player", "ProfilePublicKey"));
					} else {
						entityPlayerConstructor = ClazzContainer.classByName("net.minecraft.server.level", "EntityPlayer")
								.getConstructor(minecraftServer, worldServer.getClass(), profile.getClass());
					}
				}

				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1)) {
					return entityPlayerConstructor.newInstance(getServer(minecraftServer), worldServer, profile, null);
				}

				return entityPlayerConstructor.newInstance(getServer(minecraftServer), worldServer, profile);
			}

			if (interactManager == null) {
				interactManager = ClazzContainer.classByName("net.minecraft.server.level", "PlayerInteractManager");
			}

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_14_R1)) {
				if (interactManagerConstructor == null) {
					interactManagerConstructor = interactManager.getConstructor(worldServer.getClass());
				}
			} else if (interactManagerConstructor == null) {
				interactManagerConstructor = interactManager.getConstructors()[0];
			}

			if (entityPlayerConstructor == null) {
				entityPlayerConstructor = ClazzContainer.classByName("net.minecraft.server.level", "EntityPlayer")
						.getConstructor(minecraftServer, worldServer.getClass(), profile.getClass(), interactManager);
			}

			return entityPlayerConstructor.newInstance(getServer(minecraftServer), worldServer, profile,
					interactManagerConstructor.newInstance(worldServer));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private static Method getServerMethod;
	private static Class<?> craftServerClass;

	private static Object getServer(Class<?> server) {
		if (getServerMethod == null) {
			try {
				getServerMethod = server.getMethod("getServer");
			} catch (NoSuchMethodException e) {
				return null;
			}
		}

		try {
			if (craftServerClass == null) {
				craftServerClass = getCraftClass("CraftServer");
			}

			return getServerMethod.invoke(craftServerClass.cast(org.bukkit.Bukkit.getServer()));
		} catch (Exception x) {
			try {
				return getServerMethod.invoke(server);
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}

		return null;
	}
}