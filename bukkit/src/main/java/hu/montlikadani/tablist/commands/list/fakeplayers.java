package hu.montlikadani.tablist.commands.list;

import static hu.montlikadani.tablist.utils.Util.sendMsg;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import hu.montlikadani.tablist.Perm;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.commands.CommandProcessor;
import hu.montlikadani.tablist.commands.ICommand;
import hu.montlikadani.tablist.config.ConfigMessages;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.tablist.fakeplayers.FakePlayerHandler;
import hu.montlikadani.tablist.tablist.fakeplayers.FakePlayerHandler.EditingResult;
import hu.montlikadani.tablist.tablist.fakeplayers.IFakePlayer;
import hu.montlikadani.tablist.utils.Util;

@CommandProcessor(name = "fakeplayers",
		desc = "General commands for setting fake players",
		params = "add/remove/list/rename/setdisplayname/setskin/setping",
		permission = Perm.FAKEPLAYERS,
		playerOnly = true)
public final class fakeplayers implements ICommand {

	private enum Actions {
		ADD, RENAME, SETDISPLAYNAME, SETSKIN, SETPING, REMOVE, LIST
	}

	private void sendList(String label, CommandSender sender) {
		sendMsg(sender, Util.colorText("&6/" + label + " fakeplayers"
				+ "\n          &6add <name> [ping] -&7 Adds a new fake player with a name."
				+ "\n          &6remove <name> -&7 Removes the given fake player."
				+ "\n          &6list -&7 Lists all the available fake players."
				+ "\n          &6rename <oldName> <newName> -&7 Renames the already existing fake player."
				+ "\n          &6setdisplayname <name> \"displayName...\" -&7 Sets the display name of the given fake player."
				+ "\n          &6setskin <name> <uuid/playerName> -&7 Sets a skin of the given fake player."
				+ "\n          &6setskin <name> <playerName> --force -&7 Sets a skin for the given fake player forcing a web request from mojang (server can hang)."
				+ "\n          &6setping <name> <amount> -&7 Sets the ping of the given fake player."));
	}

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (!ConfigValues.isFakePlayers()) {
			sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_DISABLED));
			return true;
		}

		if (args.length == 1) {
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

			if ((output = handler.createPlayer(name, name, "", -1)) == EditingResult.ALREADY_EXIST) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_ALREADY_ADDED, "%name%", name));
				return true;
			}

			if (output == EditingResult.OK) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_ADDED, "%name%", name));
			}

			break;
		case REMOVE:
			if ((output = handler.removePlayer(args[2])) == EditingResult.NOT_EXIST) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_NOT_EXISTS));
				return true;
			}

			if (output == EditingResult.OK) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_REMOVED, "%name%", args[2]));
			}

			break;
		case RENAME:
			if (args.length < 4) {
				sendList(label, sender);
				return true;
			}

			String oldName = args[2];
			String newName = args[3];

			if ((output = handler.renamePlayer(oldName, newName)) == EditingResult.NOT_EXIST) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_NOT_EXISTS));
				return true;
			}

			if (output == EditingResult.OK) {
				sendMsg(sender, Util.colorText("&2Old name: &e" + oldName + "&2, new name: &e" + newName));
			}

			break;
		case LIST:
			Set<IFakePlayer> list = handler.fakePlayers;

			if (list.isEmpty()) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_NO_FAKE_PLAYER));
				return true;
			}

			Collections.sort(list.stream().map(IFakePlayer::getName).collect(Collectors.toList()));

			StringBuilder res = new StringBuilder();

			for (IFakePlayer one : list) {
				if (res.length() != 0) {
					res.append("&r, ");
				}

				res.append(one.getName());
			}

			ConfigMessages.getList(ConfigMessages.MessageKeys.FAKE_PLAYER_LIST, "%amount%", list.size(), "%fake-players%", res.toString()).forEach(line -> sendMsg(sender, line));
			break;
		case SETSKIN:
			if (args.length < 4) {
				sendList(label, sender);
				return true;
			}

			final String nameOrId = args[3];
			java.util.UUID id;

			if (args.length > 4 && "--force".equalsIgnoreCase(args[4])) {
				OfflinePlayer offlinePlayer = getOfflinePlayerByName(nameOrId);

				// TODO Make it async maybe?
				if (offlinePlayer == null) {
					offlinePlayer = Bukkit.getOfflinePlayer(nameOrId);
				}

				id = offlinePlayer.getUniqueId();
			} else {
				id = Util.tryParseId(nameOrId).orElseGet(() -> {
					OfflinePlayer offlinePlayer = getOfflinePlayerByName(nameOrId);

					return offlinePlayer == null ? null : offlinePlayer.getUniqueId();
				});
			}

			if (id == null) {
				sender.sendMessage("There is no player existing with this name or id.");
				return true;
			}

			if (handler.setSkin(args[2], id) == EditingResult.NOT_EXIST) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_NOT_EXISTS));
				return true;
			}

			break;
		case SETPING:
			if (args.length < 4) {
				sendList(label, sender);
				return true;
			}

			int amount;
			try {
				amount = Integer.parseInt(args[3]);
			} catch (NumberFormatException ex) {
				amount = -1;
			}

			if ((output = handler.setPing(args[2], amount)) == EditingResult.NOT_EXIST) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_NOT_EXISTS));
				return true;
			}

			if (output == EditingResult.PING_AMOUNT) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_PING_CAN_NOT_BE_LESS, "%amount%", amount));
			}

			break;
		case SETDISPLAYNAME:
			StringBuilder builder = new StringBuilder();

			for (int i = 3; i < args.length; i++) {
				builder.append(args[i] + (i + 1 < args.length ? " " : ""));
			}

			output = handler.setDisplayName(args[2], builder.toString().replace("\"", ""));

			if (output == EditingResult.NOT_EXIST) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_NOT_EXISTS));
			}

			break;
		default:
			break;
		}

		return true;
	}

	private OfflinePlayer getOfflinePlayerByName(String nameOrId) {
		try {
			OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(nameOrId);

			if (offlinePlayer != null) {
				return offlinePlayer;
			}
		} catch (NoSuchMethodError ignored) {
		}

		for (OfflinePlayer pl : Bukkit.getOfflinePlayers()) {
			if (nameOrId.equals(pl.getName())) {
				return pl;
			}
		}

		return null;
	}
}
