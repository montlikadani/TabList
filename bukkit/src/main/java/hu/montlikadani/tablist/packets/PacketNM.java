package hu.montlikadani.tablist.packets;

import hu.montlikadani.api.IPacketNM;

public final class PacketNM {

	public static final IPacketNM NMS_PACKET;

	static {
		IPacketNM packetInstance;
		String current = hu.montlikadani.tablist.utils.ServerVersion.current().name();
		int length = current.length();
		int count = 0;

		for (int i = 0; i < length; i++) {
			if (current.charAt(i) == '_') {
				count++;
			}
		}

		if (count == 1) {
			current += "_1";
		}

		try {
			packetInstance = (IPacketNM) Class.forName("hu.montlikadani." + current + "." + current).getConstructor().newInstance();
		} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
				 java.lang.reflect.InvocationTargetException e) {
			packetInstance = new LegacyVersion();
		}

		NMS_PACKET = packetInstance;
	}
}
