package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.tablist.TabManager;
import hu.montlikadani.tablist.bukkit.tablist.TabTitle;

public class toggle implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.TOGGLE.getPerm())) {
			sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.TOGGLE.getPerm()));
			return false;
		}

		if (args.length == 1) {
			if (!(sender instanceof Player)) {
				sendMsg(sender, plugin.getMsg("toggle.console-usage", "%command%", label));
				return false;
			}

			Player p = (Player) sender;
			if (!plugin.getTabManager().isPlayerInTab(p)) {
				return true;
			}

			UUID uuid = p.getUniqueId();
			boolean changed = TabManager.TABENABLED.containsKey(uuid) ? !TabManager.TABENABLED.get(uuid) : true;
			if (changed) {
				TabManager.TABENABLED.put(uuid, true);
				TabTitle.sendTabTitle(p, "", "");
				sendMsg(p, plugin.getMsg("toggle.disabled"));
			} else {
				TabManager.TABENABLED.remove(uuid);
				plugin.getTabManager().getPlayerTab(p).get().updateTab();
				sendMsg(p, plugin.getMsg("toggle.enabled"));
			}

			return true;
		}

		if (args.length == 2) {
			if (args[1].equalsIgnoreCase("all")) {
				if (sender instanceof Player && !sender.hasPermission(Perm.TOGGLEALL.getPerm())) {
					sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.TOGGLEALL.getPerm()));
					return false;
				}

				if (Bukkit.getOnlinePlayers().isEmpty()) {
					sendMsg(sender, plugin.getMsg("toggle.no-player"));
					return false;
				}

				for (Player pl : Bukkit.getOnlinePlayers()) {
					if (!plugin.getTabManager().isPlayerInTab(pl)) {
						continue;
					}

					UUID uuid = pl.getUniqueId();
					boolean changed = TabManager.TABENABLED.containsKey(uuid) ? !TabManager.TABENABLED.get(uuid) : true;
					if (changed) {
						TabManager.TABENABLED.put(uuid, true);
						TabTitle.sendTabTitle(pl, "", "");
					} else {
						TabManager.TABENABLED.remove(uuid);
						plugin.getTabManager().getPlayerTab(pl).get().updateTab();
					}
				}

				return true;
			}

			Player pl = Bukkit.getPlayer(args[1]);
			if (pl == null) {
				sendMsg(sender, plugin.getMsg("toggle.player-not-found", "%player%", args[1]));
				return false;
			}

			if (!plugin.getTabManager().isPlayerInTab(pl)) {
				return true;
			}

			UUID uuid = pl.getUniqueId();
			boolean changed = TabManager.TABENABLED.containsKey(uuid) ? !TabManager.TABENABLED.get(uuid) : true;
			if (changed) {
				TabManager.TABENABLED.put(uuid, true);
				TabTitle.sendTabTitle(pl, "", "");
				sendMsg(pl, plugin.getMsg("toggle.disabled"));
			} else {
				TabManager.TABENABLED.remove(uuid);
				plugin.getTabManager().getPlayerTab(pl).get().updateTab();
				sendMsg(pl, plugin.getMsg("toggle.enabled"));
			}
		}

		return true;
	}
}
