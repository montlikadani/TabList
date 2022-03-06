package hu.montlikadani.tablist.tablist.objects;

public enum ObjectType {

	PING("PingTab"), HEARTH("showhealth"), CUSTOM("customObj"), NONE("none");

	private String name;

	public static final ObjectType[] VALUES = values();

	ObjectType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static ObjectType getByName(String name) {
		name = name.trim();

		for (ObjectType types : VALUES) {
			if (types.toString().equalsIgnoreCase(name)) {
				return types;
			}
		}

		return NONE;
	}
}
