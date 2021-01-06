package hu.montlikadani.tablist.bukkit;

public enum Perm {

	FAKEPLAYERS,
	HELP,
	RELOAD,
	RESET,
	SETPREFIX,
	SETPRIORITY,
	SETSUFFIX,
	REMOVEGROUP,
	TABNAME,
	TOGGLE,
	TOGGLEALL("toggle.all"),
	//SEESPECTATOR("seespectators"),
	;

	private String perm;

	Perm() {
		this("");
	}

	Perm(String perm) {
		this.perm = "tablist." + (perm.trim().isEmpty() ? toString().toLowerCase() : perm);
	}

	public String getPerm() {
		return perm;
	}
}
