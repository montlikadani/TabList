package hu.montlikadani.tablist.sponge.commands;

import java.util.UUID;
import java.util.function.Supplier;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import hu.montlikadani.tablist.sponge.TabList;
import hu.montlikadani.tablist.sponge.tablist.TabHandler;

@Command(name = "tablist", aliases = { "tablist", "tl" }, subCommands = "toggle")
public final class SpongeCommands extends ICommand implements Supplier<CommandCallable> {

	private TabList plugin;

	private CommandCallable toggleCmd;

	public SpongeCommands() {
		throw new IllegalAccessError(toString() + " can't be instantiated.");
	}

	public SpongeCommands(TabList plugin) {
		this.plugin = plugin;

		toggleCmd = CommandSpec.builder().description(Text.of("Toggle on/off the tablist."))
				.arguments(GenericArguments.optional(GenericArguments.firstParsing(
						GenericArguments.onlyOne(GenericArguments.player(Text.of("player"))),
						GenericArguments.string(Text.of("all")))))
				.permission("tablist.toggle").executor(this::handleToggle).build();

		try {
			Sponge.getCommandManager().register(plugin, get(),
					(String[]) Command.class.getMethod("aliases").invoke(null));
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
	}

	private CommandResult handleToggle(CommandSource src, CommandContext args) {
		if ("all".equalsIgnoreCase(args.<String>getOne("all").get())) {
			if (!hasPerm(src, "tablist.toggle.all")) {
				return CommandResult.empty();
			}

			for (Player pl : Sponge.getServer().getOnlinePlayers()) {
				if (!plugin.getTabHandler().isPlayerInTab(pl)) {
					continue;
				}

				UUID uuid = pl.getUniqueId();

				boolean changed = TabHandler.TABENABLED.containsKey(uuid) ? !TabHandler.TABENABLED.get(uuid) : true;
				if (changed) {
					TabHandler.TABENABLED.put(uuid, true);
				} else {
					TabHandler.TABENABLED.remove(uuid);
					plugin.getTabHandler().getPlayerTab(pl).get().loadTab();
				}
			}

			return CommandResult.success();
		}

		if (args.<Player>getOne("player").isPresent()) {
			Player p = args.<Player>getOne("player").get();
			if (!plugin.getTabHandler().isPlayerInTab(p)) {
				return CommandResult.empty();
			}

			UUID uuid = p.getUniqueId();

			boolean changed = TabHandler.TABENABLED.containsKey(uuid) ? !TabHandler.TABENABLED.get(uuid) : true;
			if (changed) {
				TabHandler.TABENABLED.put(uuid, true);
				sendMsg(src, "&cThe tab has been disabled for &e" + p.getName() + "&c!");
			} else {
				TabHandler.TABENABLED.remove(uuid);
				plugin.getTabHandler().getPlayerTab(p).get().loadTab();
				sendMsg(src, "&aThe tab has been enabled for &e" + p.getName() + "&a!");
			}

			return CommandResult.success();
		}

		if (src instanceof Player) {
			Player p = (Player) src;
			if (!plugin.getTabHandler().isPlayerInTab(p)) {
				return CommandResult.empty();
			}

			UUID uuid = p.getUniqueId();

			boolean changed = TabHandler.TABENABLED.containsKey(uuid) ? !TabHandler.TABENABLED.get(uuid) : true;
			if (changed) {
				TabHandler.TABENABLED.put(uuid, true);
				sendMsg(src, "&cThe tab has been disabled for &e" + p.getName() + "&c!");
			} else {
				TabHandler.TABENABLED.remove(uuid);
				plugin.getTabHandler().getPlayerTab(p).get().loadTab();
				sendMsg(src, "&aThe tab has been enabled for &e" + p.getName() + "&a!");
			}

			return CommandResult.success();
		}

		return CommandResult.empty();
	}

	@Override
	public CommandCallable get() {
		return CommandSpec.builder().child(toggleCmd, "toggle").description(Text.of("Toggling tablist visibility"))
				.build();
	}
}
