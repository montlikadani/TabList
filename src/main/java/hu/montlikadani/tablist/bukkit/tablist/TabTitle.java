package hu.montlikadani.tablist.bukkit.tablist;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public abstract class TabTitle {

	public static void sendTabTitle(Player player, String header, String footer) {
		if (player == null) {
			return;
		}

		if (header == null) header = "";
		if (footer == null) footer = "";

		//header = Util.colorMsg(header);
		//footer = Util.colorMsg(footer);

		/*if (Version.isCurrentEqualOrHigher(Version.v1_16_R1)) {
			player.setPlayerListHeaderFooter(header, footer);
			return;
		}*/

		try {
			java.lang.reflect.Constructor<?> titleConstructor = null;

			try {
				titleConstructor = ReflectionUtils.getNMSClass("PacketPlayOutPlayerListHeaderFooter").getConstructor();

				Object packet = titleConstructor.newInstance(),
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
				if (Version.isCurrentEqualOrHigher(Version.v1_12_R1)) {
					titleConstructor = ReflectionUtils.getNMSClass("PacketPlayOutPlayerListHeaderFooter")
							.getConstructor();
				} else if (Version.isCurrentLower(Version.v1_12_R1)) {
					Object tabHeader = ReflectionUtils.getAsIChatBaseComponent(header);
					titleConstructor = ReflectionUtils.getNMSClass("PacketPlayOutPlayerListHeaderFooter")
							.getConstructor(tabHeader.getClass());
				}

				if (titleConstructor == null) {
					return;
				}

				Object tabFooter = ReflectionUtils.getAsIChatBaseComponent(footer);
				ReflectionUtils.setField(titleConstructor, "b", tabFooter);
				ReflectionUtils.sendPacket(player, titleConstructor);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}