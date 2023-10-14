package hu.montlikadani.tablist.packets;

import hu.montlikadani.api.IPacketNM;

public final class PacketNM {

	public static final IPacketNM NMS_PACKET;

	static {
		IPacketNM packetInstance;
		String current = hu.montlikadani.tablist.utils.ServerVersion.getCurrent().name();

		try {
			packetInstance = (IPacketNM) Class.forName("hu.montlikadani." + current + "." + current.replace('v', 'V'))
					.getConstructor().newInstance();
		} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
				 java.lang.reflect.InvocationTargetException e) {
			packetInstance = new LegacyVersion();
		}

		NMS_PACKET = packetInstance;
	}
}
