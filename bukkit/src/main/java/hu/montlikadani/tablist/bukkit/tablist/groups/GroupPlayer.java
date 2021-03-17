package hu.montlikadani.tablist.bukkit.tablist.groups;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.tablist.groups.impl.ITabScoreboard;
import hu.montlikadani.tablist.bukkit.tablist.groups.impl.ReflectionHandled;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.PluginUtils;

public final class GroupPlayer implements Comparable<GroupPlayer> {

	private final TabListUser tabListUser;
	private final ITabScoreboard tabTeam;
	private final TabList plugin;

	private TeamHandler group, globalGroup;

	private String customPrefix, customSuffix, playerVaultGroup;

	private boolean afk;
	private int customPriority = Integer.MIN_VALUE;

	public GroupPlayer(TabList plugin, TabListUser tabListUser) {
		this.plugin = plugin;
		this.tabListUser = tabListUser;

		tabTeam = new ReflectionHandled(tabListUser);
	}

	public ITabScoreboard getTabTeam() {
		return tabTeam;
	}

	public void setGroup(TeamHandler group) {
		this.group = group;
	}

	public TeamHandler getGroup() {
		return group;
	}

	public String getFullGroupTeamName() {
		String name = Integer.toString(100000 + getPriority())
				+ (group == null ? getUser().getPlayer().getName() : group.getTeam());

		if (name.length() > 16) {
			name = name.substring(0, 16);
		}

		return name;
	}

	public void removeGroup() {
		group = null;
		globalGroup = null;
	}

	public TabListUser getUser() {
		return tabListUser;
	}

