package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.ICommand;

public class fakeplayers implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sendMsg(sender, plugin.getMsg("no-console", "%command%", label + " " + args[0]));
			return false;
		}

		Player p = (Player) sender;
		if (!p.hasPermission(Perm.FAKEPLAYERS.getPerm())) {
			sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.FAKEPLAYERS.getPerm()));
			return false;
		}

		if (!plugin.getC().getBoolean("enable-fake-players")) {
			sendMsg(p, plugin.getMsg("fake-player.disabled"));
			return false;
		}

		plugin.getConf().createFakePlayersFile();

		if (args.length < 2) {
			sendMsg(p, plugin.getMsg("fake-player.usage", "%command%", label));
			return false;
		}

		if (args[1].equalsIgnoreCase("add")) {
			if (!p.hasPermission(Perm.ADDFAKEPLAYER.getPerm())) {
				sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.ADDFAKEPLAYER.getPerm()));
				return false;
			}

			if (args.length < 3) {
				sendMsg(p, plugin.getMsg("fake-player.usage", "%command%", label));
				return false;
			}

			String name = args[2];
			if (plugin.getConf().getFakeplayers().getStringList("fakeplayers").contains(name)) {
				sendMsg(p, plugin.getMsg("fake-player.already-added", "%name%", name));
				return false;
			}

			if (plugin.getFakePlayerHandler().createPlayer(p, name)) {
				sendMsg(p, plugin.getMsg("fake-player.added", "%name%", name));
			}
		} else if (args[1].equalsIgnoreCase("remove")) {
			if (!p.hasPermission(Perm.REMOVEFAKEPLAYER.getPerm())) {
				sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.REMOVEFAKEPLAYER.getPerm()));
				return true;
			}

			if (args.length < 3) {
				sendMsg(p, plugin.getMsg("fake-player.usage", "%command%", label));
				return false;
			}

			String name = args[2];
			if (!plugin.getConf().getFakeplayers().getStringList("fakeplayers").contains(name)) {
				sendMsg(p, plugin.getMsg("fake-player.already-removed", "%name%", name));
				return false;
			}

			if (plugin.getFakePlayerHandler().removePlayer(name)) {
				sendMsg(p, plugin.getMsg("fake-player.removed", "%name%", name));
			}
		} else if (args[1].equalsIgnoreCase("list")) {
			if (!p.hasPermission(Perm.LISTFAKEPLAYERS.getPerm())) {
				sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.LISTFAKEPLAYERS.getPerm()));
				return false;
			}

			List<String> fakepls = plugin.getConf().getFakeplayers().getStringList("fakeplayers");
			if (fakepls.isEmpty()) {
				sendMsg(p, plugin.getMsg("fake-player.no-fake-player"));
				return false;
			}

			Collections.sort(fakepls);

			String msg = "";
			for (String fpl : fakepls) {
				if (!msg.isEmpty()) {
					msg += "&r, ";
				}

				msg += fpl;
			}

			for (String lpl : plugin.getMsgs().getStringList("fake-player.list")) {
				sendMsg(p, colorMsg(lpl.replace("%amount%", fakepls.size() + "").replace("%fake-players%", msg)));
			}
		}

		return true;
	}
}
