package hu.montlikadani.tablist.Sponge;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import com.google.inject.Inject;

import hu.montlikadani.tablist.AnimCreator;

import java.util.ArrayList;
import java.util.List;

@Plugin(id = "tablist", name = "TabList", version = "1.0", description = "An ultimate animated tablist", authors = "montlikadani")
public class TabList {

	private static TabList instance = null;

	@Inject
	private PluginContainer pc;

	private ConfigHandlers config, animationsFile;

	private TabListManager tManager;
	private Variables variables;

	private final List<AnimCreator> animations = new ArrayList<>();

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
	public void onReload(GameReloadEvent event) {
		reload();
	}

	private void initConfigs() {
		config = new ConfigHandlers(this, "spongeConfig.conf");
		config.createFile();
		Config.load();

		animationsFile = new ConfigHandlers(this, "animations.conf");
		animationsFile.createFile();
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

		if (!config.getConfig().getFile().exists()) {
			initConfigs();
		} else {
			Config.load();
		}

		tManager.cancelTabForAll();
		Sponge.getServer().getOnlinePlayers().forEach(tManager::loadTab);
	}

	private void loadAnimations() {
		animations.clear();

		// TODO: Load animations from file
	}

	public ConfigHandlers getConfig() {
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
