package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabHandler;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.ICommand;

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
			UUID uuid = p.getUniqueId();

			if (TabHandler.tabEnabled.containsKey(uuid)) {
				if (!TabHandler.tabEnabled.get(uuid)) {
					TabHandler.tabEnabled.put(uuid, true);
					sendMsg(p, plugin.getMsg("toggle.disabled"));
				} else {
					TabHandler.tabEnabled.put(uuid, false);
					sendMsg(p, plugin.getMsg("toggle.enabled"));
				}
			} else {
				TabHandler.tabEnabled.put(uuid, true);
				sendMsg(p, plugin.getMsg("toggle.disabled"));
			}
		} else if (args.length == 2) {
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
					UUID uuid = pl.getUniqueId();
					if (TabHandler.tabEnabled.containsKey(uuid)) {
						if (!TabHandler.tabEnabled.get(uuid)) {
							TabHandler.tabEnabled.put(uuid, true);
							plugin.getTabHandler().cancelTabForPlayer(pl, true);
						} else {
							TabHandler.tabEnabled.put(uuid, false);
							plugin.getTabHandler().updateTab(pl);
						}
					} else {
						TabHandler.tabEnabled.put(uuid, true);
						plugin.getTabHandler().cancelTabForPlayer(pl, true);
					}
				}

				return true;
			}

			Player pl = Bukkit.getPlayer(args[1]);
			if (pl == null) {
				sendMsg(sender, plugin.getMsg("toggle.player-not-found", "%player%", args[1]));
				return false;
			}

			UUID uuid = pl.getUniqueId();
			if (TabHandler.tabEnabled.containsKey(uuid)) {
				if (!TabHandler.tabEnabled.get(uuid)) {
					TabHandler.tabEnabled.put(uuid, true);
					sendMsg(sender, plugin.getMsg("toggle.disabled"));
				} else {
					TabHandler.tabEnabled.put(uuid, false);
					sendMsg(sender, plugin.getMsg("toggle.enabled"));
				}
			} else {
				TabHandler.tabEnabled.put(uuid, true);
				sendMsg(sender, plugin.getMsg("toggle.disabled"));
			}
		}

		return true;
	}
}
