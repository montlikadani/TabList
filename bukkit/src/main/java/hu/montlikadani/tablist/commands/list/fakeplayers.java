package hu.montlikadani.tablist.commands.list;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import hu.montlikadani.tablist.utils.PlayerSkinProperties;
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

	private void sendList(TabList tl, String label, CommandSender sender) {
		tl.getComplement().sendMessage(sender, Util.applyMinimessageFormat("&6/" + label + " fakeplayers"
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
	public void run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (!ConfigValues.isFakePlayers()) {
			plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_DISABLED));
			return;
		}

		if (args.length == 1) {
			sendList(plugin, label, sender);
			return;
		}

		Actions action = Actions.ADD;
		try {
			action = Actions.valueOf(args[1].toUpperCase(java.util.Locale.ENGLISH));
		} catch (IllegalArgumentException e) {
		}

		if (action != Actions.LIST && args.length < 3) {
			sendList(plugin, label, sender);
			return;
		}

		final FakePlayerHandler handler = plugin.getFakePlayerHandler();
		EditingResult output;

		switch (action) {
		case ADD:
			String name = args[2];

			if ((output = handler.createPlayer(name, name, "", -1)) == EditingResult.ALREADY_EXIST) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_ALREADY_ADDED, "%name%", name));
				return;
			}

			if (output == EditingResult.OK) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_ADDED, "%name%", name));
			}

			break;
		case REMOVE:
			if ((output = handler.removePlayer(args[2])) == EditingResult.NOT_EXIST) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_NOT_EXISTS));
				return;
			}

			if (output == EditingResult.OK) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_REMOVED, "%name%", args[2]));
			}

			break;
		case RENAME:
			if (args.length < 4) {
				sendList(plugin, label, sender);
				return;
			}

			String oldName = args[2];
			String newName = args[3];

			if ((output = handler.renamePlayer(oldName, newName)) == EditingResult.NOT_EXIST) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_NOT_EXISTS));
				return;
			}

			if (output == EditingResult.OK) {
				plugin.getComplement().sendMessage(sender, Util.applyMinimessageFormat("&2Old name: &e" + oldName + "&2, new name: &e" + newName));
			}

			break;
		case LIST:
			Set<IFakePlayer> list = handler.fakePlayers;

			if (list.isEmpty()) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_NO_FAKE_PLAYER));
				return;
			}

			Collections.sort(list.stream().map(IFakePlayer::getName).collect(Collectors.toList()));

			StringBuilder res = new StringBuilder();

			for (IFakePlayer one : list) {
				if (res.length() != 0) {
					res.append("&r, ");
				}

				res.append(one.getName());
			}

			ConfigMessages.getList(ConfigMessages.MessageKeys.FAKE_PLAYER_LIST, "%amount%", list.size(), "%fake-players%", res.toString()).forEach(line -> plugin.getComplement().sendMessage(sender, line));
			break;
		case SETSKIN:
			if (args.length < 4) {
				sendList(plugin, label, sender);
				return;
			}

			final String nameOrId = args[3];
			java.util.Optional<UUID> optional = Util.tryParseId(nameOrId);

			if (optional.isPresent()) {
				if (handler.setSkin(args[2], new PlayerSkinProperties(null, optional.get())) == EditingResult.NOT_EXIST) {
					plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_NOT_EXISTS));
				}
			} else {
				idOfPlayer(nameOrId).whenComplete((skinProperties, t) -> {
					if (skinProperties == null && args.length > 4 && "--force".equalsIgnoreCase(args[4])) {

						// Load and retrieve from disk
						for (OfflinePlayer pl : Bukkit.getOfflinePlayers()) {
							if (nameOrId.equals(pl.getName())) {
								skinProperties = new PlayerSkinProperties(nameOrId, pl.getUniqueId());
								break;
							}
						}

						// Retrieve from blocking web request as a last resort
						if (skinProperties == null) {
							org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(nameOrId);

							skinProperties = new PlayerSkinProperties(nameOrId, offlinePlayer.getUniqueId());
						}
					}

					if (skinProperties == null) {
						plugin.getComplement().sendMessage(sender, "There is no player existing with this name or id.");
						return;
					}

					if (handler.setSkin(args[2], skinProperties) == EditingResult.NOT_EXIST) {
						plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_NOT_EXISTS));
					}
				});
			}

			break;
		case SETPING:
			if (args.length < 4) {
				sendList(plugin, label, sender);
				return;
			}

			int amount;
			try {
				amount = Integer.parseInt(args[3]);
			} catch (NumberFormatException ex) {
				amount = -1;
			}

			if ((output = handler.setPing(args[2], amount)) == EditingResult.NOT_EXIST) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_NOT_EXISTS));
				return;
			}

			if (output == EditingResult.PING_AMOUNT) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_PING_CAN_NOT_BE_LESS, "%amount%", amount));
			}

			break;
		case SETDISPLAYNAME:
			StringBuilder builder = new StringBuilder();

			for (int i = 3; i < args.length; i++) {
				builder.append(args[i] + (i + 1 < args.length ? " " : ""));
			}

			output = handler.setDisplayName(args[2], builder.toString().replace("\"", ""));

			if (output == EditingResult.NOT_EXIST) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.FAKE_PLAYER_NOT_EXISTS));
			}

			break;
		default:
			break;
		}
	}

	private CompletableFuture<PlayerSkinProperties> idOfPlayer(String playerName) {
		try {
			OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);

			if (offlinePlayer != null) {
				CompletableFuture<PlayerSkinProperties> future = new CompletableFuture<>();

				future.complete(new PlayerSkinProperties(offlinePlayer.getName(), offlinePlayer.getUniqueId(), null, null));
				return future;
			}
		} catch (NoSuchMethodError ignored) {
		}

		return hu.montlikadani.tablist.utils.datafetcher.URLDataFetcher.fetchProfile(playerName);
	}
}
