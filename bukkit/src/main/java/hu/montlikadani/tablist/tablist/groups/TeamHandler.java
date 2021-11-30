package hu.montlikadani.tablist.tablist.groups;

public class TeamHandler {

	private String team, prefix, suffix, tabName = "", permission = "";

	private boolean global = false;

	private int priority = 0;

	public TeamHandler() {
	}

	public TeamHandler(String team, String prefix, String suffix) {
		this.team = team;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	public TeamHandler(String team, String prefix, String suffix, String permission, int priority) {
		this(team, prefix, suffix);

		this.permission = permission;
		this.priority = priority;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public void setTabName(String tabName) {
		if (tabName != null) {
			this.tabName = tabName;
		}
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	public boolean isGlobal() {
		return global;
	}

	public String getTeam() {
		return team;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getSuffix() {
		return suffix;
	}

	public String getTabName() {
		return tabName;
	}

	public String getPermission() {
		return permission;
	}

	public int getPriority() {
		return priority;
	}
}
