package hu.montlikadani.tablist.bukkit.tablist.fakeplayers;

import java.lang.reflect.Array;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;

public class FakePlayers implements IFakePlayers {

	private String name;

	private Object fakePl = null;
	private GameProfile profile = null;
	private Class<?> enumPlayerInfoAction = null;

	public FakePlayers(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void createFakeplayer(Player p) {
		try {
			profile = new GameProfile(UUID.randomUUID(), name);
			fakePl = ReflectionUtils.Classes.getPlayerContructor(p, profile);

			ReflectionUtils.setField(fakePl, "listName", ReflectionUtils.getAsIChatBaseComponent(profile.getName()));

			enumPlayerInfoAction = ReflectionUtils.Classes.getEnumPlayerInfoAction();

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

	@Override
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