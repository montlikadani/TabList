package hu.montlikadani.tablist.tablist.groups;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.tablist.TabText;
import hu.montlikadani.tablist.tablist.groups.impl.ReflectionHandled;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.PluginUtils;

public final class GroupPlayer {

	private transient final TabListUser tabListUser;
	private transient final ReflectionHandled tabTeam;
	private transient final TabList tl;

	private transient TeamHandler group, globalGroup;

	private String customPrefix, customSuffix, playerPrimaryGroup;

	private int customPriority = Integer.MIN_VALUE;
	private int safePriority = 0;

	public GroupPlayer(TabList tl, TabListUser tabListUser) {
		this.tl = tl;
		this.tabListUser = tabListUser;

		tabTeam = new ReflectionHandled(tl, this);
	}

	public ReflectionHandled getTabTeam() {
		return tabTeam;
	}

	/**
	 * Sets the main group for this player.
	 *
	 * @param group the {@link TeamHandler} to be set
	 */
	public void setGroup(TeamHandler group) {
		this.group = group;
		tl.getGroups().setToSort(true);
	}

	public TeamHandler getGroup() {
		return group;
	}

	/**
	 * @return the full group name with priority required for creating
	 */
	public String getFullGroupTeamName() {
		// We starts the sorting with "a" character and a number as without this will
		// not work after the 10th player
		return 'a' + (safePriority > 9 ? "" : "0") + safePriority;
	}

	/**
	 * Removes the group cache from this player
	 */
	public void removeGroup() {
		group = globalGroup = null;
		playerPrimaryGroup = null;
	}

	public TabListUser getUser() {
		return tabListUser;
	}

	/**
	 * Sets a custom prefix for this group
	 *
	 * @param customPrefix
	 */
	public void setCustomPrefix(String customPrefix) {
		this.customPrefix = customPrefix;
	}

	/**
	 * Sets a custom suffix for this group
	 *
	 * @param customSuffix
	 */
	public void setCustomSuffix(String customSuffix) {
		this.customSuffix = customSuffix;
	}

	/**
	 * Sets a custom priority for this group
	 *
	 * @param customPriority
	 */
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

	/**
	 * This method finds and caches player primary group once. If player
	 * primary group changed it will be refreshed with the new one. This
	 * prevents luckperms verbose spam as the group will be checked at
	 * every tick when enabled.
	 */
	private void refreshPlayerPrimaryGroup(Player player) {
		if (!tl.hasPermissionService() || !ConfigValues.isPreferPrimaryVaultGroup()) {
			return;
		}

		if (playerPrimaryGroup == null || !tl.getPermissionService().playerInGroup(player, playerPrimaryGroup)) {
			playerPrimaryGroup = tl.getPermissionService().getPrimaryGroup(player);
		}
	}

	public boolean update() {
		Player player = tabListUser.getPlayer();

		if (player == null) {
			return false;
		}

		// Initial inspections to remove group from player, if one of condition is true
		if (!isPlayerCanSeeGroup(player)) {
			if (group != null || globalGroup != null) {
				tl.getGroups().removePlayerGroup(tabListUser);
				tl.getGroups().setToSort(false);
				return true;
			}

			// Player's group is not exist anymore so no need to update
			return false;
		}

		Groups groups = tl.getGroups();
		java.util.List<TeamHandler> teams = groups.getTeams();
		boolean update = false;

		// Search for global group
		for (TeamHandler gl : teams) {
			if (gl.global && globalGroup != gl) {
				globalGroup = gl;
				groups.setToSort(true);
				update = true;
				break;
			}
		}

		String playerName = player.getName();

		// Search for player name
		for (TeamHandler team : teams) {
			if (team.global || !playerName.equalsIgnoreCase(team.team)) {
				continue;
			}

			if (group != team) {
				setGroup(team);
				return true;
			}

			return update;
		}

		refreshPlayerPrimaryGroup(player);

		if (!groups.orderedGroupsByWeight().isEmpty()) {
			teams = groups.orderedGroupsByWeight();
		}

		for (TeamHandler team : teams) {
			if (team.global) {
				continue;
			}

			if (playerPrimaryGroup != null) {
				if (playerPrimaryGroup.equalsIgnoreCase(team.team)) {
					if (group != team) {
						update = true;
						setGroup(team);
					}

					return update;
				}

				continue; // Only check primary
			}

			if (!team.permission.isEmpty()) {
				if (PluginUtils.hasPermission(player, team.permission)) {
					if (group != team) {
						update = true;
						setGroup(team);
					}

					return update;
				}
			} else if (tl.hasPermissionService() && tl.getPermissionService().playerInGroup(player, team.team)) {
				if (group != team) {
					update = true;
					setGroup(team);
				}

				return update;
			}
		}

		// If player has a group which is not in groups.yml, and it has not changed since last check
		if (group != groups.getDefaultAssignedGroup() || (group == null && globalGroup == null)) {
			setGroup(groups.getDefaultAssignedGroup());
			update = true;
		}

		return update;
	}

	private boolean isPlayerCanSeeGroup(Player player) {
		if (ConfigValues.isAfkStatusEnabled() && !ConfigValues.isAfkStatusShowPlayerGroup() && PluginUtils.isAfk(player)) {
			return false;
		}

		if (ConfigValues.isHideGroupInVanish() && PluginUtils.isVanished(player)) {
			tabTeam.unregisterTeam();
			return false;
		}

		boolean containsWorld = ConfigValues.getGroupsDisabledWorlds().indexOf(player.getWorld().getName()) != -1;

		return (!ConfigValues.isUseDisabledWorldsAsWhiteList() || containsWorld) && (ConfigValues.isUseDisabledWorldsAsWhiteList() || !containsWorld);
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

		if (fullName == null) {
			fullName = TabText.parseFromText(full);
		} else {
			fullName.updateText(full);
		}

		return fullName;
	}
}
