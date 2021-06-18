package hu.montlikadani.tablist.bukkit.tablist;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.reflection.NMSContainer;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

public abstract class TabTitle {

	private static Class<?> playerListHeaderFooter;
	private static java.lang.reflect.Constructor<?> playerListHeaderFooterConstructor;

	static {
		try {
			playerListHeaderFooter = ReflectionUtils.getNMSClass("PacketPlayOutPlayerListHeaderFooter");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		try {
			playerListHeaderFooterConstructor = playerListHeaderFooter.getConstructor();
		} catch (NoSuchMethodException e) {
			try {
				playerListHeaderFooterConstructor = playerListHeaderFooter
						.getConstructor(NMSContainer.getIChatBaseComponent());
			} catch (NoSuchMethodException ex) {
				ex.printStackTrace();
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
			try {
				Object packet = playerListHeaderFooterConstructor.newInstance(),
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
				Object packet = null;

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
			}
		} catch (Exception t) {
			t.printStackTrace();
		}
	}
}