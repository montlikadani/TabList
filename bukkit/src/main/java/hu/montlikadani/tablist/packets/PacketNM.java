package hu.montlikadani.tablist.packets;

import hu.montlikadani.api.IPacketNM;
import hu.montlikadani.tablist.utils.ServerVersion;

public final class PacketNM {

	public static final IPacketNM NMS_PACKET;

	static {
		IPacketNM packetInstance;
		ServerVersion serverVersion = ServerVersion.current();
		String current = serverVersion.name();

		if (serverVersion == ServerVersion.v1_19) {
			current += "_1"; // Just because I messed up
		}

		try {
			packetInstance = (IPacketNM) Class.forName("hu.montlikadani." + current + "." + current)
					.getConstructor().newInstance();
		} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
				 java.lang.reflect.InvocationTargetException e) {
			packetInstance = new LegacyVersion();
		}

		NMS_PACKET = packetInstance;
	}
}
