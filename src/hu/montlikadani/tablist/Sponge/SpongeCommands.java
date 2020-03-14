package hu.montlikadani.tablist.Sponge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.source.CommandBlockSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

public class SpongeCommands implements Supplier<CommandCallable> {

	private TabList plugin;

	private CommandCallable reloadCmd;
	private CommandCallable toggleCmd;

	public static Map<UUID, Boolean> enabled = new HashMap<>();

	public SpongeCommands(TabList plugin) {
		this.plugin = plugin;

		Builder builder = CommandSpec.builder();
		reloadCmd = builder.description(Text.of("Reloads the plugin"))
				.arguments(GenericArguments.optional(GenericArguments.none())).executor(this::reloadCommand).build();

		toggleCmd = builder.description(Text.of("Toggle on/off the tablist."))
				.arguments(GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.player(Text.of("player")))),
						GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.string(Text.of("all")))))
				.executor(this::toggleCommand).build();
	}

	public void init() {
		Sponge.getCommandManager().register(plugin, get(), "tablist", "tl");
	}

	private CommandResult reloadCommand(CommandSource src, CommandContext args) {
		if (!src.hasPermission("tablist.reload")) {
			return CommandResult.empty();
		}

		plugin.reload();
		src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize("&aConfig has been reloaded."));
		return CommandResult.success();
	}

	private CommandResult toggleCommand(CommandSource src, CommandContext args) {
		if (src instanceof ConsoleSource || src instanceof CommandBlockSource) {
			Player p = args.<Player>getOne("player").get();
			UUID uuid = p.getUniqueId();

			if (enabled.containsKey(uuid)) {
				if (!enabled.get(uuid)) {
					enabled.put(uuid, true);
					src.sendMessage(TextSerializers.FORMATTING_CODE
							.deserialize("&cThe tab has been disabled for &e" + p.getName() + "&c!"));
				} else {
					enabled.put(uuid, false);
					src.sendMessage(TextSerializers.FORMATTING_CODE
							.deserialize("&aThe tab has been enabled for &e" + p.getName() + "&a!"));
				}
			} else {
				enabled.put(uuid, true);
				src.sendMessage(TextSerializers.FORMATTING_CODE
						.deserialize("&cThe tab has been disabled for &e" + p.getName() + "&c!"));
			}

			return CommandResult.success();
		}

		if (args.hasAny("all")) {
			if (!src.hasPermission("tablist.toggle.all")) {
				return CommandResult.empty();
			}

			for (Player pl : Sponge.getServer().getOnlinePlayers()) {
				UUID uuid = pl.getUniqueId();
				if (enabled.containsKey(uuid)) {
					if (!enabled.get(uuid)) {
						enabled.put(uuid, true);
						plugin.getTManager().cancelTab(pl);
						src.sendMessage(TextSerializers.FORMATTING_CODE
								.deserialize("&cThe tablist has been disabled for all players."));
					} else {
						enabled.put(uuid, false);
						plugin.getTManager().loadTab(pl);
						src.sendMessage(TextSerializers.FORMATTING_CODE
								.deserialize("&aThe tab has been enabled for &e" + pl.getName() + "&a!"));
					}
				} else {
					enabled.put(uuid, true);
					plugin.getTManager().cancelTab(pl);
					src.sendMessage(TextSerializers.FORMATTING_CODE
							.deserialize("&cThe tablist has been disabled for all players."));
				}
			}

			return CommandResult.success();
		}

		return CommandResult.empty();
	}

	@Override
	public CommandCallable get() {
		return CommandSpec.builder().child(reloadCmd, "reload", "rl").child(toggleCmd, "toggle").build();
	}
}
