package hu.montlikadani.tablist.bukkit;

import com.earth2me.essentials.Essentials;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

public class TabListPlayer implements Comparable<TabListPlayer> {

	private TabList plugin;

	private final Player player;

	private TeamHandler group;

	private String nick;
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
		return;
	}

	public Player getPlayer() {
		return player;
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

	public int getPriority() {
		return customPriority == Integer.MIN_VALUE ? group == null ? Integer.MAX_VALUE : group.getPriority()
				: customPriority;
	}

	public boolean update() {
		String phPath = "placeholder-format.afk-status.";
		if (!isPlayerCanSeeGroup() || plugin.getC().getBoolean(phPath + "enable") && plugin.isAfk(player, false)
				&& !plugin.getC().getBoolean(phPath + "show-player-group")) {
			if (group != null) {
				group = null;
			}

			return true;
		}

		boolean update = false;

		for (final TeamHandler team : plugin.getGroups().getGroupsList()) {
			String name = team.getTeam();

			if (name.equalsIgnoreCase(player.getName())) {
				if (group != team) {
					update = true;
					group = team;
				}
			}

			String perm = team.getPermission();

			if (!perm.isEmpty() && ((plugin.isPluginEnabled("PermissionsEx")
					&& PermissionsEx.getPermissionManager().has(player, perm))
					|| (player.isPermissionSet(perm) && player.hasPermission(perm)))) {
				if (group != team) {
					update = true;
					group = team;
				}
			}

			if (perm.isEmpty() && plugin.isPluginEnabled("Vault")) {
				for (String groups : plugin.getVaultPerm().getPlayerGroups(player)) {
					if (groups.equalsIgnoreCase(name)) {
						if (group != team) {
							update = true;
							group = team;
						}
					}
				}
			}
		}

		if (plugin.isPluginEnabled("Essentials")
				&& plugin.getC().getBoolean("change-prefix-suffix-in-tablist.use-essentials-nickname")) {
			String nick = JavaPlugin.getPlugin(Essentials.class).getUser(player).getNickname();
			if (nick == null && this.nick != null || nick != null && !nick.equals(this.nick)) {
				this.nick = nick;
				update = true;
			}
		}

		return update;
	}

	private boolean isPlayerCanSeeGroup() {
		String path = "change-prefix-suffix-in-tablist.";
		Player p = this.player;

		if (plugin.getC().getBoolean(path + "disabled-worlds.use-as-whitelist", false)) {
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

		if (plugin.getC().getBoolean(path + "hide-group-when-player-vanished") && plugin.isVanished(p, false)) {
			plugin.getGroups().removePlayerGroup(p);
			return false;
		}

		if (plugin.getC().getBoolean(path + "hide-group-when-player-afk") && plugin.isAfk(p, false)) {
			plugin.getGroups().removePlayerGroup(p);
			return false;
		}

		return true;
	}

	public String getPrefix() {
		String prefix = plugin.getPlaceholders().replaceVariables(player,
				plugin.makeAnim(customPrefix == null ? group == null ? "" : group.getPrefix() : customPrefix));
		String phPath = "placeholder-format.afk-status.";

		final boolean rightLeft = plugin.getC().getBoolean(phPath + "show-in-right-or-left-side");

		if (plugin.getC().getBoolean(phPath + "enable") && !rightLeft) {
			prefix = colorMsg(
					plugin.getC().getString(phPath + "format-" + (plugin.isAfk(player, false) ? "yes" : "no"), ""))
					+ prefix;
		}

		return prefix;
	}

	public String getSuffix() {
		String suffix = plugin.getPlaceholders().replaceVariables(player,
				plugin.makeAnim(customSuffix == null ? group == null ? "" : group.getSuffix() : customSuffix));
		String phPath = "placeholder-format.afk-status.";

		final boolean rightLeft = plugin.getC().getBoolean(phPath + "show-in-right-or-left-side");

		if (plugin.getC().getBoolean(phPath + "enable") && rightLeft) {
			suffix = suffix + colorMsg(
					plugin.getC().getString(phPath + "format-" + (plugin.isAfk(player, false) ? "yes" : "no"), ""));
		}

		return suffix;
	}

	public String getPlayerName() {
		return nick == null ? player.getName() : nick;
	}

	@Override
	public int compareTo(TabListPlayer tlp) {
		int ownPriority = this.getPriority();
		int tlpPriority = tlp.getPriority();

		if (ownPriority == tlpPriority)
			return this.getPlayerName().compareTo(tlp.getPlayerName());

		return ownPriority - tlpPriority;
	}
}
