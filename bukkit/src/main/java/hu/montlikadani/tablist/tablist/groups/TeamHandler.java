package hu.montlikadani.tablist.tablist.groups;

import hu.montlikadani.tablist.tablist.TabText;

public class TeamHandler {

	public String team = "", permission = "";
	public transient TabText prefix, suffix, tabName = TabText.EMPTY;

	public boolean global = false;

	public int priority = 0;
	private int afkSortPriority = -1;

	public TeamHandler() {
	}

	public TeamHandler(String team, TabText prefix, TabText suffix) {
		this.team = team == null ? "" : team;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	public TeamHandler(String team, TabText prefix, TabText suffix, String permission, int priority) {
		this(team, prefix, suffix);

		this.permission = permission == null ? "" : permission;
		this.priority = priority;
	}

	public void setAfkSortPriority(int afkSortPriority) {
		if (afkSortPriority >= 0) {
			this.afkSortPriority = afkSortPriority;
		}
	}

	public int getAfkSortPriority() {
		return afkSortPriority;
	}
}
