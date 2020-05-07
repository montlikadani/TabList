package hu.montlikadani.tablist.bukkit;

import com.earth2me.essentials.Essentials;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

public class TabListPlayer implements Comparable<TabListPlayer>{
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
        return customPriority == Integer.MIN_VALUE ? group == null ? Integer.MAX_VALUE : group.getPriority() : customPriority;
    }

    public boolean update() {
        boolean update = false;
        String phPath = "placeholder-format.afk-status.";
        if (!isPlayerCanSeeGroup() || plugin.getC().getBoolean(phPath + "enable") && plugin.isAfk(this.player, false)
                && !plugin.getC().getBoolean(phPath + "show-player-group")) {
            if (group != null) {
                group = null;
                update = true;
            }
        }
        for (final TeamHandler team : TabList.getInstance().getGroups().getGroupsList()) {
            String name = team.getTeam();

            if (name.equalsIgnoreCase(player.getName())) {
                update = true;
                this.group = team;
                break;
            }

            if (plugin.isPluginEnabled("Vault") && plugin.getVaultPerm().playerInGroup(player, name)) {
                update = true;
                this.group = team;
                break;
            }

            if (!team.getPermission().isEmpty()) {
                if (plugin.isPluginEnabled("PermissionsEx")) {
                    if (PermissionsEx.getPermissionManager().has(this.player, team.getPermission())) {
                        update = true;
                        this.group = team;
                        break;
                    }
                } else if (player.hasPermission(team.getPermission())) {
                    update = true;
                    this.group = team;
                    break;
                }
            }
        }
        if (plugin.isPluginEnabled("Essentials")
                && plugin.getC().getBoolean("change-prefix-suffix-in-tablist.use-essentials-nickname")) {
            String nick = JavaPlugin.getPlugin(Essentials.class).getUser(player).getNickname();
            if (nick == null && this.nick != null || nick != null &&  !nick.equals(this.nick)) {
                update = true;
                this.nick = nick;
            }
        }
        return update;
    }

    private boolean isPlayerCanSeeGroup() {
        Player p = this.player;
        String path = "change-prefix-suffix-in-tablist.";
        if (plugin.getC().getBoolean(path + "disabled-worlds.use-as-whitelist", false)) {
            if (!plugin.getC().getStringList(path + "disabled-worlds.list").contains(p.getWorld().getName())) {
                return false;
            }
        } else {
            if (plugin.getC().getStringList(path + "disabled-worlds.list").contains(p.getWorld().getName())) {
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
        String prefix = plugin.getPlaceholders().replaceVariables(player, plugin.makeAnim(customPrefix == null ? group == null ? "" : group.getPrefix() : customPrefix));
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
        String suffix = plugin.getPlaceholders().replaceVariables(player, plugin.makeAnim(customSuffix == null ? group == null ? "" : group.getSuffix() : customSuffix));
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
        if (ownPriority == tlpPriority) return this.getPlayerName().compareTo(tlp.getPlayerName());
        return ownPriority - tlpPriority;
    }
}
