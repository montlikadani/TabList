package hu.montlikadani.tablist.bukkit;

import hu.montlikadani.tablist.bukkit.config.ConfigValues;
import hu.montlikadani.tablist.bukkit.tablist.groups.ITabScoreboard;
import hu.montlikadani.tablist.bukkit.tablist.groups.ReflectionHandled;
import hu.montlikadani.tablist.bukkit.utils.PluginUtils;

import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

public class TabListPlayer implements Comparable<TabListPlayer> {

	private TabList plugin;

	private final Player player;

	private TeamHandler group, globalGroup;

	private boolean afk;

	private String customPrefix, customSuffix, playerVaultGroup;

	private int customPriority = Integer.MIN_VALUE;

	private ITabScoreboard tabTeam;

	TabListPlayer(TabList plugin, Player player) {
		this.plugin = plugin;
		this.player = player;

		tabTeam = new ReflectionHandled(this);
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
		String name = Integer.toString(100000 + getPriority()) + (group == null ? player.getName() : group.getTeam());
		if (name.length() > 16) {
			name = name.substring(0, 16);
		}

		return name;
	}

	public void removeGroup() {
		group = null;
		globalGroup = null;
	}

	public Player getPlayer() {
		return player;
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

		List<TeamHandler> groupsList = plugin.getGroups().getGroupsList();
		List<TeamHandler> playerNameGroups = groupsList.stream()
				.filter(group -> player.getName().equalsIgnoreCase(group.getTeam())).collect(Collectors.toList());
		if (!playerNameGroups.isEmpty()) {
			TeamHandler team = playerNameGroups.get(0);
			if (!team.isGlobal()) {
				for (TeamHandler t : groupsList) {
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
		} else {
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

			List<TeamHandler> playerPrimaryVaultGroups;
			if (playerVaultGroup != null && ConfigValues.isPreferPrimaryVaultGroup()
					&& (!(playerPrimaryVaultGroups = groupsList.stream()
							.filter(group -> playerVaultGroup.equalsIgnoreCase(group.getTeam()))
							.collect(Collectors.toList())).isEmpty()
							|| !(playerPrimaryVaultGroups = groupsList.stream()
									.filter(group -> StringUtils.containsIgnoreCase(group.getTeam(), playerVaultGroup))
									.collect(Collectors.toList())).isEmpty())) {
				TeamHandler team = playerPrimaryVaultGroups.get(0);
				if (!team.isGlobal()) {
					for (TeamHandler t : groupsList) {
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
			} else {
				for (final TeamHandler team : groupsList) {
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
			}
		}

		return update;
	}

	private boolean isPlayerCanSeeGroup() {
		String path = "change-prefix-suffix-in-tablist.";

		if (ConfigValues.isUseDisabledWorldsAsWhiteList()) {
			if (!plugin.getConfig().getStringList(path + "disabled-worlds.list")
					.contains(player.getWorld().getName())) {
				return false;
			}
		} else {
			if (plugin.getConfig().getStringList(path + "disabled-worlds.list").contains(player.getWorld().getName())) {
				return false;
			}
		}

		if (PluginUtils.isInGame(player)) {
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

		if (globalGroup != null) {
			prefix = globalGroup.getPrefix() + prefix;
		}

		if (ConfigValues.isAfkStatusEnabled() && !ConfigValues.isAfkStatusShowInRightLeftSide()) {
			prefix = colorMsg(plugin.getConfig()
					.get("placeholder-format.afk-status.format-" + (PluginUtils.isAfk(player) ? "yes" : "no"), ""))
					+ prefix;
		}

		return prefix.isEmpty() ? prefix : plugin.getPlaceholders().replaceVariables(player, plugin.makeAnim(prefix));
	}

	public String getSuffix() {
		String suffix = customSuffix == null ? group == null ? "" : group.getSuffix() : customSuffix;

		if (globalGroup != null) {
			suffix += globalGroup.getSuffix();
		}

		if (ConfigValues.isAfkStatusEnabled() && ConfigValues.isAfkStatusShowInRightLeftSide()) {
			suffix += colorMsg(plugin.getConfig()
					.get("placeholder-format.afk-status.format-" + (PluginUtils.isAfk(player) ? "yes" : "no"), ""));
		}

		return suffix.isEmpty() ? suffix : plugin.getPlaceholders().replaceVariables(player, plugin.makeAnim(suffix));
	}

	public String getCustomTabName() {
		String tabName = (group == null || group.getTabName().isEmpty()) ? player.getName()
				: plugin.getPlaceholders().replaceVariables(player, plugin.makeAnim(group.getTabName()));
		return getPrefix() + tabName + getSuffix();
	}

	@Override
	public int compareTo(TabListPlayer tlp) {
		if (ConfigValues.isAfkSortLast()) {
			int comp = Boolean.compare(isAfk(), tlp.isAfk());
			if (comp != 0) return comp;
		}

		int ownPriority = this.getPriority();
		int tlpPriority = tlp.getPriority();

		if (ownPriority == tlpPriority)
			return getCustomTabName().compareTo(tlp.getCustomTabName());

		return ownPriority - tlpPriority;
	}
}
