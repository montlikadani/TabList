package hu.montlikadani.tablist.bukkit;

import com.earth2me.essentials.Essentials;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.util.List;
import java.util.stream.Collectors;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

public class TabListPlayer implements Comparable<TabListPlayer> {

	private TabList plugin;

	private final Player player;

	private TeamHandler group;

	private boolean afk;

	private String nick;
	private String tabName;
	private String customPrefix;
	private String customSuffix;

	private int customPriority = Integer.MIN_VALUE;

	TabListPlayer(TabList plugin, Player player) {
		this.plugin = plugin;
		this.player = player;
	}

	public void setGroup(TeamHandler group) {
		this.group = group;
	}

	public TeamHandler getGroup() {
		return group;
	}

	public void removeGroup() {
		this.group = null;
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

	public void setNick(String nick) {
		this.nick = nick;
	}

	public void setTabName(String tabName) {
		this.tabName = tabName;
	}

	public int getPriority() {
		return customPriority == Integer.MIN_VALUE ? group == null ? Integer.MAX_VALUE : group.getPriority()
				: customPriority;
	}

	public boolean update() {
		boolean update = false;

		if (!isPlayerCanSeeGroup() || ConfigValues.isAfkStatusEnabled() && plugin.isAfk(player, false)
				&& !ConfigValues.isAfkStatusShowPlayerGroup()) {
			if (group != null) {
				group = null;
				update = true;
			}

			return update;
		}

		boolean afk = plugin.isAfk(player, false);
		if (this.afk != afk) {
			this.afk = afk;
			update = true;
		}

		List<TeamHandler> groupsList = plugin.getGroups().getGroupsList();
		List<TeamHandler> playerNameGroups = groupsList.parallelStream()
				.filter((group) -> group.getTeam().equals(player.getName())).collect(Collectors.toList());
		if (playerNameGroups.size() > 0) { // can there be more than 1? probably doesn't matter
			TeamHandler team = playerNameGroups.get(0);
			if (group != team) {
				update = true;
				group = team;
			}
		} else {
			List<TeamHandler> playerPrimaryVaultGroups;
			if (plugin.isPluginEnabled("Vault") && ConfigValues.isPreferPrimaryVaultGroup()
					&& (playerPrimaryVaultGroups = groupsList.parallelStream()
							.filter((group) -> group.getTeam().equals(plugin.getVaultPerm().getPrimaryGroup(player)))
							.collect(Collectors.toList())).size() > 0) { // can there be more than 1? probably doesn't matter
				TeamHandler team = playerPrimaryVaultGroups.get(0);
				if (group != team) {
					update = true;
					group = team;
				}
			} else {
				for (final TeamHandler team : plugin.getGroups().getGroupsList()) {
					String name = team.getTeam();
					String perm = team.getPermission();

					if (!perm.isEmpty() && ((plugin.isPluginEnabled("PermissionsEx")
							&& PermissionsEx.getPermissionManager().has(player, perm))
							|| (player.isPermissionSet(perm) && player.hasPermission(perm)))) {
						if (group != team) {
							update = true;
							group = team;
						}

						break;
					}

					if (perm.isEmpty() && plugin.isPluginEnabled("Vault")) {
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

		if (plugin.isPluginEnabled("Essentials") && ConfigValues.isUseEssentialsNickName()) {
			String nick = JavaPlugin.getPlugin(Essentials.class).getUser(player).getNickname();
			if (nick == null && this.nick != null || nick != null && !nick.equals(this.nick)) {
				this.nick = nick;
				update = true;
			}
		}

		if (ConfigValues.isTabNameEnabled() && ConfigValues.isUseTabName()) {
			String tabName = plugin.getTabNameHandler().getTabName(player);
			if (tabName.isEmpty() && this.tabName != null || !tabName.isEmpty() && !tabName.equals(this.tabName)) {
				this.tabName = tabName;
				update = true;
			}
		}

		return update;
	}

	private boolean isPlayerCanSeeGroup() {
		String path = "change-prefix-suffix-in-tablist.";
		Player p = this.player;

		if (ConfigValues.isUseDisabledWorldsAsWhiteList()) {
			if (!plugin.getC().getStringList(path + "disabled-worlds.list").contains(p.getWorld().getName())) {
				return false;
			}
		} else {
			if (plugin.getC().getStringList(path + "disabled-worlds.list").contains(p.getWorld().getName())) {
				nick = null;
				return false;
			}
		}

		if (plugin.isHookPreventTask(p)) {
			return false;
		}

		if (ConfigValues.isHideGroupInVanish() && plugin.isVanished(p, false)) {
			plugin.getGroups().removePlayerGroup(p);
			return false;
		}

		if (ConfigValues.isHideGroupWhenAfk() && plugin.isAfk(p, false)) {
			plugin.getGroups().removePlayerGroup(p);
			return false;
		}

		return true;
	}

	public String getPrefix() {
		String prefix = plugin.getPlaceholders().replaceVariables(player,
				plugin.makeAnim(customPrefix == null ? group == null ? "" : group.getPrefix() : customPrefix));

		if (ConfigValues.isAfkStatusEnabled() && !ConfigValues.isAfkStatusShowInRightLeftSide()) {
			prefix = colorMsg(plugin.getC().getString(
					"placeholder-format.afk-status.format-" + (plugin.isAfk(player, false) ? "yes" : "no"), ""))
					+ prefix;
		}

		return prefix;
	}

	public String getSuffix() {
		String suffix = plugin.getPlaceholders().replaceVariables(player,
				plugin.makeAnim(customSuffix == null ? group == null ? "" : group.getSuffix() : customSuffix));

		if (ConfigValues.isAfkStatusEnabled() && ConfigValues.isAfkStatusShowInRightLeftSide()) {
			suffix = suffix + colorMsg(plugin.getC().getString(
					"placeholder-format.afk-status.format-" + (plugin.isAfk(player, false) ? "yes" : "no"), ""));
		}

		return suffix;
	}

	public String getPlayerName() {
		return nick == null ? tabName == null ? player.getName() : tabName : nick;
	}

	@Override
	public int compareTo(TabListPlayer tlp) {
		if (ConfigValues.isAfkSortLast()) {
			int comp = Boolean.compare(this.isAfk(), tlp.isAfk());
			if (comp != 0) return comp;
		}

		int ownPriority = this.getPriority();
		int tlpPriority = tlp.getPriority();

		if (ownPriority == tlpPriority)
			return this.getPlayerName().compareTo(tlp.getPlayerName());

		return ownPriority - tlpPriority;
	}
}
