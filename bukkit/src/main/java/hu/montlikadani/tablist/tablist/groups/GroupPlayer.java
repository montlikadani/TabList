package hu.montlikadani.tablist.tablist.groups;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.tablist.groups.impl.ITabScoreboard;
import hu.montlikadani.tablist.tablist.groups.impl.ReflectionHandled;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.PluginUtils;

public final class GroupPlayer {

	private final TabListUser tabListUser;
	private final ITabScoreboard tabTeam;
	private final TabList tl;

	private TeamHandler group, globalGroup;

	private String customPrefix, customSuffix, playerVaultGroup;

	private int customPriority = Integer.MIN_VALUE;
	private int safePriority = 0;

	public GroupPlayer(TabList tl, TabListUser tabListUser) {
		this.tl = tl;
		this.tabListUser = tabListUser;

		tabTeam = new ReflectionHandled(tl);
	}

	public ITabScoreboard getTabTeam() {
		return tabTeam;
	}

	public void setGroup(TeamHandler group) {
		this.group = group;
		tl.getGroups().setToSort(true);
	}

	public TeamHandler getGroup() {
		return group;
	}

	public String getFullGroupTeamName() {
		// We starts the sorting with "a" character and a number as without this will
		// not work after the 10th player
		return 'a' + (safePriority > 9 ? "" : "0") + safePriority;
	}

	public void removeGroup() {
		group = globalGroup = null;
		playerVaultGroup = null;
	}

	public TabListUser getUser() {
		return tabListUser;
	}

	public void setCustomPrefix(String customPrefix) {
		this.customPrefix = customPrefix;
	}

	public void setCustomSuffix(String customSuffix) {
		this.customSuffix = customSuffix;
	}

	public void setCustomPriority(int customPriority) {
		this.customPriority = customPriority;
	}

	/**
	 * Sets variable safePriority.
	 *
	 * @param safePriority Safe priority value
	 */
	public void setSafePriority(int safePriority) {
		if (safePriority > 98) {
			this.safePriority = 99;
			return;
		}

		if (safePriority < 0) {
			safePriority = 0;
		}

		this.safePriority = safePriority;
	}

	public int getPriority() {
		return customPriority == Integer.MIN_VALUE ? group == null ? Integer.MAX_VALUE : group.getPriority() : customPriority;
	}

	public boolean update() {
		Player player = tabListUser.getPlayer();

		if (player == null) {
			return false;
		}

		// Initial inspections to remove group from player, if one of condition is true
		if (!isPlayerCanSeeGroup(player)) {

			// Make sure the group or global is exist and remove only once
			if (group != null || globalGroup != null) {
				tl.getGroups().removePlayerGroup(tabListUser);
				tl.getGroups().setToSort(false); // Refuse to sort by this player
				return true;
			}

			// Player's group is not exist anymore so no need to update
			return false;
		}

		boolean update = false;
		Groups groups = tl.getGroups();
		String playerName = player.getName();
		java.util.List<TeamHandler> teams = groups.getGroupsList();

		// Search for player' name
		for (TeamHandler team : teams) {
			if (playerName.equalsIgnoreCase(team.getTeam())) {

				// Player can have two groups assigned
				// globalGroup + normalGroup
				if (!team.isGlobal()) {
					for (TeamHandler t : teams) {
						if (t.isGlobal() && globalGroup != t) {
							globalGroup = t;
							groups.setToSort(true);
							break;
						}
					}
				}

				if (group != team) { // Player' group was changed
					update = true;
					setGroup(team);
				}

				return update;
			}
		}

		if (tl.hasVault()) {
			boolean found = false;

			if (playerVaultGroup != null) {
				for (String g : tl.getVaultPerm().getPlayerGroups(player)) {
					if (playerVaultGroup.equalsIgnoreCase(g)) {
						found = true;
						break;
					}
				}
			}

			// Avoiding verbose spam
			if (!found && ConfigValues.isPreferPrimaryVaultGroup()) {
				playerVaultGroup = tl.getVaultPerm().getPrimaryGroup(player);
			}
		}

		for (TeamHandler team : teams) {

			// Primary group found
			if (playerVaultGroup != null && playerVaultGroup.equalsIgnoreCase(team.getTeam())) {

				// Search for global group and cache if exists to allow assigning multiple
				// prefixes for primary group
				if (!team.isGlobal()) {
					for (TeamHandler t : teams) {
						if (t.isGlobal() && globalGroup != t) {
							groups.setToSort(true);
							globalGroup = t;
							break;
						}
					}
				}

				if (group != team) { // Player' group was changed or not set
					update = true;
					setGroup(team);
				}

				return update;
			}

			if (team.isGlobal() && globalGroup != team) {
				globalGroup = team;
				groups.setToSort(true);

				// Users can also display the global group without any other group specified
				if (teams.size() == 1) {
					update = true;
				}

				continue;
			}

			if (!team.getPermission().isEmpty()) {

				// Permission based groups
				if (PluginUtils.hasPermission(player, team.getPermission())) {
					if (group != team) {
						update = true;
						setGroup(team);
					}

					break;
				}
			} else if (tl.hasVault()) {
				for (String playerGroup : tl.getVaultPerm().getPlayerGroups(player)) {
					if (playerGroup != null && playerGroup.equalsIgnoreCase(team.getTeam())) {

						// Search for global group and cache if exists to allow assigning multiple
						// prefixes for this group
						if (!team.isGlobal()) {
							for (TeamHandler t : teams) {
								if (t.isGlobal() && globalGroup != t) {
									groups.setToSort(true);
									globalGroup = t;
									break;
								}
							}
						}

						if (group != team) {
							update = true;
							setGroup(team);
						}

						// Player group found so break loop
						break;
					}
				}

				if (group == team) {
					break; // Proper group been set so break loop
				}
			}
		}

		return update;
	}

