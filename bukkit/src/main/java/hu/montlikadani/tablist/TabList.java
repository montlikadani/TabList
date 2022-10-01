package hu.montlikadani.tablist;

import static hu.montlikadani.tablist.utils.Util.colorText;
import static hu.montlikadani.tablist.utils.Util.logConsole;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import hu.montlikadani.tablist.commands.Commands;
import hu.montlikadani.tablist.config.CommentedConfig;
import hu.montlikadani.tablist.config.Configuration;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.listeners.HidePlayerListener;
import hu.montlikadani.tablist.listeners.Listeners;
import hu.montlikadani.tablist.tablist.TabManager;
import hu.montlikadani.tablist.tablist.fakeplayers.FakePlayerHandler;
import hu.montlikadani.tablist.tablist.groups.Groups;
import hu.montlikadani.tablist.user.TabListPlayer;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.PluginUtils;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.UpdateDownloader;
import hu.montlikadani.tablist.utils.plugin.VaultPermission;
import hu.montlikadani.tablist.utils.stuff.Complement;
import hu.montlikadani.tablist.utils.stuff.Complement1;
import hu.montlikadani.tablist.utils.stuff.Complement2;
import hu.montlikadani.tablist.utils.task.Tasks;
import hu.montlikadani.tablist.utils.variables.Variables;

public final class TabList extends org.bukkit.plugin.java.JavaPlugin {

	private transient VaultPermission vaultPermission;
	private transient Objects objects;
	private transient Variables variables;
	private transient Groups groups;
	private transient Configuration conf;
	private transient TabManager tabManager;
	private transient FakePlayerHandler fakePlayerHandler;
	private transient Complement complement;

	private transient org.bukkit.plugin.Plugin papi;

	private transient boolean hasVault = false;

