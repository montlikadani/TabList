package hu.montlikadani.tablist;

import org.spongepowered.api.Engine;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.LoadedGameEvent;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import com.google.inject.Inject;

import hu.montlikadani.tablist.config.ConfigHandlers;
import hu.montlikadani.tablist.config.ConfigManager;
import hu.montlikadani.tablist.config.ConfigValues;
import hu.montlikadani.tablist.tablist.TabHandler;
import hu.montlikadani.tablist.tablist.groups.GroupTask;
import hu.montlikadani.tablist.tablist.groups.TabGroup;
import hu.montlikadani.tablist.tablist.objects.ObjectType;
import hu.montlikadani.tablist.tablist.objects.TabListObjects;
import hu.montlikadani.tablist.user.TabListPlayer;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.Variables;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Plugin("tablist")
public final class TabList {

	private ConfigHandlers config, animationsFile, groupsFile;

	private TabHandler tabHandler;
	private Variables variables;
	private GroupTask groupTask;
	private TabListObjects objects;

	private final PluginContainer plugin;

	private final Set<TabListUser> tabUsers = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Set<TabGroup> groupsList = new HashSet<>(2);
	private final Set<TextAnimation> animations = new HashSet<>(2);

	@Inject
	TabList(PluginContainer plugin) {
		this.plugin = plugin;
	}

	@Listener
	public void onPluginInit(StartingEngineEvent<Engine> ev) {
		initConfigs();

		tabHandler = new TabHandler(this);
		variables = new Variables();
		objects = new TabListObjects(this);
	}

	@Listener
	public void onServerStarted(LoadedGameEvent event) {
		Sponge.eventManager().registerListeners(plugin, new EventListeners(this));
		reload();
	}

	@Listener
	public void onPluginStop(StoppingEngineEvent<Engine> e) {
		cancelAll();

		Sponge.eventManager().unregisterListeners(this);
		cancelAllTask();

		tabUsers.clear();
	}

	private Parameter.Value<String> allValue;
	private Parameter.Value<ServerPlayer> playerValue;

	private Component tlUsage, tabDisabled, tabEnabled, noOneOnTheServer, tabVisibilitySwitched;

	@Listener
	public void onRegisterCommands(RegisterCommandEvent<Command.Parameterized> event) {
		Command.Parameterized toggleCommand = Command.builder().permission("tablist.toggle")
				.shortDescription(Component.text("Toggles the visibility of tablist"))
				.addParameters(playerValue = Parameter.player().key("player").optional().build(),
						allValue = Parameter.string().key("all").optional().requiredPermission("tablist.toggle.all")
								.completer((context, currentInput) -> currentInput.isEmpty()
										? java.util.Arrays.asList(CommandCompletion.of("all"))
										: java.util.Arrays.asList())
								.build())
				.executor(this::processToggle).build();

		event.register(plugin,
				Command.builder().addChild(toggleCommand, "toggle").terminal(true).executor(this::processToggle).build(),
				"tablist", "tl");
	}

	private CommandResult processToggle(CommandContext context) {
		Optional<ServerPlayer> opt = context.one(playerValue);

		if (opt.isPresent()) {
			getUser(opt.get().uniqueId()).ifPresent(user -> {
				if (TabHandler.TABENABLED.remove(user.getUniqueId()) == null) {
					TabHandler.TABENABLED.put(user.getUniqueId(), true);

					if (tabDisabled == null) {
						tabDisabled = Component.text("Tab has been disabled for ", NamedTextColor.RED);
					}

					context.sendMessage(Identity.nil(), tabDisabled.append(user.getName()).color(NamedTextColor.YELLOW));
				} else {
					user.getTabListManager().loadTab();

					if (tabEnabled == null) {
						tabEnabled = Component.text("Tab has been enabled for ", NamedTextColor.GREEN);
					}

					context.sendMessage(Identity.nil(), tabEnabled.append(user.getName()).color(NamedTextColor.YELLOW));
				}
			});
		} else if (context.hasAny(allValue)) {
			if (tabUsers.isEmpty()) {
				if (noOneOnTheServer == null) {
					noOneOnTheServer = Component.text("No one on the server", NamedTextColor.RED);
				}

				context.sendMessage(Identity.nil(), noOneOnTheServer);
			} else {
				for (TabListUser user : tabUsers) {
					if (TabHandler.TABENABLED.remove(user.getUniqueId()) == null) {
						TabHandler.TABENABLED.put(user.getUniqueId(), true);
					} else {
						user.getTabListManager().loadTab();
					}
				}

				if (tabVisibilitySwitched == null) {
					tabVisibilitySwitched = Component.text("Tab has been switched for every player.", NamedTextColor.DARK_GREEN);
				}

				context.sendMessage(Identity.nil(), tabVisibilitySwitched);
			}
		} else {
			if (tlUsage == null) {
				tlUsage = Component.text("Usage:", NamedTextColor.RED).append(Component.text(" /tablist toggle playerName/all"));
			}

			context.sendMessage(Identity.nil(), tlUsage);
		}

		return CommandResult.success();
	}

	@Listener
	public void onReload(RefreshGameEvent event) {
		reload();
	}

	private void initConfigs() {
		if (config == null) {
			config = new ConfigHandlers(this, "spongeConfig.conf", true);
		}

		if (groupsFile == null) {
			groupsFile = new ConfigHandlers(this, "groups.conf", false);
		}

		if (animationsFile == null) {
			animationsFile = new ConfigHandlers(this, "animations.conf", false);
		}

		config.reload();
		groupsFile.reload();
		animationsFile.reload();
		ConfigValues.loadValues(config.get());
	}

