package hu.montlikadani.tablist;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import hu.montlikadani.tablist.commands.SpongeCommands;
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
import ninja.leaping.configurate.ConfigurationNode;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(id = "tablist",
		name = "TabList",
		version = "1.0.5",
		description = "An ultimate animated tablist",
		authors = "montlikadani",
		dependencies = @Dependency(id = "spongeapi", version = "7.4.0"))
public final class TabList {

	public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TabList.class);

	private ConfigHandlers config, animationsFile, groupsFile;

	private TabHandler tabHandler;
	private Variables variables;
	private GroupTask groupTask;
	private TabListObjects objects;

	private final Set<TabListUser> tabUsers = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Set<TabGroup> groupsList = new HashSet<>(2);
	private final Set<TextAnimation> animations = new HashSet<>(2);

	@Listener
	public void onPluginInit(GameInitializationEvent ev) {
		initConfigs();
		new SpongeCommands(this);

		tabHandler = new TabHandler(this);
		variables = new Variables();
		objects = new TabListObjects(this);
	}

	@Listener
	public void onServerStarted(GameStartedServerEvent event) {
		Sponge.getEventManager().registerListeners(this, new EventListeners(this));
		reload();
	}

	@Listener
	public void onPluginStop(GameStoppingEvent e) {
		cancelAll();

		Sponge.getEventManager().unregisterListeners(this);
		Sponge.getCommandManager().getOwnedBy(this).forEach(Sponge.getCommandManager()::removeMapping);
		Sponge.getScheduler().getScheduledTasks(this).forEach(Task::cancel);

		tabUsers.clear();
	}

	@Listener
	public void onReload(GameReloadEvent event) {
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

	public void reload() {
		tabHandler.removeAll();

		if (groupTask != null) {
			groupTask.cancel();

			for (TabListUser user : tabUsers) {
				groupTask.removePlayer(user);
			}
		}

		Sponge.getScheduler().getScheduledTasks(this).forEach(Task::cancel);

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

		for (Object key : node.getChildrenMap().keySet()) {
			String name = (String) key;

			if (name.equalsIgnoreCase("exampleGroup")) {
				continue;
			}

			String prefix = node.getNode(name, "prefix").getString("");
			String suffix = node.getNode(name, "suffix").getString("");
			String permission = node.getNode(name, "permission").getString("tablist." + name);

			groupsList.add(
					new TabGroup(this, name, prefix, suffix, permission, last = node.getNode(name, "priority").getInt(last + 1)));
		}
	}

	private void loadAnimations() {
		animations.clear();

		ConfigManager conf = animationsFile.get();
		ConfigurationNode node = conf.getNode("animations");

		if (!conf.contains(node)) {
			return;
		}

		for (Object o : node.getChildrenMap().keySet()) {
			String name = (String) o;
			List<String> texts = conf.getAsList(node.getNode(name, "texts"));

			if (!texts.isEmpty()) {
				animations.add(new TextAnimation(name, texts, node.getNode(name, "interval").getInt(200),
						node.getNode(name, "random").getBoolean()));
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
		for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) {
			updateAll(player);
		}
	}

	public void updateAll(final Player player) {
		UUID playerId = player.getUniqueId();

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
		groupTask.runTask(this);

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
		player.getTabList().setHeaderAndFooter(Text.EMPTY, Text.EMPTY);

		java.util.Iterator<TabListUser> iterator = tabUsers.iterator();
		UUID playerId = player.getUniqueId();

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

	public Optional<TabListUser> getUser(Player player) {
		return player == null ? Optional.empty() : getUser(player.getUniqueId());
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
}
