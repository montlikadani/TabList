package hu.montlikadani.tablist.bukkit.tablist;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;
import hu.montlikadani.tablist.bukkit.utils.Util;

public abstract class TabTitle {

	public static void sendTabTitle(Player player, String header, String footer) {
		if (player == null) {
			return;
		}

		if (header == null) header = "";
		if (footer == null) footer = "";

		if (Version.isCurrentEqualOrLower(Version.v1_15_R2)) {
			header = Util.colorMsg(header);
			footer = Util.colorMsg(footer);
		}

		try {
			Class<?> packetPlayOutPlayerListHeaderFooter = ReflectionUtils
					.getNMSClass("PacketPlayOutPlayerListHeaderFooter");

			try {
				Object packet = packetPlayOutPlayerListHeaderFooter.getConstructor().newInstance(),
						tabHeader = ReflectionUtils.getAsIChatBaseComponent(header),
						tabFooter = ReflectionUtils.getAsIChatBaseComponent(footer);

				if (Version.isCurrentEqualOrHigher(Version.v1_13_R2)) {
					ReflectionUtils.setField(packet, "header", tabHeader);
					ReflectionUtils.setField(packet, "footer", tabFooter);
				} else {
					ReflectionUtils.setField(packet, "a", tabHeader);
					ReflectionUtils.setField(packet, "b", tabFooter);
				}

				ReflectionUtils.sendPacket(player, packet);
			} catch (Exception f) {
				java.lang.reflect.Constructor<?> titleConstructor = null;
				if (Version.isCurrentEqualOrHigher(Version.v1_12_R1)) {
					titleConstructor = packetPlayOutPlayerListHeaderFooter.getConstructor();
				} else if (Version.isCurrentLower(Version.v1_12_R1)) {
					titleConstructor = packetPlayOutPlayerListHeaderFooter
							.getConstructor(ReflectionUtils.getAsIChatBaseComponent(header).getClass());
				}

				if (titleConstructor != null) {
					ReflectionUtils.setField(titleConstructor, "b", ReflectionUtils.getAsIChatBaseComponent(footer));
					ReflectionUtils.sendPacket(player, titleConstructor);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}