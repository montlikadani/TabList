package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.CommandProcessor;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.config.ConfigValues;
import hu.montlikadani.tablist.bukkit.config.Configuration;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.FakePlayerHandler;
import hu.montlikadani.tablist.bukkit.utils.Util;

@CommandProcessor(name = "fakeplayers", permission = Perm.FAKEPLAYERS, playerOnly = true)
public class fakeplayers implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player) sender;
		if (!ConfigValues.isFakePlayers()) {
			sendMsg(p, plugin.getMsg("fake-player.disabled"));
			return false;
		}

		plugin.getConf().createFakePlayersFile();

		if (args.length < 2) {
			if (sender instanceof Player) {
				((Player) sender).performCommand("tl help");
			} else {
				Bukkit.dispatchCommand(sender, "tl help");
			}

			return false;
		}

		final FakePlayerHandler handler = plugin.getFakePlayerHandler();
		if (args[1].equalsIgnoreCase("add")) {
			if (!p.hasPermission(Perm.ADDFAKEPLAYER.getPerm())) {
				sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.ADDFAKEPLAYER.getPerm()));
				return false;
			}

			if (args.length < 3) {
				if (sender instanceof Player) {
					((Player) sender).performCommand("tl help");
				} else {
					Bukkit.dispatchCommand(sender, "tl help");
				}

				return false;
			}

			String name = args[2];
			if (handler.getFakePlayerByName(name).isPresent()) {
				sendMsg(p, plugin.getMsg("fake-player.already-added", "%name%", name));
				return false;
			}

			String headUUID = args.length > 3 ? args[3] : "";
			int ping = -1;
			try {
				ping = args.length > 4 ? Integer.parseInt(args[4]) : -1;
			} catch (NumberFormatException e) {
			}

			if (handler.createPlayer(p, name, headUUID, ping)) {
				sendMsg(p, plugin.getMsg("fake-player.added", "%name%", name));
			}
		} else if (args[1].equalsIgnoreCase("setskin")) {
			if (!p.hasPermission(Perm.SETSKINFAKEPLAYER.getPerm())) {
				sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.SETSKINFAKEPLAYER.getPerm()));
				return false;
			}

			if (args.length < 3) {
				if (sender instanceof Player) {
					((Player) sender).performCommand("tl help");
				} else {
					Bukkit.dispatchCommand(sender, "tl help");
				}

				return false;
			}

			if (!handler.getFakePlayerByName(args[2]).isPresent()) {
				sendMsg(p, plugin.getMsg("fake-player.not-exists"));
				return false;
			}

			String uuid = args[3];
			if (!Util.isRealUUID(uuid)) {
				p.sendMessage("This uuid not matches to a real player uuid.");
				return false;
			}

			handler.getFakePlayerByName(args[2]).get().setSkin(uuid);

			String result = "";
			List<String> fakepls = handler.getFakePlayersFromConfig();
			int i = 0;
			for (; i < fakepls.size(); i++) {
				String l = fakepls.get(i);
				String[] split = l.contains(";") ? l.split(";") : new String[] { l };
				String name = split[0];
				if (name.equalsIgnoreCase(args[2])) {
					result = name + ";" + uuid + (split.length > 2 ? ";" + split[2] : "");
					break;
				}
			}

			if (result.isEmpty()) {
				return false;
			}

			fakepls.remove(i);
			fakepls.add(result);

			Configuration conf = plugin.getConf();
			conf.getFakeplayers().set("fakeplayers", fakepls);
			try {
				conf.getFakeplayers().save(conf.getFakeplayersFile());
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		} else if (args[1].equalsIgnoreCase("setping")) {
			if (!p.hasPermission(Perm.SETPINGFAKEPLAYER.getPerm())) {
				sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.SETPINGFAKEPLAYER.getPerm()));
				return false;
			}

			if (args.length < 3) {
				if (sender instanceof Player) {
					((Player) sender).performCommand("tl help");
				} else {
					Bukkit.dispatchCommand(sender, "tl help");
				}

				return false;
			}

			if (!handler.getFakePlayerByName(args[2]).isPresent()) {
				sendMsg(p, plugin.getMsg("fake-player.not-exists"));
				return false;
			}

			int amount = -1;
			try {
				amount = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
			}

			if (amount < 0) {
				sendMsg(p, plugin.getMsg("fake-player.ping-can-not-be-less", "%amount%", amount));
				return false;
			}

			handler.getFakePlayerByName(args[2]).get().setPing(amount);

			String result = "";
			List<String> fakepls = handler.getFakePlayersFromConfig();
			int i = 0;
			for (; i < fakepls.size(); i++) {
				String l = fakepls.get(i);
				String[] split = l.contains(";") ? l.split(";") : new String[] { l };
				String name = split[0];
				if (name.equalsIgnoreCase(args[2])) {
					result = name + ";" + (split.length > 1 ? split[1] : "uuid") + ";" + amount;
					break;
				}
			}

			if (result.isEmpty()) {
				return false;
			}

			fakepls.remove(i);
			fakepls.add(result);

			Configuration conf = plugin.getConf();
			conf.getFakeplayers().set("fakeplayers", fakepls);
			try {
				conf.getFakeplayers().save(conf.getFakeplayersFile());
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		} else if (args[1].equalsIgnoreCase("remove")) {
			if (!p.hasPermission(Perm.REMOVEFAKEPLAYER.getPerm())) {
				sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.REMOVEFAKEPLAYER.getPerm()));
				return true;
			}

			if (args.length < 3) {
				if (sender instanceof Player) {
					((Player) sender).performCommand("tl help");
				} else {
					Bukkit.dispatchCommand(sender, "tl help");
				}

				return false;
			}

			String name = args[2];
			if (handler.getFakePlayerByName(name).isPresent()) {
				sendMsg(p, plugin.getMsg("fake-player.not-exists"));
				return false;
			}

			if (handler.removePlayer(name)) {
				sendMsg(p, plugin.getMsg("fake-player.removed", "%name%", name));
			}
		} else if (args[1].equalsIgnoreCase("list")) {
			if (!p.hasPermission(Perm.LISTFAKEPLAYERS.getPerm())) {
				sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.LISTFAKEPLAYERS.getPerm()));
				return false;
			}

			List<String> fakepls = handler.getFakePlayersFromConfig();
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

			for (String lpl : plugin.getConf().getMessages().getStringList("fake-player.list")) {
				sendMsg(p, Util.colorMsg(lpl.replace("%amount%", fakepls.size() + "").replace("%fake-players%", msg)));
			}
		}

		return true;
	}
}