	private final Set<TextAnimation> animations = new HashSet<>(8);
	private final Set<TabListUser> users = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Override
	public void onEnable() {
		long load = System.currentTimeMillis();

		if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R1)) {
			logConsole(Level.SEVERE,
					"Your server version does not supported by this plugin! Please use 1.8+ or higher versions!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		int availableProcessors = Runtime.getRuntime().availableProcessors();
		if (availableProcessors < 3) {
			logConsole(Level.WARNING, "You're currently having " + availableProcessors
					+ " CPU processors, which is not enough to run a minecraft server with scheduled plugins including TabList.");
			logConsole(Level.WARNING,
					"You will be experiencing many lags during the server is running. Consider upgrading your CPU to at least reach 3 cores.");
		}

		verifyServerSoftware();

		conf = new Configuration(this);
		objects = new Objects(this);
		groups = new Groups(this);
		variables = new Variables(this);
		tabManager = new TabManager(this);
		fakePlayerHandler = new FakePlayerHandler(this);

		// Load static references in any way
		// This includes ReflectionUtils, ClazzContainer
		try {
			Class.forName("hu.montlikadani.tablist.tablist.TabTitle");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		hu.montlikadani.tablist.api.TabListAPI.getTPS();

		conf.loadFiles();
		variables.load();

		if (ConfigValues.isPlaceholderAPI() && (papi = getServer().getPluginManager().getPlugin("PlaceholderAPI")) != null
				&& papi.isEnabled()) {
			logConsole("Hooked " + papi.getName() + " version: " + papi.getDescription().getVersion());
		}

		hasVault = isPluginEnabled("Vault") && (vaultPermission = new VaultPermission()).getPermission() != null;

		fakePlayerHandler.load();
		loadAnimations();
		loadListeners();
		registerCommands();
		tabManager.toggleBase.loadToggledTabs();
		groups.load();

		getServer().getOnlinePlayers().forEach(this::updateAll);

		UpdateDownloader.checkFromGithub(this);
		beginDataCollection();

		if (ConfigValues.isLogConsole()) {
			getServer().getConsoleSender().sendMessage(colorText("&6[&5Tab&cList&6]&7 >&a Enabled&6 v"
					+ getDescription().getVersion() + "&a (" + (System.currentTimeMillis() - load) + "ms)"));
		}
	}

	@Override
	public void onDisable() {
		groups.cancelUpdate();
		objects.cancelTask();

		if (!users.isEmpty()) {
			for (Objects.ObjectTypes t : Objects.ObjectTypes.values()) {
				if (t == Objects.ObjectTypes.NONE) {
					continue;
				}

				for (TabListUser user : users) {
					objects.unregisterObjective(t, user);
				}
			}
		}

		tabManager.toggleBase.saveToggledTabs();
		tabManager.cancelTask();

		fakePlayerHandler.removeAllFakePlayer();

		users.forEach(tlu -> {
			tlu.getTabHandler().sendEmptyTab(tlu.getPlayer());
			tlu.setHidden(false);
		});

		conf.deleteEmptyFiles();

		HandlerList.unregisterAll(this);
		getServer().getScheduler().cancelTasks(this);
		users.clear();

		// Async tasks can't be cancelled sometimes, with this we forcedly interrupts
		// the active ones
		for (org.bukkit.scheduler.BukkitWorker worker : getServer().getScheduler().getActiveWorkers()) {
			if (equals(worker.getOwner()) && !worker.getThread().isInterrupted()) {
				worker.getThread().interrupt();
			}
		}
	}

	@Override
	public CommentedConfig getConfig() {
		return conf.getConfig();
	}

	@Override
	public void saveConfig() {
		getConfig().save();
	}

	private void verifyServerSoftware() {
		// We're using reflection to check if for example a method is exists
		// So this useless
		/*try {
			Class.forName("com.destroystokyo.paper.PaperConfig");
			isPaper = true;
		} catch (ClassNotFoundException e) {
			try {
				Class.forName("io.papermc.paper.configuration.Configuration");
				isPaper = true;
			} catch (ClassNotFoundException ex) {
				isPaper = false;
			}
		}*/

		boolean kyoriSupported = false;
		try {
			Class.forName("net.kyori.adventure.text.Component");
			Player.class.getDeclaredMethod("displayName");
			kyoriSupported = ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_16_R3);
		} catch (NoSuchMethodException | ClassNotFoundException e) {
		}

		complement = kyoriSupported ? new Complement2() : new Complement1();
	}

	private void beginDataCollection() {
		Metrics metrics = new Metrics(this, 1479);

		metrics.addCustomChart(
				new org.bstats.charts.SimplePie("using_placeholderapi", () -> Boolean.toString(ConfigValues.isPlaceholderAPI())));

		if (TabConfigValues.isEnabled()) {
			metrics.addCustomChart(
					new org.bstats.charts.SimplePie("tab_interval", () -> Integer.toString(TabConfigValues.getUpdateInterval())));
		}

		metrics.addCustomChart(
				new org.bstats.charts.SimplePie("enable_tablist", () -> Boolean.toString(TabConfigValues.isEnabled())));

		if (ConfigValues.getObjectType() != Objects.ObjectTypes.NONE) {
			metrics.addCustomChart(
					new org.bstats.charts.SimplePie("object_type", () -> ConfigValues.getObjectType().loweredName));
		}

		metrics.addCustomChart(
				new org.bstats.charts.SimplePie("enable_fake_players", () -> Boolean.toString(ConfigValues.isFakePlayers())));

		metrics.addCustomChart(
				new org.bstats.charts.SimplePie("enable_groups", () -> Boolean.toString(ConfigValues.isPrefixSuffixEnabled())));
	}

	private void registerCommands() {
		Optional.ofNullable(getCommand("tablist")).ifPresent(tl -> {
			Commands cmds = new Commands(this);

			tl.setExecutor(cmds);
			tl.setTabCompleter(cmds);
		});
	}

	void loadListeners() {
		HandlerList.unregisterAll(this);

		getServer().getPluginManager().registerEvents(new Listeners(this), this);

		if (ConfigValues.isHidePlayersFromTab()) {
			new HidePlayerListener(this);
		}

		if (isPluginEnabled("Essentials")) {
			new hu.montlikadani.tablist.listeners.resources.EssAfkStatus(this);
		}

		if (isPluginEnabled("CMI")) {
			getServer().getPluginManager().registerEvents(new hu.montlikadani.tablist.listeners.resources.CMIAfkStatus(), this);
		}

		if (PluginUtils.isPurpur()) {
			new hu.montlikadani.tablist.listeners.resources.PurpurAfkStatus(this);
		}

		if (isPluginEnabled("ProtocolLib")) {
			ProtocolPackets.onSpectatorChange();
		}
	}

	public void reload() {
		tabManager.cancelTask();
		printed = false;

		users.forEach(user -> user.getTabHandler().sendEmptyTab(user.getPlayer()));

		groups.cancelUpdate();
		fakePlayerHandler.removeAllFakePlayer();

		loadListeners();
		conf.loadFiles();
		loadAnimations();
		variables.load();
		fakePlayerHandler.load();
		groups.load();

		Objects.ObjectTypes current = ConfigValues.getObjectType();

		if (current == Objects.ObjectTypes.NONE || current == Objects.ObjectTypes.HEALTH) {
			objects.cancelTask();
		}

		getServer().getOnlinePlayers().forEach(pl -> updateAll(pl, true));
	}

	private void loadAnimations() {
		animations.clear();

		org.bukkit.configuration.ConfigurationSection section = conf.getAnimCreator().getConfigurationSection("animations");
		if (section == null) {
			return;
		}

		for (String name : section.getKeys(false)) {
			List<String> list = section.getStringList(name + ".texts");

			if (!list.isEmpty()) {
				list.replaceAll(variables::replaceMiscVariables);

				animations.add(new TextAnimation(name, list, section.getInt(name + ".interval", 200),
						section.getBoolean(name + ".random", false)));
			}
		}
	}

	public String makeAnim(String str) {
		if (str.isEmpty()) {
			return str;
		}

		StringBuilder builder = new StringBuilder(str);
		int i = 0;

		while (i < animations.size()) {
			for (TextAnimation ac : animations) {
				hu.montlikadani.tablist.Global.replace(builder, "%anim:" + ac.getName() + "%", () -> ac.getText());
			}

			i++;
		}

		return builder.toString();
	}

	public void updateAll(Player p) {
		updateAll(p, false);
	}

	void updateAll(Player player, boolean reload) {
		TabListUser user = getUser(player.getUniqueId()).orElseGet(() -> {
			TabListUser tlu = new TabListPlayer(this, player.getUniqueId());
			users.add(tlu);
			return tlu;
		});

		if (reload) { // Reset player score for integer objectives
			user.getPlayerScore().setLastScore(-1);
		}

		switch (ConfigValues.getObjectType()) {
		case PING:
		case CUSTOM:
			if (reload) {
				objects.unregisterHealthObjective(player);
			}

			if (reload || objects.isCancelled()) {
				objects.startTask();

				if (reload) {
					objects.unregisterObjective(Objects.ObjectTypes.PING, user);
					objects.unregisterObjective(Objects.ObjectTypes.CUSTOM, user);
				}
			}

			break;
		case HEALTH:
			if (reload) {
				objects.unregisterObjective(Objects.ObjectTypes.PING, user);
				objects.unregisterObjective(Objects.ObjectTypes.CUSTOM, user);
			} else {
				objects.registerHealthTab(player);
			}

			break;
		default:
			if (reload) {
				for (Objects.ObjectTypes type : Objects.ObjectTypes.values()) {
					if (type != Objects.ObjectTypes.NONE) {
						objects.unregisterObjective(type, user);
					}
				}
			}

			break;
		}

		if (ConfigValues.isFakePlayers()) {
			fakePlayerHandler.display();
		}

		tabManager.addPlayer(user);

		if (!reload) {
			groups.startTask();
		}

		if (ConfigValues.isHidePlayersFromTab()) {
			user.removeFromPlayerList();
		} else {
			user.addToPlayerList();

			if (ConfigValues.isPerWorldPlayerList()) {
				Tasks.submitSync(() -> {
					user.setHidden(true);

					if (user.isHidden()) {
						((TabListPlayer) user).getPlayerList().displayInWorld();
					}

					return 1;
				});
			} else if (user.isHidden()) {
				Tasks.submitSync(() -> {
					user.setHidden(false);
					return 1;
				});
			}
		}
	}

	public void onPlayerQuit(Player player) {
		java.util.Iterator<TabListUser> iterator = users.iterator();
		UUID playerId = player.getUniqueId();

		while (iterator.hasNext()) {
			TabListUser user = iterator.next();

			if (playerId.equals(user.getUniqueId())) {
				user.getTabHandler().sendEmptyTab(player);
				user.removeAllVisibleFakePlayer();

				groups.removePlayerGroup(user);

				objects.unregisterObjective(Objects.ObjectTypes.PING, user);
				objects.unregisterObjective(Objects.ObjectTypes.CUSTOM, user);

				iterator.remove();
				break;
			}
		}
	}

	private boolean printed = false;

	public boolean performanceIsUnderValue() {
		if (ConfigValues.getTpsPerformanceObservationValue() != -1.0
				&& hu.montlikadani.tablist.api.TabListAPI.getTPS() <= ConfigValues.getTpsPerformanceObservationValue()) {
			if (!printed) {
				getLogger().log(Level.INFO, "All {0} schedulers has been cancelled. (Low performance)", getName());
				printed = true;
			}

			return true;
		}

		return false;
	}

	public Optional<TabListUser> getUser(Player player) {
		return player == null ? Optional.empty() : getUser(player.getUniqueId());
	}

	public Optional<TabListUser> getUser(UUID uuid) {
		if (uuid != null) {
			for (TabListUser tlp : users) {
				if (uuid.equals(tlp.getUniqueId())) {
					return Optional.of(tlp);
				}
			}
		}

		return Optional.empty();
	}

	public Set<TabListUser> getUsers() {
		return users;
	}

	public boolean hasVault() {
		return hasVault;
	}

	public boolean hasPapi() {
		return papi != null && papi.isEnabled();
	}

	public Variables getPlaceholders() {
		return variables;
	}

	public FakePlayerHandler getFakePlayerHandler() {
		return fakePlayerHandler;
	}

	public TabManager getTabManager() {
		return tabManager;
	}

	public Groups getGroups() {
		return groups;
	}

	public Objects getObjects() {
		return objects;
	}

	public Configuration getConf() {
		return conf;
	}

	public VaultPermission getVaultPerm() {
		return vaultPermission;
	}

	public Complement getComplement() {
		return complement;
	}

	boolean isPluginEnabled(String name) {
		return getServer().getPluginManager().isPluginEnabled(name);
	}

	public File getFolder() {
		File dataFolder = getDataFolder();
		dataFolder.mkdirs();
		return dataFolder;
	}
}
