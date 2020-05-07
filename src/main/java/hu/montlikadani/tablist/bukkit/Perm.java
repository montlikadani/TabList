package hu.montlikadani.tablist.bukkit;

public enum Perm {
	ADDFAKEPLAYER("tablist.fakeplayers.add"),
	FAKEPLAYERS("tablist.fakeplayers"),
	GET("tablist.get"),
	GETO("tablist.get.other"),
	HELP("tablist.help"),
	LISTFAKEPLAYERS("tablist.fakeplayers.list"),
	RELOAD("tablist.reload"),
	REMOVEFAKEPLAYER("tablist.fakeplayers.remove"),
	RESET("tablist.reset"),
	RESETOTHERTAB("tablist.reset.other"),
	SETPREFIX("tablist.setprefix"),
	SETPRIORITY("tablist.setpriority"),
	SETSUFFIX("tablist.setsuffix"),
	REMOVEGROUP("tablist.removegroup"),
	TABNAME("tablist.tabname"),
	TABNAMEOTHER("tablist.tabname.other"),
	TOGGLE("tablist.toggle"),
	TOGGLEALL("tablist.toggle.all");

	private String perm;

	Perm(String perm) {
		this.perm = perm;
	}

	public String getPerm() {
		return perm;
	}
}
