package hu.montlikadani.tablist.packets;

public final class PacketNM {

	public static final IPacketNM NMS_PACKET;

	static {
		switch (hu.montlikadani.tablist.utils.ServerVersion.getCurrent()) {
		case v1_19_R2:
			NMS_PACKET = new V1_19_R2();
			break;
		default:
			NMS_PACKET = new LegacyVersion();
			break;
		}
	}
}
