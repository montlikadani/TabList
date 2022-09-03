package hu.montlikadani.tablist.tablist.groups;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.tablist.TabText;
import hu.montlikadani.tablist.tablist.groups.impl.ITabScoreboard;
import hu.montlikadani.tablist.tablist.groups.impl.ReflectionHandled;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.PluginUtils;
import hu.montlikadani.tablist.utils.ServerVersion;

public final class GroupPlayer {

	private transient final TabListUser tabListUser;
	private transient final ITabScoreboard tabTeam;
	private transient final TabList tl;

	private transient TeamHandler group, globalGroup;

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
		return customPriority == Integer.MIN_VALUE ? group == null ? Integer.MAX_VALUE : group.priority : customPriority;
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
			if (playerName.equalsIgnoreCase(team.team)) {

				// Player can have two groups assigned
				// globalGroup + normalGroup
				if (!team.global) {
					for (TeamHandler t : teams) {
						if (t.global && globalGroup != t) {
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

			// Avoiding verbose spam
			if (playerVaultGroup != null) {
				for (String g : tl.getVaultPerm().getPlayerGroups(player)) {
					if (playerVaultGroup.equalsIgnoreCase(g)) {
						found = true;
						break;
					}
				}
			}

			if (!found && ConfigValues.isPreferPrimaryVaultGroup()) {
				playerVaultGroup = tl.getVaultPerm().getPrimaryGroup(player);
			}
		}

		for (TeamHandler team : teams) {

			// Primary group found
			if (playerVaultGroup != null && playerVaultGroup.equalsIgnoreCase(team.team)) {

				// Search for global group and cache if exists to allow assigning multiple
				// prefixes for primary group
				if (!team.global) {
					for (TeamHandler t : teams) {
						if (t.global && globalGroup != t) {
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

			if (team.global && globalGroup != team) {
				globalGroup = team;
				groups.setToSort(true);

				// Users can also display the global group without any other group specified
				//if (teams.size() == 1) {
					update = true;
				//}

				continue;
			}

			if (!team.permission.isEmpty()) {

				// Permission based groups
				if (PluginUtils.hasPermission(player, team.permission)) {
					if (group != team) {
						update = true;
						setGroup(team);
					}

					break;
				}
			} else if (tl.hasVault()) {
				for (String playerGroup : tl.getVaultPerm().getPlayerGroups(player)) {
					if (playerGroup != null && playerGroup.equalsIgnoreCase(team.team)) {

						// Search for global group and cache if exists to allow assigning multiple
						// prefixes for this group
						if (!team.global) {
							for (TeamHandler t : teams) {
								if (t.global && globalGroup != t) {
									globalGroup = t;
									groups.setToSort(true);
									break;
								}
							}
						}

						if (group != team) {
							update = true;
							setGroup(team);
						}

						// Player group found
						return update;
					}
				}
			}
		}

		return update;
	}

	private boolean isPlayerCanSeeGroup(Player player) {
		if (ConfigValues.isAfkStatusEnabled() && !ConfigValues.isAfkStatusShowPlayerGroup() && PluginUtils.isAfk(player)) {
			return false;
		}

		if (ConfigValues.isHideGroupInVanish() && PluginUtils.isVanished(player)) {
			tabTeam.unregisterTeam(this);
			return false;
		}

		boolean containsWorld = ConfigValues.getGroupsDisabledWorlds().indexOf(player.getWorld().getName()) != -1;

		if (((ConfigValues.isUseDisabledWorldsAsWhiteList() && !containsWorld)
				|| (!ConfigValues.isUseDisabledWorldsAsWhiteList() && containsWorld))) {
			return false;
		}

		return true;
	}

	private TabText fullName;

	// TODO Refactor, this method is currently disgusting
	public TabText getTabNameWithPrefixSuffix() {
		String tabName = null;

		// Assigning global group
		if (ConfigValues.isAssignGlobalGroup() && globalGroup != null && !globalGroup.tabName.getPlainText().isEmpty()) {
			tabName = globalGroup.tabName.getPlainText();

			// Assigning both global and normal
			if (group != null && !group.tabName.getPlainText().isEmpty()) {
				tabName = group.tabName.getPlainText() + tabName;
			}
		} else if (group != null && !group.tabName.getPlainText().isEmpty()) {
			tabName = group.tabName.getPlainText();
		}

		Player player = tabListUser.getPlayer();

		if (tabName == null) {
			if (globalGroup != null && !globalGroup.tabName.getPlainText().isEmpty()) {
				tabName = globalGroup.tabName.getPlainText();
			} else {
				tabName = player != null ? player.getName() : "";
			}
		}

		String prefix = customPrefix == null ? group == null ? "" : group.prefix.getPlainText() : customPrefix;
		String suffix = customSuffix == null ? group == null ? "" : group.suffix.getPlainText() : customSuffix;

		// Assign global group prefix/suffix

		if (globalGroup != null) {
			if (ConfigValues.isAssignGlobalGroup()) {
				prefix = globalGroup.prefix.getPlainText() + prefix;
				suffix += globalGroup.suffix.getPlainText();
			} else if (prefix.isEmpty() && suffix.isEmpty()) { // Assign global group if both prefix/suffix is empty
				prefix = globalGroup.prefix.getPlainText();
				suffix = globalGroup.suffix.getPlainText();
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

		String full = tl.getPlaceholders().replaceVariables(player, tl.makeAnim(prefix + tabName + suffix));

		if (ServerVersion.isCurrentLower(ServerVersion.v1_16_R1)) {
			full = org.bukkit.ChatColor.translateAlternateColorCodes('&', full);
		}

		if (fullName == null) {
			fullName = TabText.parseFromText(full);
		} else {
			fullName.updateText(full);
		}

		return fullName;
	}
}
