package hu.montlikadani.tablist;

import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.LoadedGameEvent;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.jvm.Plugin;

import com.google.inject.Inject;

import hu.montlikadani.tablist.commands.SpongeCommands;
import hu.montlikadani.tablist.player.ITabPlayer;
import hu.montlikadani.tablist.player.TabPlayer;
import hu.montlikadani.tablist.tablist.TabHandler;
import hu.montlikadani.tablist.tablist.groups.GroupTask;
import hu.montlikadani.tablist.tablist.groups.TabGroup;
import hu.montlikadani.tablist.tablist.objects.ObjectType;
import hu.montlikadani.tablist.tablist.objects.TabListObjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Plugin("tablist")
public class TabList {

	private static TabList instance;

	@Inject
	private PluginContainer pc;

	private ConfigHandlers config, animationsFile, groupsFile;

	private TabHandler tabHandler;
	private Variables variables;
	private GroupTask groupTask;
	private TabListObjects objects;

	private final Set<ITabPlayer> tabPlayers = new HashSet<>();
	private final Set<TabGroup> groupsList = new HashSet<>();
	private final Set<AnimCreator> animations = Collections.synchronizedSet(new HashSet<AnimCreator>());

	public static Scoreboard board;

	static {
		board = Sponge.getServer().getServerScoreboard().orElse(null);
		if (board == null) {
			board = Scoreboard.builder().build();
		}
	}

	@Listener(order = Order.PRE)
	public void onPluginPreInit(ConstructPluginEvent e) {
		instance = this;
	}

	@Listener(order = Order.AFTER_PRE)
	public void onPluginInit(StartingEngineEvent<Server> ev) {
		initConfigs();
		new SpongeCommands(this);

		Sponge.getEventManager().registerListeners(pc, new EventListeners());

		tabHandler = new TabHandler(this);
		variables = new Variables();
		objects = new TabListObjects(this);
	}

	@Listener
	public void onServerStarted(LoadedGameEvent event) {
		loadAnimations();
		loadGroups();
		variables.loadExpressions();
		updateAll();
	}

	@Listener
	public void onPluginStop(StoppingEngineEvent<Server> e) {
		cancelAll();

		Sponge.getEventManager().unregisterListeners(this);
		Sponge.getAsyncScheduler().getTasksByPlugin(pc).forEach(ScheduledTask::cancel);

		instance = null;
	}

	@Listener
	public void onReload(RefreshGameEvent event) {
		reload();
	}

	private void initConfigs() {
		if (config == null) {
			config = new ConfigHandlers(this, "spongeConfig.conf", true);
		}

		config.reload();
		ConfigValues.loadValues();

		if (groupsFile == null) {
			groupsFile = new ConfigHandlers(this, "groups.conf", false);
		}

		groupsFile.reload();

		if (animationsFile == null) {
			animationsFile = new ConfigHandlers(this, "animations.conf", false);
		}

		animationsFile.reload();
	}

	public void reload() {
		if (tabHandler == null) {
			tabHandler = new TabHandler(this);
		}

		tabHandler.removeAll();

		if (groupTask != null) {
			groupTask.cancel();
		}

		Sponge.getAsyncScheduler().getTasksByPlugin(pc).forEach(ScheduledTask::cancel);

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

		int last = 0;
		for (Object gr : groupsFile.getConfig().get("groups").childrenMap().keySet()) {
			String name = (String) gr;

			if (name.equalsIgnoreCase("exampleGroup")) {
				continue;
			}

			String prefix = groupsFile.getConfig().getString("", "groups", name, "prefix"),
					suffix = groupsFile.getConfig().getString("", "groups", name, "suffix"),
					permission = groupsFile.getConfig().getString("tablist." + name, "groups", name, "permission");

			int priority = groupsFile.getConfig().getInt(last + 1, "groups", name, "priority");

			groupsList.add(new TabGroup(name, prefix, suffix, permission, priority));

			last = priority;
		}
	}

