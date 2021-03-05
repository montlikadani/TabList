package hu.montlikadani.tablist.bukkit.tablist;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.Util;

public abstract class TabTitle {

	private static Class<?> playerListHeaderFooter;

	static {
		try {
			playerListHeaderFooter = ReflectionUtils.getNMSClass("PacketPlayOutPlayerListHeaderFooter");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void sendTabTitle(Player player, String header, String footer) {
		if (player == null) {
			return;
		}

		if (header == null) header = "";
		if (footer == null) footer = "";

		if (ServerVersion.isCurrentEqualOrLower(ServerVersion.v1_15_R2)) {
			header = Util.colorMsg(header);
			footer = Util.colorMsg(footer);
		}

		try {
			try {
				Object packet = playerListHeaderFooter.getConstructor().newInstance(),
						tabHeader = ReflectionUtils.getAsIChatBaseComponent(header),
						tabFooter = ReflectionUtils.getAsIChatBaseComponent(footer);

				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
					ReflectionUtils.setField(packet, "header", tabHeader);
					ReflectionUtils.setField(packet, "footer", tabFooter);
				} else {
					ReflectionUtils.setField(packet, "a", tabHeader);
					ReflectionUtils.setField(packet, "b", tabFooter);
				}

				ReflectionUtils.sendPacket(player, packet);
			} catch (Exception f) {
				java.lang.reflect.Constructor<?> titleConstructor = null;
				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_12_R1)) {
					titleConstructor = ReflectionUtils.getNMSClass("PacketPlayOutPlayerListHeaderFooter")
							.getConstructor();
				} else if (ServerVersion.isCurrentLower(ServerVersion.v1_12_R1)) {
					titleConstructor = ReflectionUtils.getNMSClass("PacketPlayOutPlayerListHeaderFooter")
							.getConstructor(ReflectionUtils.getAsIChatBaseComponent(header).getClass());
				}

				if (titleConstructor != null) {
					ReflectionUtils.setField(titleConstructor, "b", ReflectionUtils.getAsIChatBaseComponent(footer));
					ReflectionUtils.sendPacket(player, titleConstructor);
				}
			}
		} catch (Exception t) {
			t.printStackTrace();
		}
	}
}