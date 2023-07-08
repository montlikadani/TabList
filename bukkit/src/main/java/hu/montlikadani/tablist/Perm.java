package hu.montlikadani.tablist;

public enum Perm {

	FAKEPLAYERS,
	RELOAD,
	GROUP_META("groupmeta"),
	PLAYER_META("playermeta"),
	TOGGLE,
	TOGGLEALL("toggle.all"),
	;

	public final String value;

	Perm() {
		value = "tablist." + name().toLowerCase(java.util.Locale.ENGLISH);
	}

	Perm(String extra) {
		value = "tablist." + extra;
	}
}
