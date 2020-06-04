package hu.montlikadani.tablist.sponge.tablist.groups;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.text.Text;

import hu.montlikadani.tablist.sponge.ConfigValues;
import hu.montlikadani.tablist.sponge.TabList;

public class TabGroup implements Cloneable {

	private String groupName;
	private String prefix;
	private String suffix;
	private String permission;

	private int priority;

	public TabGroup(String groupName, String prefix, String suffix, String permission) {
		this(groupName, prefix, suffix, permission, 0);
	}

	public TabGroup(String groupName, String prefix, String suffix, String permission, int priority) {
		this.groupName = groupName;
		this.prefix = prefix;
		this.suffix = suffix;
		this.permission = permission;
		this.priority = priority;
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
		return priority;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getFullGroupName() {
		return priority + groupName;
	}

	public void setTeam(final Player player) {
		final String pref = TabList.get().makeAnim(prefix);
		final String suf = TabList.get().makeAnim(suffix);
		final String teamName = getFullGroupName();
		final Scoreboard b = getScoreboard(player);

		Team team = b.getTeam(teamName).orElse(Team.builder().name(teamName).build());

		if (!b.getTeam(teamName).isPresent()) {
			b.registerTeam(team);
		}

		if (!team.getMembers().contains(player.getTeamRepresentation())) {
			team.addMember(player.getTeamRepresentation());
		}

		final Text name = TabList.get().getVariables().replaceVariables(player, pref + player.getName() + suf);

		Sponge.getServer().getOnlinePlayers().forEach(all -> {
			all.getTabList().getEntry(player.getUniqueId()).ifPresent(te -> {
				te.setDisplayName(name);
				setScoreboard(all);
			});
		});
	}

	public void removeTeam(final Player player) {
		getScoreboard(player).getTeam(getFullGroupName()).ifPresent(t -> {
			t.removeMember(player.getTeamRepresentation());
			setScoreboard(player);
		});
	}

	public void setScoreboard(Player player) {
		player.setScoreboard(getScoreboard(player));
	}

	public Scoreboard getScoreboard(Player player) {
		return ConfigValues.isUseOwnScoreboard() ? player.getScoreboard() : TabList.BOARD;
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
