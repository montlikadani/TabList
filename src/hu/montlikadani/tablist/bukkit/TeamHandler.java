package hu.montlikadani.tablist.bukkit;

import org.bukkit.entity.Player;

public class TeamHandler {

	private String team;
	private String prefix;
	private String suffix;
	private int priority;
	@Deprecated private Player player;

	public TeamHandler(String team, String prefix, String suffix) {
		this(team, prefix, suffix, 0);
	}

	@Deprecated
	public TeamHandler(String team, String prefix, String suffix, Player player) {
		this(team, prefix, suffix, 0, player);
	}

	public TeamHandler(String team, String prefix, String suffix, int priority) {
		this.team = team;
		this.prefix = prefix;
		this.suffix = suffix;
		this.priority = priority;
	}

	@Deprecated
	public TeamHandler(String team, String prefix, String suffix, int priority, Player player) {
		this.team = team;
		this.prefix = prefix;
		this.suffix = suffix;
		this.priority = priority;
		this.player = player;
	}

	/**
	 * @deprecated Not used anymore
	 * @param player Player
	 */
	@Deprecated
	public void setPlayer(Player player) {
		this.player = player;
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

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getTeam() {
		return team;
	}

	public String getFullTeamName() {
		return priority + team;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getSuffix() {
		return suffix;
	}

	public int getPriority() {
		return priority;
	}

	/**
	 * @deprecated Not used anymore
	 * @return {@link Player}
	 */
	@Deprecated
	public Player getPlayer() {
		return player;
	}
}