	public boolean isAfk() {
		return afk;
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

	public int getPriority() {
		return customPriority == Integer.MIN_VALUE ? group == null ? Integer.MAX_VALUE : group.getPriority()
				: customPriority;
	}

	public boolean update() {
		boolean update = false;
		Player player = getUser().getPlayer();

		if (!isPlayerCanSeeGroup() || ConfigValues.isAfkStatusEnabled() && PluginUtils.isAfk(player)
				&& !ConfigValues.isAfkStatusShowPlayerGroup()) {
			if (group != null || globalGroup != null) {
				removeGroup();
				update = true;
			}

			return update;
		}

		boolean afk = PluginUtils.isAfk(player);
		if (this.afk != afk) {
			this.afk = afk;
			update = true;
		}

		for (TeamHandler team : plugin.getGroups().getGroupsList()) {
			if (player.getName().equalsIgnoreCase(team.getTeam())) {
				if (!team.isGlobal()) {
					for (TeamHandler t : plugin.getGroups().getGroupsList()) {
						if (t.isGlobal() && globalGroup != t) {
							globalGroup = t;
							break;
						}
					}
				}

				if (group != team) {
					update = true;
					group = team;
				}

				return update;
			}
		}

		if (plugin.hasVault()) {
			boolean found = false;
			if (playerVaultGroup != null) {
				for (String g : plugin.getVaultPerm().getPlayerGroups(player)) {
					if (playerVaultGroup.equalsIgnoreCase(g)) {
						found = true;
						break;
					}
				}
			}

			// Avoiding verbose spam
			if (!found) {
				playerVaultGroup = plugin.getVaultPerm().getPrimaryGroup(player);
			}
		}

		for (TeamHandler team : plugin.getGroups().getGroupsList()) {
			if (playerVaultGroup != null && ConfigValues.isPreferPrimaryVaultGroup()
					&& (playerVaultGroup.equalsIgnoreCase(team.getTeam())
							|| StringUtils.containsIgnoreCase(team.getTeam(), playerVaultGroup))) {
				if (!team.isGlobal()) {
					for (TeamHandler t : plugin.getGroups().getGroupsList()) {
						if (t.isGlobal() && globalGroup != t) {
							globalGroup = t;
							break;
						}
					}
				}

				if (group != team) {
					update = true;
					group = team;
				}

				return update;
			}

			String name = team.getTeam();
			String perm = team.getPermission();

			if (team.isGlobal() && globalGroup != team) {
				globalGroup = team;
				continue;
			}

			if (PluginUtils.hasPermission(player, perm)) {
				if (group != team) {
					update = true;
					group = team;
				}

				break;
			}

			if (perm.isEmpty() && plugin.hasVault()) {
				for (String groups : plugin.getVaultPerm().getPlayerGroups(player)) {
					if (groups.equalsIgnoreCase(name)) {
						if (group != team) {
							update = true;
							group = team;
						}

						break;
					}
				}
			}
		}

		return update;
	}

	private boolean isPlayerCanSeeGroup() {
		Player player = getUser().getPlayer();

		if (((ConfigValues.isUseDisabledWorldsAsWhiteList()
				&& !ConfigValues.getGroupsDisabledWorlds().contains(player.getWorld().getName()))
				|| (!ConfigValues.isUseDisabledWorldsAsWhiteList()
						&& ConfigValues.getGroupsDisabledWorlds().contains(player.getWorld().getName())))
				|| PluginUtils.isInGame(player)) {
			return false;
		}

		if ((ConfigValues.isHideGroupInVanish() && PluginUtils.isVanished(player))
				|| (ConfigValues.isHideGroupWhenAfk() && PluginUtils.isAfk(player))) {
			tabTeam.unregisterTeam(getFullGroupTeamName());
			removeGroup();
			return false;
		}

		return true;
	}

	public String getPrefix() {
		String prefix = customPrefix == null ? group == null ? "" : group.getPrefix() : customPrefix;

		if ((!prefix.isEmpty() && ConfigValues.isAssignGlobalGroup() && globalGroup != null)
				|| (prefix.isEmpty() && globalGroup != null)) {
			prefix = globalGroup.getPrefix() + prefix;
		}

		Player player = getUser().getPlayer();

		if (ConfigValues.isAfkStatusEnabled() && !ConfigValues.isAfkStatusShowInRightLeftSide()) {
			prefix = colorMsg(plugin.getConfig()
					.get("placeholder-format.afk-status.format-" + (PluginUtils.isAfk(player) ? "yes" : "no"), ""))
					+ prefix;
		}

		if (prefix.isEmpty()) {
			return prefix;
		}

		prefix = plugin.getPlaceholders().replaceVariables(player, plugin.makeAnim(prefix));

		// Replace other plugin's bul...s with only #
		if (prefix.contains("&#")) {
			prefix = StringUtils.replace(prefix, "&#", "#");
		}

		return prefix;
	}

	public String getSuffix() {
		String suffix = customSuffix == null ? group == null ? "" : group.getSuffix() : customSuffix;

		if ((!suffix.isEmpty() && ConfigValues.isAssignGlobalGroup() && globalGroup != null)
				|| (suffix.isEmpty() && globalGroup != null)) {
			suffix += globalGroup.getSuffix();
		}

		Player player = getUser().getPlayer();

		if (ConfigValues.isAfkStatusEnabled() && ConfigValues.isAfkStatusShowInRightLeftSide()) {
			suffix += colorMsg(plugin.getConfig()
					.get("placeholder-format.afk-status.format-" + (PluginUtils.isAfk(player) ? "yes" : "no"), ""));
		}

		if (suffix.isEmpty()) {
			return suffix;
		}

		suffix = plugin.getPlaceholders().replaceVariables(player, plugin.makeAnim(suffix));

		if (suffix.contains("&#")) {
			suffix = StringUtils.replace(suffix, "&#", "#");
		}

		return suffix;
	}

	public String getCustomTabName() {
		Player player = getUser().getPlayer();
		String tabName = player.getName();

		if (ConfigValues.isAssignGlobalGroup() && globalGroup != null && !globalGroup.getTabName().isEmpty()) {
			tabName = plugin.getPlaceholders().replaceVariables(player, plugin.makeAnim(globalGroup.getTabName()));
		} else if (group != null && !group.getTabName().isEmpty()) {
			tabName = plugin.getPlaceholders().replaceVariables(player, plugin.makeAnim(group.getTabName()));
		}

		if (tabName.contains("&#")) {
			tabName = StringUtils.replace(tabName, "&#", "#");
		}

		return getPrefix() + tabName + getSuffix();
	}

	@Override
	public int compareTo(GroupPlayer tlp) {
		if (ConfigValues.isAfkSortLast()) {
			int comp = Boolean.compare(isAfk(), tlp.isAfk());
			if (comp != 0) {
				return comp;
			}
		}

		int ownPriority = getPriority();
		int tlpPriority = tlp.getPriority();

		if (ownPriority == tlpPriority)
			return getCustomTabName().compareTo(tlp.getCustomTabName());

		return ownPriority - tlpPriority;
	}
}
