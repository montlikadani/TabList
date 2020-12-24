package hu.montlikadani.tablist.bukkit;

public enum Perm {

	ADDFAKEPLAYER("fakeplayers.add"),
	SETSKINFAKEPLAYER("fakeplayers.setskin"),
	SETPINGFAKEPLAYER("fakeplayers.setping"),
	FAKEPLAYERS,
	GET,
	GETO("get.other"),
	HELP,
	LISTFAKEPLAYERS("fakeplayers.list"),
	RELOAD,
	REMOVEFAKEPLAYER("fakeplayers.remove"),
	RESET,
	RESETOTHERTAB("reset.other"),
	SETPREFIX,
	SETPRIORITY,
	SETSUFFIX,
	REMOVEGROUP,
	TABNAME,
	TABNAMEOTHER("tabname.other"),
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