	private boolean isPlayerCanSeeGroup(Player player) {
		if (ConfigValues.isAfkStatusEnabled() && !ConfigValues.isAfkStatusShowPlayerGroup() && PluginUtils.isAfk(player)) {
			return false;
		}

		boolean containsWorld = ConfigValues.getGroupsDisabledWorlds().contains(player.getWorld().getName());

		if (((ConfigValues.isUseDisabledWorldsAsWhiteList() && !containsWorld)
				|| (!ConfigValues.isUseDisabledWorldsAsWhiteList() && containsWorld))) {
			return false;
		}

		if (ConfigValues.isHideGroupInVanish() && PluginUtils.isVanished(player)) {
			tabTeam.unregisterTeam(this);
			return false;
		}

		return true;
	}

	public String getTabNameWithPrefixSuffix() {
		Player player = tabListUser.getPlayer();
		String tabName = null;

		// Assigning global group
		if (ConfigValues.isAssignGlobalGroup() && globalGroup != null && !globalGroup.getTabName().isEmpty()) {
			tabName = globalGroup.getTabName();

			// Assigning both global and normal
			if (group != null && !group.getTabName().isEmpty()) {
				tabName = group.getTabName() + tabName;
			}
		} else if (group != null && !group.getTabName().isEmpty()) {
			tabName = group.getTabName();
		}

		if (tabName == null) {
			tabName = player != null ? player.getName() : "";
		}

		String prefix = customPrefix == null ? group == null ? "" : group.getPrefix() : customPrefix;
		String suffix = customSuffix == null ? group == null ? "" : group.getSuffix() : customSuffix;

		// Assign global group prefix/suffix

		if (globalGroup != null) {
			if (ConfigValues.isAssignGlobalGroup()) {
				prefix = globalGroup.getPrefix() + prefix;
				suffix += globalGroup.getSuffix();
			} else if (prefix.isEmpty() && suffix.isEmpty()) { // Assign global group if both prefix/suffix is empty
				prefix = globalGroup.getPrefix();
				suffix = globalGroup.getSuffix();
			}
		}

		// AFK status

		if (player != null && ConfigValues.isAfkStatusEnabled()) {
			if (!ConfigValues.isAfkStatusShowInRightLeftSide()) {
				prefix = (PluginUtils.isAfk(player) ? ConfigValues.getAfkFormatYes() : ConfigValues.getAfkFormatNo()) + prefix;
			} else {
				suffix += PluginUtils.isAfk(player) ? ConfigValues.getAfkFormatYes() : ConfigValues.getAfkFormatNo();
			}
		}

		return tl.getPlaceholders().replaceVariables(player, tl.makeAnim(prefix + tabName + suffix));
	}
}
