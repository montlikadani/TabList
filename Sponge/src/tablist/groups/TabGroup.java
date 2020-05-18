package hu.montlikadani.tablist.Sponge.src.tablist.groups;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.text.Text;

import hu.montlikadani.tablist.Sponge.src.TabList;

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

	public void setGroup(final Player player) {
		if (!player.getSubjectData().getPermissions(player.getActiveContexts()).containsKey(permission)
				&& !player.hasPermission(permission)) {
			return;
		}

		final String pref = TabList.get().makeAnim(prefix);
		final String suf = TabList.get().makeAnim(suffix);
		final String teamName = getFullGroupName();

		Team team = null;
		if (!TabList.BOARD.getTeam(teamName).isPresent()) {
			team = Team.builder().name(teamName).build();
			TabList.BOARD.registerTeam(team);
		} else {
			team = TabList.BOARD.getTeam(teamName).get();
		}

		team.addMember(player.getTeamRepresentation());

		final Text name = TabList.get().getVariables().replaceVariables(player, pref + player.getName() + suf);

		Sponge.getServer().getOnlinePlayers().forEach(all -> {
			all.getTabList().getEntry(player.getUniqueId()).ifPresent(te -> {
				te.setDisplayName(name);
			});
		});

		setScoreboard(player);
	}

	public void removeGroup(final Player player) {
		TabList.BOARD.getTeam(getFullGroupName()).ifPresent(t -> {
			t.removeMember(player.getTeamRepresentation());
			setScoreboard(player);
		});
	}

	public void setScoreboard(Player player) {
		player.setScoreboard(TabList.BOARD);
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
