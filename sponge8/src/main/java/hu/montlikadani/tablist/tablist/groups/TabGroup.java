package hu.montlikadani.tablist.tablist.groups;

import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.Util;
import net.kyori.adventure.text.Component;

public class TabGroup implements Cloneable {

	private final String groupName;

	private String prefix, suffix, permission;
	private int priority;

	private final TabList tl;

	public TabGroup(TabList tl, String groupName, String prefix, String suffix, String permission, int priority) {
		this.groupName = groupName;
		this.prefix = prefix;
		this.suffix = suffix;
		this.permission = permission;
		this.priority = priority;
		this.tl = tl;
	}

	public String getGroupName() {
		return groupName;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getSuffix() {
		return suffix;
	}

	public String getPermission() {
		return permission;
	}

	public int getPriority() {
		return 100000 + priority;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix == null ? "" : prefix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix == null ? "" : suffix;
	}

	public void setPermission(String permission) {
		this.permission = permission == null ? "" : permission;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getFullGroupName() {
		String name = Integer.toString(getPriority()) + groupName;

		if (name.length() > 16) {
			name = name.substring(0, 16);
		}

		return name;
	}

	public void setTeam(final TabListUser user, int priority) {
		user.getPlayer().ifPresent(player -> {
			String teamName = Integer.toString(100000 + priority) + groupName;

			if (teamName.length() > 16) {
				teamName = teamName.substring(0, 16);
			}

			final Scoreboard board = Util.GLOBAL_SCORE_BOARD;

			if (!board.team(teamName).isPresent()) {
				board.registerTeam(Team.builder().name(teamName).build());
				player.setScoreboard(board);
			}

			final String pref = tl.makeAnim(prefix), suf = tl.makeAnim(suffix);
			final Component resultName = tl.getVariables().replaceVariables(player,
					tl.getVariables().setSymbols(pref + player.name() + suf));

			tl.getTabUsers().forEach(users -> users.getPlayer()
					.ifPresent(pl -> pl.tabList().entry(user.getUniqueId()).ifPresent(te -> te.setDisplayName(resultName))));
		});
	}

	public void removeTeam(final ServerPlayer player) {
		Util.GLOBAL_SCORE_BOARD.team(getFullGroupName()).ifPresent(Team::unregister);

		// Get a new scoreboard
		player.setScoreboard(Util.getNewScoreboard());
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this || (obj instanceof TabGroup && groupName.equalsIgnoreCase(((TabGroup) obj).getGroupName()));
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}

		return null;
	}
}
