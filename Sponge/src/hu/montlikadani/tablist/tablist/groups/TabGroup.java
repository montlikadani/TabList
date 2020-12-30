package hu.montlikadani.tablist.tablist.groups;

import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;

import hu.montlikadani.tablist.ConfigValues;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.player.ITabPlayer;
import net.kyori.adventure.text.Component;

public class TabGroup implements Cloneable {

	private String groupName, prefix, suffix, permission;

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

	public void setTeam(final UUID playerUUID, int priority) {
		String teamName = Integer.toString(100000 + priority) + groupName;
		if (teamName.length() > 16) {
			teamName = teamName.substring(0, 16);
		}

		final String name = teamName;

		TabList.get().getTabPlayer(playerUUID).ifPresent(tabPlayer -> tabPlayer.asServerPlayer().ifPresent(player -> {
			final String pref = TabList.get().makeAnim(prefix), suf = TabList.get().makeAnim(suffix);
			final Scoreboard b = getScoreboard(tabPlayer);

			Team team = b.getTeam(name).orElse(Team.builder().name(name).build());

			if (!b.getTeam(name).isPresent()) {
				b.registerTeam(team);
			}

			final Component resultName = TabList.get().getVariables().replaceVariables(player,
					pref + player.getName() + suf);

			Sponge.getServer().getOnlinePlayers()
					.forEach(all -> all.getTabList().getEntry(player.getUniqueId()).ifPresent(te -> {
						te.setDisplayName(resultName);
						all.setScoreboard(b);
					}));
		}));
	}

	public void removeTeam(final TabUser tabUser) {
		getScoreboard(tabUser).getTeam(getFullGroupName()).ifPresent(t -> tabUser.asServerPlayer().ifPresent(sp -> {
			t.removeMember(sp.getTeamRepresentation());
			t.getScoreboard().ifPresent(sb -> sp.setScoreboard(sb));
		}));
	}

	public Scoreboard getScoreboard(final ITabPlayer tabPlayer) {
		return ConfigValues.isUseOwnScoreboard() && tabPlayer.asServerPlayer().isPresent()
				? tabPlayer.asServerPlayer().get().getScoreboard()
				: TabList.board;
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
