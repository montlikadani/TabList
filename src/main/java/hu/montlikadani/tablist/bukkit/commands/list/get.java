package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.utils.Util;

public class get implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.GET.getPerm())) {
			sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.GET.getPerm()));
			return false;
		}

		if (!plugin.getC().getBoolean("tabname.enable")) {
			sendMsg(sender, Util.colorMsg("Tabname option is disabled in configuration!"));
			return false;
		}

		plugin.getConf().createNamesFile();

		if (!(sender instanceof Player)) {
			if (args.length == 1) {
				sendMsg(sender, plugin.getMsg("tabname.get-name.no-console", "%command%", label, "%args%", args[0]));
			} else if (args.length == 2) {
				Player targ = Bukkit.getPlayer(args[1]);
				if (targ == null) {
					sendMsg(sender, plugin.getMsg("tabname.player-not-online", "%player%", args[1]));
					return false;
				}

				String nam = targ.getName();
				if (!plugin.getConf().getNames().contains("players." + nam + ".tabname")) {
					sendMsg(sender, plugin.getMsg("tabname.no-tab-name", "%player%", nam));
					return false;
				}

				sendMsg(sender, plugin.getMsg("tabname.get-name.result", "%target%", nam, "%name%",
						plugin.getConf().getNames().getString("players." + nam + ".tabname")));
			}

			return true;
		}

		Player p = (Player) sender;
		if (args.length == 1) {
			if (!plugin.getConf().getNames().contains("players." + p.getName() + ".tabname")) {
				sendMsg(p, plugin.getMsg("tabname.no-tab-name", "%player%", p.getName()));
				return false;
			}

			sendMsg(p, plugin.getMsg("tabname.get-name.result", "%name%",
					plugin.getConf().getNames().getString("players." + p.getName() + ".tabname")));
		} else if (args.length == 2) {
			if (!p.hasPermission(Perm.GETO.getPerm())) {
				sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.GETO.getPerm()));
				return false;
			}

			Player targ = Bukkit.getPlayer(args[1]);
			if (targ == null) {
				sendMsg(p, plugin.getMsg("tabname.player-not-online", "%player%", args[1]));
				return false;
			}

			String nam = targ.getName();
			if (!plugin.getConf().getNames().contains("players." + nam + ".tabname")) {
				sendMsg(p, plugin.getMsg("tabname.no-tab-name", "%player%", nam));
				return false;
			}

			sendMsg(p, plugin.getMsg("tabname.get-name.target-result", "%target%", nam, "%name%",
					plugin.getConf().getNames().getString("players." + nam + ".tabname")));
		}

		return true;
	}
}
