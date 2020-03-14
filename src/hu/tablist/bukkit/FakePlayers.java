package hu.montlikadani.tablist.bukkit;

import java.lang.reflect.Array;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.tablist.bukkit.ServerVersion.Version;

public class FakePlayers {

	private String name;

	private Object fakePl = null;
	private GameProfile profile = null;
	private Class<?> enumPlayerInfoAction = null;

	public FakePlayers(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void createFakeplayer(Player p) {
		try {
			Class<?> server = null;
			try {
				server = ReflectionUtils.getNMSClass("MinecraftServer");
			} catch (ClassNotFoundException c) {
				server = ReflectionUtils.getNMSClass("DedicatedServer");
			}
			Object serverIns = null;
			try {
				serverIns = server.getMethod("getServer")
						.invoke(ReflectionUtils.getCraftClass("CraftServer").cast(Bukkit.getServer()));
			} catch (Exception x) {
				serverIns = server.getMethod("getServer").invoke(server);
			}

			profile = new GameProfile(UUID.randomUUID(), name);

			Class<?> manager = ReflectionUtils.getNMSClass("PlayerInteractManager");
			Object managerIns = null;
			Object world = null;
			if (Version.isCurrentEqualOrHigher(Version.v1_14_R1)) {
				world = ReflectionUtils.getHandle(p.getWorld());
				managerIns = manager.getConstructor(world.getClass()).newInstance(world);
			} else if (Version.isCurrentEqual(Version.v1_13_R1) || Version.isCurrentEqual(Version.v1_13_R2)) {
				world = ReflectionUtils.getHandle(p.getWorld());
			} else {
				world = server.getDeclaredMethod("getWorldServer", int.class).invoke(serverIns, 0);
			}

			if (managerIns == null) {
				managerIns = manager.getConstructors()[0].newInstance(world);
			}

			Object player = ReflectionUtils.getHandle(p);
			fakePl = player.getClass().getConstructor(server, world.getClass(), profile.getClass(), manager)
					.newInstance(serverIns, world, profile, managerIns);

			ReflectionUtils.setField(fakePl, "listName", ReflectionUtils.getAsIChatBaseComponent(profile.getName()));

			if (Version.isCurrentEqual(Version.v1_8_R1)) {
				enumPlayerInfoAction = ReflectionUtils.getNMSClass("EnumPlayerInfoAction");
			} else if (Version.isCurrentEqualOrHigher(Version.v1_11_R1)) {
				enumPlayerInfoAction = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo").getDeclaredClasses()[1];
			}

			if (enumPlayerInfoAction == null) {
				enumPlayerInfoAction = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo").getDeclaredClasses()[2];
			}

			Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakePl);

			Object packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo")
					.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass()).newInstance(ReflectionUtils
							.getFieldObject(enumPlayerInfoAction, enumPlayerInfoAction.getDeclaredField("ADD_PLAYER")),
							entityPlayerArray);

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void removeFakePlayer() {
		try {
			ReflectionUtils.setField(fakePl, "listName", ReflectionUtils.getAsIChatBaseComponent(profile.getName()));

			Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakePl);

			Object packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo")
					.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass())
					.newInstance(ReflectionUtils.getFieldObject(enumPlayerInfoAction,
							enumPlayerInfoAction.getDeclaredField("REMOVE_PLAYER")), entityPlayerArray);

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}