	private void cancelAllTask() {
		Sponge.asyncScheduler().tasks(plugin).forEach(ScheduledTask::cancel);
		Sponge.server().scheduler().tasks(plugin).forEach(ScheduledTask::cancel);
	}

	public void reload() {
		tabHandler.removeAll();

		if (groupTask != null) {
			groupTask.cancel();

			for (TabListUser user : tabUsers) {
				groupTask.removePlayer(user);
			}
		}

		cancelAllTask();

		initConfigs();
		loadAnimations();
		loadGroups();
		variables.loadExpressions();
		updateAll();
	}

	private void loadGroups() {
		groupsList.clear();

		if (!ConfigValues.isTablistGroups()) {
			return;
		}

		ConfigManager conf = groupsFile.get();
		ConfigurationNode node = conf.getNode("groups");

		if (!conf.contains(node)) {
			return;
		}

		int last = 0;

		for (Object key : node.childrenMap().keySet()) {
			String name = (String) key;

			if (name.equalsIgnoreCase("exampleGroup")) {
				continue;
			}

			String prefix = node.node(name, "prefix").getString("");
			String suffix = node.node(name, "suffix").getString("");
			String permission = node.node(name, "permission").getString("tablist." + name);

			groupsList.add(
					new TabGroup(this, name, prefix, suffix, permission, last = node.node(name, "priority").getInt(last + 1)));
		}
	}

	private void loadAnimations() {
		animations.clear();

		ConfigManager conf = animationsFile.get();
		ConfigurationNode node = conf.getNode("animations");

		if (!conf.contains(node)) {
			return;
		}

		for (Object o : node.childrenMap().keySet()) {
			String name = (String) o;
			List<String> texts = conf.getAsList(node.node(name, "texts"));

			if (!texts.isEmpty()) {
				animations.add(new TextAnimation(name, texts, node.node(name, "interval").getInt(200),
						node.node(name, "random").getBoolean(false)));
			}
		}
	}

	public String makeAnim(String str) {
		if (str.isEmpty()) {
			return "";
		}

		int a = 0; // Avoid infinite loop

		while (a < 100 && !animations.isEmpty() && str.indexOf("%anim:") != -1) { // when using multiple animations
			for (TextAnimation ac : animations) {
				str = str.replace("%anim:" + ac.getName() + "%", ac.getText());
			}

			a++;
		}

		return str;
	}

	public void updateAll() {
		for (ServerPlayer player : Sponge.game().server().onlinePlayers()) {
			updateAll(player);
		}
	}

	public void updateAll(final ServerPlayer player) {
		UUID playerId = player.uniqueId();

		TabListUser user = getUser(playerId).orElseGet(() -> {
			TabListUser tlu = new TabListPlayer(this, playerId);
			tabUsers.add(tlu);
			return tlu;
		});

		tabHandler.addPlayer(user);

		if (groupTask != null) {
			groupTask.removePlayer(user);
		} else {
			groupTask = new GroupTask(this);
		}

		groupTask.addPlayer(user);
		groupTask.runTask();

		for (ObjectType t : ObjectType.VALUES) {
			if (t != ObjectType.HEARTH) {
				objects.unregisterObjective(t.getName());
			}
		}

		if (ConfigValues.getTablistObjectsType() == ObjectType.HEARTH) {
			objects.loadHealthObject(player);
		} else if (objects.isCancelled()) {
			objects.loadObjects();
		}
	}

	public void onQuit(Player player) {
		player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());

		java.util.Iterator<TabListUser> iterator = tabUsers.iterator();
		UUID playerId = player.uniqueId();

		while (iterator.hasNext()) {
			TabListUser user = iterator.next();

			if (playerId.equals(user.getUniqueId())) {
				if (groupTask != null) {
					groupTask.removePlayer(user);
				}

				iterator.remove();
				break;
			}
		}
	}

	public void cancelAll() {
		tabHandler.removeAll();

		objects.cancelTask();
		objects.unregisterAllObjective();

		if (groupTask != null) {
			groupTask.cancel();

			for (TabListUser user : tabUsers) {
				groupTask.removePlayer(user);
			}
		}

		groupsList.clear();
	}

	public Optional<TabListUser> getUser(ServerPlayer player) {
		return player == null ? Optional.empty() : getUser(player.uniqueId());
	}

	public Optional<TabListUser> getUser(UUID uuid) {
		if (uuid == null) {
			return Optional.empty();
		}

		for (TabListUser tlp : tabUsers) {
			if (uuid.equals(tlp.getUniqueId())) {
				return Optional.of(tlp);
			}
		}

		return Optional.empty();
	}

	public Set<TabListUser> getTabUsers() {
		return tabUsers;
	}

	public Set<TextAnimation> getAnimations() {
		return animations;
	}

	public Set<TabGroup> getGroupsList() {
		return groupsList;
	}

	public GroupTask getGroupTask() {
		return groupTask;
	}

	public ConfigHandlers getConfig() {
		return config;
	}

	public ConfigHandlers getGroups() {
		return groupsFile;
	}

	public ConfigHandlers getAnimationsFile() {
		return animationsFile;
	}

	public TabHandler getTabHandler() {
		return tabHandler;
	}

	public Variables getVariables() {
		return variables;
	}

	public TabListObjects getTabListObjects() {
		return objects;
	}

	public PluginContainer getPluginContainer() {
		return plugin;
	}
}
