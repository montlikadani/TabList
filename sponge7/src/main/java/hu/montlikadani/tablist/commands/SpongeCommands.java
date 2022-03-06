package hu.montlikadani.tablist.commands;

import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.tablist.TabHandler;
import hu.montlikadani.tablist.user.TabListUser;

public final class SpongeCommands extends ICommand {

	private final TabList plugin;

	public SpongeCommands() {
		throw new IllegalAccessError(getClass().getSimpleName() + " can't be instantiated.");
	}

	public SpongeCommands(TabList plugin) {
		this.plugin = plugin;

		Text desc = Text.of("Toggles the visibility of tablist");

		Sponge.getCommandManager().register(plugin,
				CommandSpec.builder()
						.child(CommandSpec.builder().description(desc)
								.arguments(GenericArguments.optional(GenericArguments.firstParsing(
										GenericArguments.onlyOne(GenericArguments.player(Text.of("player"))),
										GenericArguments.string(Text.of("all")))))
								.permission("tablist.toggle").executor(this::processToggle).build(), "toggle")
						.description(desc).build(),
				new String[] { "tablist", "tl" });
	}

	private CommandResult processToggle(CommandSource src, CommandContext args) {
		if ("all".equalsIgnoreCase(args.<String>getOne("all").orElse(""))) {
			if (!hasPerm(src, "tablist.toggle.all")) {
				return CommandResult.empty();
			}

			for (TabListUser user : plugin.getTabUsers()) {
				if (TabHandler.TABENABLED.remove(user.getUniqueId()) == null) {
					TabHandler.TABENABLED.put(user.getUniqueId(), true);
				} else {
					user.getTabListManager().loadTab();
				}
			}

			sendMsg(src, plugin.getTabUsers().isEmpty() ? "&cNo one on the server" : "&2Tab has been switched for everyone!");
			return CommandResult.success();
		}

		Optional<Player> one = args.<Player>getOne("player");

		if (one.isPresent()) {
			Player target = one.get();

			plugin.getUser(target).ifPresent(user -> {
				if (TabHandler.TABENABLED.remove(user.getUniqueId()) == null) {
					TabHandler.TABENABLED.put(user.getUniqueId(), true);
					sendMsg(src, "&cTab has been disabled for &e" + target.getName() + "&c!");
				} else {
					user.getTabListManager().loadTab();
					sendMsg(src, "&aTab has been enabled for &e" + target.getName() + "&a!");
				}
			});

			return CommandResult.success();
		}

		if (src instanceof Player) {
			plugin.getUser((Player) src).ifPresent(user -> {
				if (TabHandler.TABENABLED.remove(user.getUniqueId()) == null) {
					TabHandler.TABENABLED.put(user.getUniqueId(), true);
					sendMsg(src, "&cTab has been disabled for you.");
				} else {
					user.getTabListManager().loadTab();
					sendMsg(src, "&aTab has been enabled for you.");
				}
			});

			return CommandResult.success();
		}

		sendMsg(src, "&cUsage: /" + args.<String>getOne("tablist").orElse("tablist") + " toggle <player;all>");
		return CommandResult.empty();
	}
}