	private void loadAnimations() {
		animations.clear();

		if (animationsFile == null || !animationsFile.getConfig().getFile().exists()) {
			initConfigs();
		}

		ConfigManager c = animationsFile.getConfig();
		if (!c.contains("animations")) {
			return;
		}

		for (Object o : c.get("animations").childrenMap().keySet()) {
			String name = (String) o;
			List<String> texts = c.getStringList("animations", name, "texts");
			if (texts.isEmpty()) {
				continue;
			}

			boolean random = c.getBoolean(false, "animations", name, "random");
			int time = c.getInt(200, "animations", name, "interval");
			if (time < 0) {
				animations.add(new AnimCreator(name, new ArrayList<>(texts), random));
			} else {
				animations.add(new AnimCreator(name, new ArrayList<>(texts), time, random));
			}
		}
	}

	public String makeAnim(String name) {
		if (name == null) {
			return "";
		}

		while (name.contains("%anim:") && !animations.isEmpty()) { // when using multiple animations
			synchronized (animations) {
				for (AnimCreator ac : animations) {
					name = name.replace("%anim:" + ac.getAnimName() + "%",
							ac.getTime() > 0 ? ac.getRandomText() : ac.getFirstText());
				}
			}
		}

		return name;
	}

	public void updateAll() {
		Sponge.getServer().getOnlinePlayers().forEach(this::updateAll);
	}

	public void updateAll(final Player player) {
		ITabPlayer tabPlayer = getTabPlayer(player.getUniqueId()).orElseGet(() -> {
			ITabPlayer ty = new TabPlayer(player.getUniqueId());
			tabPlayers.add(ty);
			return ty;
		});

		tabHandler.addPlayer(tabPlayer);

		if (groupTask != null) {
			groupTask.removePlayer(tabPlayer);
		} else {
			groupTask = new GroupTask();
		}

		groupTask.addPlayer(tabPlayer);
		groupTask.runTask();

		if (objects == null) {
			objects = new TabListObjects(this);
		}

		for (ObjectType t : ObjectType.values()) {
			if (t != ObjectType.HEARTH) {
				objects.unregisterObjective(tabPlayer, t.getName());
			}
		}

		if (objects.isCancelled()) {
			objects.loadObjects();
		}
	}

	public void onQuit(final Player player) {
		if (player == null) {
			return;
		}

		getTabPlayer(player.getUniqueId()).ifPresent(tabPlayer -> {
			tabHandler.removePlayer(tabPlayer);

			if (groupTask != null) {
				groupTask.removePlayer(tabPlayer);
			}

			if (objects != null) {
				objects.unregisterAllObjective(tabPlayer);
			}

			tabPlayers.remove(tabPlayer);
		});
	}

	public void cancelAll() {
		tabHandler.removeAll();

		if (objects != null) {
			objects.cancelTask();
			objects.unregisterAllObjective();
		}

		if (groupTask != null) {
			groupTask.cancel();
			tabPlayers.forEach(groupTask::removePlayer);
		}

		groupsList.clear();
		tabPlayers.clear();
	}

	public Optional<ITabPlayer> getTabPlayer(UUID uuid) {
		if (uuid != null) {
			for (ITabPlayer tabPlayer : tabPlayers) {
				if (uuid.equals(tabPlayer.getPlayerUUID())) {
					return Optional.of(tabPlayer);
				}
			}
		}

		return Optional.empty();
	}

	public Set<AnimCreator> getAnimations() {
		return animations;
	}

	public Set<TabGroup> getGroupsList() {
		return groupsList;
	}

	public Set<ITabPlayer> getTabPlayers() {
		return tabPlayers;
	}

	public GroupTask getGroupTask() {
		return groupTask;
	}

	public ConfigHandlers getC() {
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
		return pc;
	}

	public static TabList get() {
		return instance;
	}
}
