package hu.montlikadani.tablist.sponge.tablist.objects;

public enum ObjectType {

	PING("PingTab"), HEARTH("showhealth"), CUSTOM("customObj"), NONE("none");

	private String name;

	ObjectType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static ObjectType getByName(String name) {
		for (ObjectType types : values()) {
			if (types.toString().equalsIgnoreCase(name.trim())) {
				return types;
			}
		}

		return null;
	}
}
