package hu.montlikadani.tablist;

public enum Perm {

	FAKEPLAYERS,
	HELP,
	RELOAD,
	GROUP_META("groupmeta"),
	PLAYER_META("playermeta"),
	TOGGLE,
	TOGGLEALL("toggle.all"),
	;

	public final String permission;

	Perm() {
		this.permission = "tablist." + toString().toLowerCase(java.util.Locale.ENGLISH);
	}

	Perm(String permission) {
		this.permission = "tablist." + permission;
	}
}
