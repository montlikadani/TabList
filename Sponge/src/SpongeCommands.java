package hu.montlikadani.tablist.sponge;

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

	private CommandCallable toggleCmd;

	public static final Map<UUID, Boolean> TABENABLED = new HashMap<>();

	public SpongeCommands(TabList plugin) {
		this.plugin = plugin;

		toggleCmd = CommandSpec.builder().description(Text.of("Toggle on/off the tablist."))
				.arguments(GenericArguments.optional(GenericArguments.firstParsing(
						GenericArguments.onlyOne(GenericArguments.player(Text.of("player"))),
						GenericArguments.string(Text.of("all")))))
				.permission("tablist.toggle").executor(this::toggleCommand).build();
	}

	public void init() {
		Sponge.getCommandManager().register(plugin, get(), "tablist", "tl");
	}

	private CommandResult toggleCommand(CommandSource src, CommandContext args) {
		if (args.<String>getOne("all").isPresent() && args.<String>getOne("all").get().equalsIgnoreCase("all")) {
			if (!hasPerm(src, "tablist.toggle.all")) {
				return CommandResult.empty();
			}

			for (Player pl : Sponge.getServer().getOnlinePlayers()) {
				UUID uuid = pl.getUniqueId();

				boolean changed = TABENABLED.containsKey(uuid) ? !TABENABLED.get(uuid) : true;
				if (changed) {
					TABENABLED.put(uuid, true);
				} else {
					TABENABLED.remove(uuid);
					plugin.getTManager().loadTab(pl);
				}
			}

			return CommandResult.success();
		}

		if (args.<Player>getOne("player").isPresent()) {
			Player p = args.<Player>getOne("player").get();
			UUID uuid = p.getUniqueId();

			boolean changed = TABENABLED.containsKey(uuid) ? !TABENABLED.get(uuid) : true;
			if (changed) {
				TABENABLED.put(uuid, true);
				sendMsg(src, "&cThe tab has been disabled for &e" + p.getName() + "&c!");
			} else {
				TABENABLED.remove(uuid);
				plugin.getTManager().loadTab(p);
				sendMsg(src, "&aThe tab has been enabled for &e" + p.getName() + "&a!");
			}

			return CommandResult.success();
		}

		if (src instanceof Player) {
			Player p = (Player) src;
			UUID uuid = p.getUniqueId();

			boolean changed = TABENABLED.containsKey(uuid) ? !TABENABLED.get(uuid) : true;
			if (changed) {
				TABENABLED.put(uuid, true);
				sendMsg(src, "&cThe tab has been disabled for &e" + p.getName() + "&c!");
			} else {
				TABENABLED.remove(uuid);
				plugin.getTManager().loadTab(p);
				sendMsg(src, "&aThe tab has been enabled for &e" + p.getName() + "&a!");
			}

			return CommandResult.success();
		}

		return CommandResult.empty();
	}

	@Override
	public CommandCallable get() {
		return CommandSpec.builder().child(toggleCmd, "toggle").description(Text.of("Tablist modifier")).build();
	}

	private boolean hasPerm(CommandSource src, String perm) {
		return !(src instanceof Player) ? true : src.hasPermission(perm);
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
