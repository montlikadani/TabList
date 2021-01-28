package hu.montlikadani.tablist.commands;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.plugin.PluginContainer;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.player.ITabPlayer;
import hu.montlikadani.tablist.tablist.TabHandler;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class SpongeCommands {

	private TabList plugin;

	public SpongeCommands() {
		throw new IllegalAccessError(getClass().getSimpleName() + " cannot be instantiated.");
	}

	public SpongeCommands(TabList plugin) {
		this.plugin = plugin;

		Sponge.getEventManager().registerListeners(plugin.getPluginContainer(),
				new CommandRegisterListener(plugin.getPluginContainer(),
						Command.builder().flag(Flag.builder().aliases("tablist", "tl").build())
								.parameter(Parameter.builder(CommandMapping.class).setKey("toggle").build()).build()));

		Command.builder().setShortDescription(Component.text("Toggle on/off the tablist."))
				.parameters(Parameter.playerOrTarget().optional().build(),
						Parameter.string().setKey("all").optional().build())
				.setPermission("tablist.toggle").setExecutor(this::handleToggle).build();
	}

	private CommandResult handleToggle(CommandContext context) {
		if ("all".equalsIgnoreCase(context.<String>getOne(Parameter.key("all", String.class)).orElse(""))) {
			if (!context.hasPermission("tablist.toggle.all")) {
				return CommandResult.empty();
			}

			plugin.getTabPlayers().stream().filter(pl -> plugin.getTabHandler().isPlayerInTab(pl)).forEach(pl -> {
				UUID uuid = pl.getPlayerUUID();
				boolean changed = TabHandler.TABENABLED.containsKey(uuid) ? !TabHandler.TABENABLED.get(uuid) : true;
				if (changed) {
					TabHandler.TABENABLED.put(uuid, true);
				} else {
					TabHandler.TABENABLED.remove(uuid);
					plugin.getTabHandler().getPlayerTab(pl).get().loadTab();
				}
			});

			context.sendMessage(Identity.nil(),
					plugin.getTabPlayers().isEmpty() ? Component.text("No one on the server", NamedTextColor.RED)
							: Component.text("Tab has been toggled for everyone!", NamedTextColor.DARK_GREEN));
			return CommandResult.success();
		}

		if (context.<Player>getOne(Parameter.key("player", Player.class)).isPresent()) {
			Player player = context.<Player>getOne(Parameter.key("player", Player.class)).get();
			Optional<ITabPlayer> tabPlayer = plugin.getTabPlayer(player.getUniqueId());
			if (!tabPlayer.isPresent() || !plugin.getTabHandler().isPlayerInTab(tabPlayer.get())) {
				return CommandResult.empty();
			}

			UUID uuid = player.getUniqueId();

			boolean changed = TabHandler.TABENABLED.containsKey(uuid) ? !TabHandler.TABENABLED.get(uuid) : true;
			if (changed) {
				TabHandler.TABENABLED.put(uuid, true);
				context.sendMessage(Identity.nil(), Component.text("Tab has been disabled for ", NamedTextColor.RED)
						.append(Component.text().color(NamedTextColor.YELLOW).content(player.getName())));
			} else {
				TabHandler.TABENABLED.remove(uuid);
				plugin.getTabHandler().getPlayerTab(tabPlayer.get()).get().loadTab();
				context.sendMessage(Identity.nil(), Component.text("Tab has been enabled for ", NamedTextColor.GREEN)
						.append(Component.text().color(NamedTextColor.YELLOW).content(player.getName())));
			}

			return CommandResult.success();
		}

		if (context instanceof Player) {
			Player player = (Player) context;
			Optional<ITabPlayer> tabPlayer = plugin.getTabPlayer(player.getUniqueId());
			if (!tabPlayer.isPresent() || !plugin.getTabHandler().isPlayerInTab(tabPlayer.get())) {
				return CommandResult.empty();
			}

			UUID uuid = player.getUniqueId();

			boolean changed = TabHandler.TABENABLED.containsKey(uuid) ? !TabHandler.TABENABLED.get(uuid) : true;
			if (changed) {
				TabHandler.TABENABLED.put(uuid, true);
				context.sendMessage(player, Component.text("Tab has been disabled in yourself.", NamedTextColor.RED));
			} else {
				TabHandler.TABENABLED.remove(uuid);
				plugin.getTabHandler().getPlayerTab(tabPlayer.get()).get().loadTab();
				context.sendMessage(player, Component.text("Tab has been enabled in yourself.", NamedTextColor.GREEN));
			}

			return CommandResult.success();
		}

		context.sendMessage(Identity.nil(),
				Component.text("Usage: /", NamedTextColor.RED)
						.append(Component
								.text(context.<String>getOne(Parameter.key("tablist", String.class)).orElse("tablist"))
								.append(Component.text(" toggle <player;all>", NamedTextColor.GRAY))));
		return CommandResult.empty();
	}

	final class CommandRegisterListener {

		private PluginContainer container;
		private Command command;

		public CommandRegisterListener(PluginContainer container, Command command) {
			this.container = container;
			this.command = command;
		}

		@Listener
		public void onCommandRegister(RegisterCommandEvent<Command> event) {
			event.register(container, command, "tablist", "tl");
		}
	}
}
