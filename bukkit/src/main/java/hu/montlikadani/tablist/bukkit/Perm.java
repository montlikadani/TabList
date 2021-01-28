package hu.montlikadani.tablist.bukkit;

public enum Perm {

	FAKEPLAYERS,
	HELP,
	RELOAD,
	GROUP_META("groupmeta"),
	PLAYER_META("playermeta"),
	TOGGLE,
	TOGGLEALL("toggle.all"),
	//SEESPECTATOR("seespectators"),
	;

	private String perm;

	Perm() {
		this("");
	}

	Perm(String perm) {
		this.perm = "tablist." + (perm.isEmpty() ? toString().toLowerCase() : perm);
	}

	public String getPerm() {
		return perm;
	}
}
