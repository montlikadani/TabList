package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.reflect.TypeToken;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.CommandProcessor;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.config.ConfigValues;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.FakePlayerHandler;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.FakePlayerHandler.EditingContextError;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.IFakePlayers;
import hu.montlikadani.tablist.bukkit.utils.Util;

@CommandProcessor(name = "fakeplayers", permission = Perm.FAKEPLAYERS, playerOnly = true)
public class fakeplayers implements ICommand {

	private enum Actions {
		ADD, RENAME, SETDISPLAYNAME, SETSKIN, SETPING, REMOVE, LIST;
	}

	@SuppressWarnings("serial")
	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player) sender;

		if (!ConfigValues.isFakePlayers()) {
			sendMsg(p, plugin.getMsg("fake-player.disabled"));
			return true;
		}

		plugin.getConf().createFakePlayersFile();

		if (args.length < 2) {
			if (sender instanceof Player) {
				((Player) sender).performCommand("tl help");
			} else {
				Bukkit.dispatchCommand(sender, "tl help");
			}

			return true;
		}

		Actions action = Actions.valueOf(args[1].toUpperCase());
		if (action == null) {
			action = Actions.ADD;
		}

		if (action != Actions.LIST && args.length < 3) {
			if (sender instanceof Player) {
				((Player) sender).performCommand("tl help");
			} else {
				Bukkit.dispatchCommand(sender, "tl help");
			}

			return true;
		}

		final FakePlayerHandler handler = plugin.getFakePlayerHandler();
		EditingContextError output;

		switch (action) {
		case ADD:
			String name = args[2];
			int ping = -1;
			try {
				ping = args.length > 4 ? Integer.parseInt(args[4]) : -1;
			} catch (NumberFormatException e) {
			}

			output = handler.createPlayer(p, name, name, args.length > 3 ? args[3] : "", ping);
			if (output == EditingContextError.ALREADY_EXIST) {
				sendMsg(p, plugin.getMsg("fake-player.already-added", "%name%", name));
				return true;
			}

			if (output == EditingContextError.OK) {
				sendMsg(p, plugin.getMsg("fake-player.added", "%name%", name));
			}

			break;
		case REMOVE:
			output = handler.removePlayer(args[2]);
			if (output == EditingContextError.NOT_EXIST) {
				sendMsg(p, plugin.getMsg("fake-player.not-exists"));
				return true;
			}

			if (output == EditingContextError.OK) {
				sendMsg(p, plugin.getMsg("fake-player.removed", "%name%", args[2]));
			}

			break;
		case RENAME:
			if (args.length < 4) {
				return true;
			}

			output = handler.renamePlayer(args[2], args[3]);

			if (output == EditingContextError.NOT_EXIST) {
				sendMsg(p, plugin.getMsg("fake-player.not-exists"));
				return true;
			}

			if (output == EditingContextError.OK) {
				sendMsg(p, Util.colorMsg("&2Old name: &e" + args[2] + "&2, new name: &e" + args[3]));
			}

			break;
		case LIST:
			Set<IFakePlayers> list = handler.getFakePlayers();
			if (list.isEmpty()) {
				sendMsg(p, plugin.getMsg("fake-player.no-fake-player"));
				return true;
			}

			Collections.sort(list.stream().map(IFakePlayers::getName).collect(Collectors.toList()));

			String msg = "";
			for (IFakePlayers one : list) {
				if (!msg.isEmpty()) {
					msg += "&r, ";
				}

				msg += one.getName();
			}

			plugin.getMsg(new TypeToken<List<String>>() {}.getSubtype(List.class),
					"fake-player.list", "%amount%", list.size(), "%fake-players%", msg)
					.forEach(line -> sendMsg(p, Util.colorMsg(line)));
			break;
		case SETSKIN:
			output = handler.setSkin(args[2], args[3]);

			if (output == EditingContextError.NOT_EXIST) {
				sendMsg(p, plugin.getMsg("fake-player.not-exists"));
				return true;
			}

			if (output == EditingContextError.UUID_MATCH_ERROR) {
				p.sendMessage("This uuid not matches to a real player uuid.");
			}

			break;
		case SETPING:
			int amount = -1;
			try {
				amount = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
			}

			output = handler.setPing(args[2], amount);
			if (output == EditingContextError.NOT_EXIST) {
				sendMsg(p, plugin.getMsg("fake-player.not-exists"));
				return true;
			}

			if (output == EditingContextError.PING_AMOUNT) {
				sendMsg(p, plugin.getMsg("fake-player.ping-can-not-be-less", "%amount%", amount));
			}

			break;
		case SETDISPLAYNAME:
			output = handler.setDisplayName(args[2], args[3]);

			if (output == EditingContextError.NOT_EXIST) {
				sendMsg(p, plugin.getMsg("fake-player.not-exists"));
			}

			break;
		default:
			break;
		}

		return true;
	}
}
