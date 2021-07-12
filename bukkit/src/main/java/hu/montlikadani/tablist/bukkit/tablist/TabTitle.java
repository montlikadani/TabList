package hu.montlikadani.tablist.bukkit.tablist;

import java.lang.reflect.Field;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.reflection.ClazzContainer;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

public abstract class TabTitle {

	private static java.lang.reflect.Constructor<?> playerListHeaderFooterConstructor;
	private static Field headerField, footerField;

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

		Object tabHeader, tabFooter;
		try {
			tabHeader = ReflectionUtils.getAsIChatBaseComponent(header);
			tabFooter = ReflectionUtils.getAsIChatBaseComponent(footer);
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}

		try {
			Object packet;

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
				packet = playerListHeaderFooterConstructor.newInstance(tabHeader, tabFooter);
			} else {
				packet = playerListHeaderFooterConstructor.newInstance();

				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
					if (headerField == null) {
						(headerField = packet.getClass().getDeclaredField("header")).setAccessible(true);
					}

					if (footerField == null) {
						(footerField = packet.getClass().getDeclaredField("footer")).setAccessible(true);
					}
				} else {
					if (headerField == null) {
						(headerField = packet.getClass().getDeclaredField("a")).setAccessible(true);
					}

					if (footerField == null) {
						(footerField = packet.getClass().getDeclaredField("b")).setAccessible(true);
					}
				}

				headerField.set(packet, tabHeader);
				footerField.set(packet, tabFooter);
			}

			ReflectionUtils.sendPacket(player, packet);
		} catch (Exception f) {
			Object packet = null;

			try {
				try {
					packet = playerListHeaderFooterConstructor.newInstance(tabHeader);
				} catch (IllegalArgumentException e) {
					try {
						packet = playerListHeaderFooterConstructor.newInstance();
					} catch (IllegalArgumentException ex) {
					}
				}

				if (packet != null) {
					if (footerField == null) {
						(footerField = packet.getClass().getDeclaredField("b")).setAccessible(true);
					}

					footerField.set(packet, tabFooter);
					ReflectionUtils.sendPacket(player, packet);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}