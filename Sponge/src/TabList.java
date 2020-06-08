package hu.montlikadani.tablist.sponge;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scoreboard.Scoreboard;

import com.google.inject.Inject;

import hu.montlikadani.tablist.sponge.tablist.TabListManager;
import hu.montlikadani.tablist.sponge.tablist.groups.GroupTask;
import hu.montlikadani.tablist.sponge.tablist.groups.TabGroup;
import hu.montlikadani.tablist.sponge.tablist.objects.TabListObjects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Plugin(id = "tablist", name = "TabList", version = "1.0.0", description = "An ultimate animated tablist", authors = "montlikadani")
public class TabList {

	private static TabList instance = null;

	@Inject
	private PluginContainer pc;

	private ConfigHandlers config, animationsFile, groupsFile;

	private TabListManager tManager;
	private Variables variables;
	private GroupTask groupTask;
	private TabListObjects objects;

	private final Set<TabGroup> groupsList = new HashSet<>();
	private final Set<AnimCreator> animations = new HashSet<>();

	public static final Scoreboard BOARD = Sponge.getServer().getServerScoreboard()
			.orElse(Scoreboard.builder().build());

	@Listener
	public void onPluginPreInit(GamePreInitializationEvent e) {
		instance = this;
	}

	@Listener
	public void onPluginInit(GameInitializationEvent ev) {
		initConfigs();
		initCommands();

		tManager = new TabListManager(this);
		variables = new Variables();
		objects = new TabListObjects(this);
	}

	@Listener
	public void onServerStarted(GameStartedServerEvent event) {
		Sponge.getEventManager().registerListeners(this, new EventListeners());

		reload();
	}

	@Listener
	public void onPluginStop(GameStoppingEvent e) {
		cancelAll();

		Sponge.getEventManager().unregisterListeners(this);
		Sponge.getCommandManager().getOwnedBy(this).forEach(Sponge.getCommandManager()::removeMapping);
		Sponge.getScheduler().getScheduledTasks(this).forEach(Task::cancel);

		instance = null;
	}

	@Listener
	public void onReload(GameReloadEvent event) {
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

	private void initCommands() {
		SpongeCommands sc = new SpongeCommands(this);
		sc.init();
	}

	public void reload() {
		if (tManager == null) {
			tManager = new TabListManager(this);
		}

		tManager.cancelTabForAll();

		if (groupTask != null) {
			groupTask.cancel();
		}

		Sponge.getScheduler().getScheduledTasks(this).forEach(Task::cancel);

		initConfigs();
		loadAnimations();
		loadGroups();
		updateAll();
	}

	private void loadGroups() {
		groupsList.clear();

		if (!ConfigValues.isTablistGroups()) {
			return;
		}

		int last = 0;
		for (Object gr : groupsFile.getConfig().get("groups").getChildrenMap().keySet()) {
			String name = (String) gr;

			if (name.equalsIgnoreCase("exampleGroup")) {
				continue;
			}

			String prefix = groupsFile.getConfig().getString("", "groups", name, "prefix");
			String suffix = groupsFile.getConfig().getString("", "groups", name, "suffix");
			String permission = groupsFile.getConfig().getString("tablist." + name, "groups", name, "permission");

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

		for (Object o : c.get("animations").getChildrenMap().keySet()) {
			String name = (String) o;
			List<String> texts = c.getStringList("animations", name, "texts");
			if (texts.isEmpty()) {
				continue;
			}

			boolean random = c.getBoolean(false, "animations", name, "random");
			int time = c.getInt(200, "animations", name, "interval");
			if (time < 0) {
				animations.add(new AnimCreator(name, new ArrayList<String>(texts), random));
			} else {
				animations.add(new AnimCreator(name, new ArrayList<String>(texts), time, random));
			}
		}
	}

	public String makeAnim(String name) {
		for (AnimCreator ac : animations) {
			name = name.replace("%anim:" + ac.getAnimName() + "%",
					ac.getTime() > 0 ? ac.getRandomText() : ac.getFirstText());
		}

		return name;
	}

	public void updateAll() {
		Sponge.getServer().getOnlinePlayers().forEach(this::updateAll);
	}

	public void updateAll(final Player player) {
		tManager.cancelTab(player);
		tManager.loadTab(player);

		if (groupTask != null) {
			groupTask.removePlayer(player);
		} else {
			groupTask = new GroupTask();
		}

		groupTask.addPlayer(player);
		groupTask.runTask();

		if (objects == null) {
			objects = new TabListObjects(this);
		}

		objects.unregisterAllObjective(player);

		if (objects.isCancelled()) {
			objects.loadObjects();
		}
	}

	public void onQuit(Player player) {
		tManager.cancelTab(player);

		if (groupTask != null) {
			groupTask.removePlayer(player);
		}

		if (objects != null) {
			objects.unregisterAllObjective(player);
		}
	}

	public void cancelAll() {
		tManager.cancelTabForAll();

		if (objects != null) {
			objects.cancelTask();
			objects.unregisterAllObjective();
		}

		if (groupTask != null) {
			groupTask.cancel();
			Sponge.getServer().getOnlinePlayers().forEach(groupTask::removePlayer);
		}

		groupsList.clear();
	}

	public Set<AnimCreator> getAnimations() {
		return animations;
	}

	public Set<TabGroup> getGroupsList() {
		return groupsList;
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

	public TabListManager getTManager() {
		return tManager;
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
