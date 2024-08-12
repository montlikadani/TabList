package hu.montlikadani.tablist.packets;

import hu.montlikadani.api.IPacketNM;
import hu.montlikadani.tablist.utils.ServerVersion;

public final class PacketNM {

	public static final IPacketNM NMS_PACKET;

	static {
		IPacketNM packetInstance;
		ServerVersion serverVersion = ServerVersion.current();
		String current = serverVersion.toString();

		if (serverVersion == ServerVersion.v1_19) {
			current += "_1"; // Just because I messed up
		}

		try {
			packetInstance = (IPacketNM) Class.forName("hu.montlikadani." + current + "." + current)
					.getConstructor().newInstance();
		} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
				 java.lang.reflect.InvocationTargetException e) {
			try {
				hu.montlikadani.tablist.utils.Util.legacyNmsVersion(); // Since we can not catch exception from class init
				packetInstance = new LegacyVersion();
			} catch (ArrayIndexOutOfBoundsException ex) {
				packetInstance = null;
			}
		}

		NMS_PACKET = packetInstance;
	}
}
