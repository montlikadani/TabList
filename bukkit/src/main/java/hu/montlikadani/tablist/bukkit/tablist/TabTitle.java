package hu.montlikadani.tablist.bukkit.tablist;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.reflection.ClazzContainer;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

public abstract class TabTitle {

	private static java.lang.reflect.Constructor<?> playerListHeaderFooterConstructor;

	static {
		Class<?> playerListHeaderFooter = null;

		try {
			playerListHeaderFooter = ReflectionUtils.getPacketClass("net.minecraft.network.protocol.game",
					"PacketPlayOutPlayerListHeaderFooter");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		if (playerListHeaderFooter != null) {
			try {
				playerListHeaderFooterConstructor = playerListHeaderFooter.getConstructor();
			} catch (NoSuchMethodException s) {
				try {
					playerListHeaderFooterConstructor = playerListHeaderFooter.getConstructor(
							ClazzContainer.getIChatBaseComponent(), ClazzContainer.getIChatBaseComponent());
				} catch (NoSuchMethodException e) {
					try {
						playerListHeaderFooterConstructor = playerListHeaderFooter
								.getConstructor(ClazzContainer.getIChatBaseComponent());
					} catch (NoSuchMethodException ex) {
						ex.printStackTrace();
					}
				}
			}
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
			Object tabHeader = ReflectionUtils.getAsIChatBaseComponent(header),
					tabFooter = ReflectionUtils.getAsIChatBaseComponent(footer);

			Object packet;

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
				packet = playerListHeaderFooterConstructor.newInstance(tabHeader, tabFooter);
			} else {
				packet = playerListHeaderFooterConstructor.newInstance();

				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
					ReflectionUtils.setField(packet, "header", tabHeader);
					ReflectionUtils.setField(packet, "footer", tabFooter);
				} else {
					ReflectionUtils.setField(packet, "a", tabHeader);
					ReflectionUtils.setField(packet, "b", tabFooter);
				}
			}

			ReflectionUtils.sendPacket(player, packet);
		} catch (Exception f) {
			Object packet = null;

			try {
				try {
					if (ServerVersion.isCurrentLower(ServerVersion.v1_12_R1)) {
						packet = playerListHeaderFooterConstructor
								.newInstance(ReflectionUtils.getAsIChatBaseComponent(header));
					}
				} catch (IllegalArgumentException e) {
					try {
						packet = playerListHeaderFooterConstructor.newInstance();
					} catch (IllegalArgumentException ex) {
					}
				}

				if (packet != null) {
					ReflectionUtils.setField(packet, "b", ReflectionUtils.getAsIChatBaseComponent(footer));
					ReflectionUtils.sendPacket(player, packet);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}