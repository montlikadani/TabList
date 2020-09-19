package hu.montlikadani.tablist.bukkit;

public enum Perm {

	ADDFAKEPLAYER("fakeplayers.add"),
	SETSKINFAKEPLAYER("fakeplayers.setskin"),
	FAKEPLAYERS("fakeplayers"),
	GET("get"),
	GETO("get.other"),
	HELP("help"),
	LISTFAKEPLAYERS("fakeplayers.list"),
	RELOAD("reload"),
	REMOVEFAKEPLAYER("fakeplayers.remove"),
	RESET("reset"),
	RESETOTHERTAB("reset.other"),
	SETPREFIX("setprefix"),
	SETPRIORITY("setpriority"),
	SETSUFFIX("setsuffix"),
	REMOVEGROUP("removegroup"),
	TABNAME("tabname"),
	TABNAMEOTHER("tabname.other"),
	TOGGLE("toggle"),
	TOGGLEALL("toggle.all"),
	SEESPECTATOR("seespectators");

	private String perm;

	Perm(String perm) {
		this.perm = "tablist." + perm;
	}

	public String getPerm() {
		return perm;
	}
}
