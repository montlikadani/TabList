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
import org.spongepowered.api.command.spec.CommandSpec;
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

		reloadCmd = CommandSpec.builder().description(Text.of("Reloads the plugin"))
				.arguments(GenericArguments.optional(GenericArguments.none())).permission("tablist.reload")
				.executor(this::reloadCommand).build();

		toggleCmd = CommandSpec.builder().description(Text.of("Toggle on/off the tablist."))
				.arguments(GenericArguments.optional(GenericArguments.player(Text.of("player"))),
						GenericArguments.optional(GenericArguments.string(Text.of("all"))))
				.permission("tablist.toggle").executor(this::toggleCommand).build();
	}

	public void init() {
		Sponge.getCommandManager().register(plugin, get(), "tablist", "tl");
	}

	private CommandResult reloadCommand(CommandSource src, CommandContext args) {
		plugin.reload();
		sendMsg(src, "&aConfig has been reloaded.");
		return CommandResult.success();
	}

	private CommandResult toggleCommand(CommandSource src, CommandContext args) {
		if (args.hasAny("player")) {
			Player p = args.<Player>getOne("player").get();
			UUID uuid = p.getUniqueId();

			if (enabled.containsKey(uuid)) {
				if (!enabled.get(uuid)) {
					enabled.put(uuid, true);
					sendMsg(src, "&cThe tab has been disabled for &e" + p.getName() + "&c!");
				} else {
					enabled.put(uuid, false);
					sendMsg(src, "&aThe tab has been enabled for &e" + p.getName() + "&a!");
				}
			} else {
				enabled.put(uuid, true);
				sendMsg(src, "&cThe tab has been disabled for &e" + p.getName() + "&c!");
			}

			return CommandResult.success();
		} else if (args.hasAny("all")) {
			if (!hasPerm(src, "tablist.toggle.all")) {
				return CommandResult.empty();
			}

			for (Player pl : Sponge.getServer().getOnlinePlayers()) {
				UUID uuid = pl.getUniqueId();
				if (enabled.containsKey(uuid)) {
					if (!enabled.get(uuid)) {
						enabled.put(uuid, true);
						plugin.getTManager().cancelTab(pl);
					} else {
						enabled.put(uuid, false);
						plugin.getTManager().loadTab(pl);
					}
				} else {
					enabled.put(uuid, true);
					plugin.getTManager().cancelTab(pl);
				}
			}

			return CommandResult.success();
		} else if (src instanceof Player) {
			Player p = (Player) src;
			UUID uuid = p.getUniqueId();
			if (enabled.containsKey(uuid)) {
				if (!enabled.get(uuid)) {
					enabled.put(uuid, true);
					sendMsg(src, "&cThe tab has been disabled for &e" + p.getName() + "&c!");
				} else {
					enabled.put(uuid, false);
					sendMsg(src, "&aThe tab has been enabled for &e" + p.getName() + "&a!");
				}
			} else {
				enabled.put(uuid, true);
				sendMsg(src, "&cThe tab has been disabled for &e" + p.getName() + "&c!");
			}

			return CommandResult.success();
		}

		return CommandResult.empty();
	}

	@Override
	public CommandCallable get() {
		return CommandSpec.builder().child(reloadCmd, "reload", "rl").child(toggleCmd, "toggle").build();
	}

	private boolean hasPerm(CommandSource src, String perm) {
		if (!(src instanceof Player)) {
			return true;
		}

		return src.hasPermission(perm);
	}

	private void sendMsg(CommandSource src, String msg) {
		if (msg != null && !msg.trim().isEmpty()) {
			sendMsg(src, TextSerializers.FORMATTING_CODE.deserialize(msg));
		}
	}

	private void sendMsg(CommandSource src, Text text) {
		src.sendMessage(text);
	}
}
