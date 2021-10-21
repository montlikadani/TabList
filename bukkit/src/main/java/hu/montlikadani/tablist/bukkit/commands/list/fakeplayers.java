package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.google.common.reflect.TypeToken;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.CommandProcessor;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.FakePlayerHandler;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.FakePlayerHandler.EditingResult;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.IFakePlayers;
import hu.montlikadani.tablist.bukkit.utils.Util;

@CommandProcessor(
		name = "fakeplayers",
		desc = "General commands for setting fake players",
		params = "add/remove/list/rename/setdisplayname/setskin",
		permission = Perm.FAKEPLAYERS,
		playerOnly = true)
public final class fakeplayers implements ICommand {

	private enum Actions {
		ADD, RENAME, SETDISPLAYNAME, SETSKIN, SETPING, REMOVE, LIST;
	}

	private void sendList(String label, CommandSender sender) {
		sendMsg(sender, Util.colorMsg("&6/" + label + " fakeplayers"
				+ "\n          &6add <name> [ping] -&7 Adds a new fake player with their name."
				+ "\n          &6remove <name> -&7 Removes the given fake player."
				+ "\n          &6list -&7 Lists all the available fake players."
				+ "\n          &6rename <oldName> <newName> -&7 Renames the already existing fake player."
				+ "\n          &6setdisplayname <name> \"displayName...\" -&7 Sets the display name of the given fake player."
				+ "\n          &6setskin <name> <uuid> -&7 Sets the skin of the given fake player."
				+ "\n          &6setping <name> <amount> -&7 Sets the ping of the given fake player."));
	}

	@SuppressWarnings("serial")
	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (!ConfigValues.isFakePlayers()) {
			sendMsg(sender, plugin.getMsg("fake-player.disabled"));
			return true;
		}

		if (args.length < 2) {
			sendList(label, sender);
			return true;
		}

		Actions action = Actions.ADD;
		try {
			action = Actions.valueOf(args[1].toUpperCase(java.util.Locale.ENGLISH));
		} catch (IllegalArgumentException e) {
		}

		if (action != Actions.LIST && args.length < 3) {
			sendList(label, sender);
			return true;
		}

		final FakePlayerHandler handler = plugin.getFakePlayerHandler();
		EditingResult output;

		switch (action) {
		case ADD:
			String name = args[2];
			int ping = args.length > 4 ? Util.tryParse(args[4]).orElse(-1) : -1;

			if ((output = handler.createPlayer(name, name, args.length > 3 ? args[3] : "",
					ping)) == EditingResult.ALREADY_EXIST) {
				sendMsg(sender, plugin.getMsg("fake-player.already-added", "%name%", name));
				return true;
			}

			if (output == EditingResult.OK) {
				sendMsg(sender, plugin.getMsg("fake-player.added", "%name%", name));
			}

			break;
		case REMOVE:
			if ((output = handler.removePlayer(args[2])) == EditingResult.NOT_EXIST) {
				sendMsg(sender, plugin.getMsg("fake-player.not-exists"));
				return true;
			}

			if (output == EditingResult.OK) {
				sendMsg(sender, plugin.getMsg("fake-player.removed", "%name%", args[2]));
			}

			break;
		case RENAME:
			if (args.length < 4) {
				return true;
			}

			if ((output = handler.renamePlayer(args[2], args[3])) == EditingResult.NOT_EXIST) {
				sendMsg(sender, plugin.getMsg("fake-player.not-exists"));
				return true;
			}

			if (output == EditingResult.OK) {
				sendMsg(sender, Util.colorMsg("&2Old name: &e" + args[2] + "&2, new name: &e" + args[3]));
			}

			break;
		case LIST:
			Set<IFakePlayers> list = handler.getFakePlayers();

			if (list.isEmpty()) {
				sendMsg(sender, plugin.getMsg("fake-player.no-fake-player"));
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

			plugin.getMsg(new TypeToken<List<String>>() {}.getSubtype(List.class), "fake-player.list", "%amount%", list.size(), "%fake-players%", msg)
					.forEach(line -> sendMsg(sender, Util.colorMsg(line)));
			break;
		case SETSKIN:
			if ((output = handler.setSkin(args[2], args[3])) == EditingResult.NOT_EXIST) {
				sendMsg(sender, plugin.getMsg("fake-player.not-exists"));
				return true;
			}

			if (output == EditingResult.UUID_MATCH_ERROR) {
				sender.sendMessage("This uuid not matches to a real player uuid.");
			}

			break;
		case SETPING:
			int amount = Util.tryParse(args[3]).orElse(-1);

			if ((output = handler.setPing(args[2], amount)) == EditingResult.NOT_EXIST) {
				sendMsg(sender, plugin.getMsg("fake-player.not-exists"));
				return true;
			}

			if (output == EditingResult.PING_AMOUNT) {
				sendMsg(sender, plugin.getMsg("fake-player.ping-can-not-be-less", "%amount%", amount));
			}

			break;
		case SETDISPLAYNAME:
			StringBuilder builder = new StringBuilder();

			for (int i = 3; i < args.length; i++) {
				builder.append(args[i] + (i + 1 < args.length ? " " : ""));
			}

			output = handler.setDisplayName(args[2], builder.toString().replace("\"", ""));

			if (output == EditingResult.NOT_EXIST) {
				sendMsg(sender, plugin.getMsg("fake-player.not-exists"));
			}

			break;
		default:
			break;
		}

		return true;
	}
}
