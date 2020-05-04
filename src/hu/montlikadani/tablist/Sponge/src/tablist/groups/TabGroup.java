package hu.montlikadani.tablist.Sponge.src.tablist.groups;

import java.util.Optional;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
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

	public void setGroup(Player player) {
		if (!player.hasPermission(permission)) {
			return;
		}

		String pref = TabList.get().makeAnim(prefix);
		String suf = TabList.get().makeAnim(suffix);

		Team team = null;
		if (!TabList.BOARD.getTeam(getFullGroupName()).isPresent()) {
			team = Team.builder().name(getFullGroupName()).build();
			TabList.BOARD.registerTeam(team);
		} else {
			team = TabList.BOARD.getTeam(getFullGroupName()).get();
		}

		Text name = TabList.get().getVariables().replaceVariables(player, pref + player.getName() + suf);

		org.spongepowered.api.entity.living.player.tab.TabList tablist = player.getTabList();
		TabListEntry entry = null;
		Optional<TabListEntry> e = tablist.getEntry(player.getUniqueId());
		if (e.isPresent()) {
			entry = e.get();
			entry.setDisplayName(name);
		} else {
			entry = TabListEntry.builder().list(tablist).gameMode(player.gameMode().get()).displayName(name)
					.profile(player.getProfile()).build();
		}

		if (!tablist.getEntries().contains(entry)) {
			tablist.addEntry(entry);
		}

		team.addMember(player.getTeamRepresentation());

		setScoreboard(player);
	}

	public void removeGroup(Player player) {
		Optional<Team> team = TabList.BOARD.getTeam(getFullGroupName());
		if (team.isPresent()) {
			team.get().removeMember(player.getTeamRepresentation());
			setScoreboard(player);
		}
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
