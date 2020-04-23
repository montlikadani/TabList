package hu.montlikadani.tablist.Sponge;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Plugin(id = "tablist", name = "TabList", version = "1.0", description = "An ultimate animated tablist", authors = "montlikadani")
public class TabList {

	private static TabList instance = null;

	@Inject
	private PluginContainer pc;

	private ConfigHandlers config, animationsFile;

	private TabListManager tManager;
	private Variables variables;

	private final Set<AnimCreator> animations = new HashSet<>();

	@Listener
	public void onPluginPreInit(GamePreInitializationEvent e) {
		instance = this;
	}

	@Listener
	public void onPluginInit(GameInitializationEvent ev) {
		initConfigs();
		initCommands();

		tManager = new TabListManager(this);
		variables = new Variables(this);
	}

	@Listener
	public void onServerStarted(GameStartedServerEvent event) {
		Sponge.getEventManager().registerListeners(this, new EventListeners());
	}

	@Listener
	public void onPluginStop(GameStoppingEvent e) {
		tManager.cancelTabForAll();

		instance = null;
	}

	@Listener
	public void onReload(GameReloadEvent event) {
		reload();
	}

	private void initConfigs() {
		if (config == null) {
			config = new ConfigHandlers(this, "spongeConfig.conf");
		}

		config.reload();

		if (animationsFile == null) {
			animationsFile = new ConfigHandlers(this, "animations.conf");
		}

		animationsFile.reload();
		loadAnimations();
	}

	private void initCommands() {
		SpongeCommands sc = new SpongeCommands(this);
		sc.init();
	}

	public void reload() {
		if (tManager == null) {
			tManager = new TabListManager(this);
		}

		initConfigs();

		tManager.cancelTabForAll();
		Sponge.getServer().getOnlinePlayers().forEach(tManager::loadTab);
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

	protected String makeAnim(String name) {
		for (AnimCreator ac : animations) {
			name = name.replace("%anim:" + ac.getAnimName() + "%",
					ac.getTime() > 0 ? ac.getRandomText() : ac.getFirstText());
		}

		return name;
	}

	public Set<AnimCreator> getAnimations() {
		return animations;
	}

	public ConfigHandlers getC() {
		return config;
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

	public PluginContainer getPluginContainer() {
		return pc;
	}

	public static TabList get() {
		return instance;
	}
}
