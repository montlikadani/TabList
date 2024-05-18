package hu.montlikadani.tablist;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import hu.montlikadani.tablist.api.TabListAPI;
import hu.montlikadani.tablist.tablist.TabToggleBase;
import hu.montlikadani.tablist.utils.Util;
import hu.montlikadani.tablist.utils.scheduler.TLScheduler;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import hu.montlikadani.tablist.commands.Commands;
import hu.montlikadani.tablist.config.CommentedConfig;
import hu.montlikadani.tablist.config.Configuration;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.listeners.Listeners;
import hu.montlikadani.tablist.packets.PacketNM;
import hu.montlikadani.tablist.tablist.TabManager;
import hu.montlikadani.tablist.tablist.fakeplayers.FakePlayerHandler;
import hu.montlikadani.tablist.tablist.groups.Groups;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.UpdateDownloader;
import hu.montlikadani.tablist.utils.plugin.PermissionService;
import hu.montlikadani.tablist.utils.stuff.Complement;
import hu.montlikadani.tablist.utils.stuff.Complement1;
import hu.montlikadani.tablist.utils.stuff.Complement2;
import hu.montlikadani.tablist.utils.variables.Variables;

public final class TabList extends org.bukkit.plugin.java.JavaPlugin {

	private transient PermissionService permissionService;
	private transient Objects objects;
	private transient Variables variables;
	private transient Groups groups;
	private transient Configuration conf;
	private transient TabManager tabManager;
	private transient FakePlayerHandler fakePlayerHandler;
	private transient Complement complement;

	private transient org.bukkit.plugin.Plugin papi;

	private transient boolean hasPermissionService;
	private transient boolean isFoliaServer;

	private final Set<TextAnimation> animations = new HashSet<>(8);
	private final Set<TabListUser> users = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private List<Class<?>> packetClasses;

	@Override
	public void onEnable() {
		long load = System.currentTimeMillis();

		if (ServerVersion.current() == null) {
			getLogger().log(Level.SEVERE, "Your server version does not supported " + getServer().getBukkitVersion());
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		verifyServerSoftware();

		if (ConfigValues.isPlaceholderAPI()) {
			papi = getServer().getPluginManager().getPlugin("PlaceholderAPI");
		}

		conf = new Configuration(this);
		groups = new Groups(this);
		variables = new Variables(this);
		tabManager = new TabManager(this);
		fakePlayerHandler = new FakePlayerHandler(this);

		// Load static references
		try {
			Class.forName("hu.montlikadani.tablist.packets.PacketNM");
		} catch (ClassNotFoundException ignored) {
		}
		TabListAPI.getTPS();

		conf.loadFiles();
		variables.load();
		loadPacketListener();

		if (hasPapi()) {
			Util.consolePrint(Level.INFO, this, "Found {0} version: {1}", papi.getName(), papi.getDescription().getVersion());
		}

		hasPermissionService = isPluginEnabled("Vault") && (permissionService = new PermissionService()).getPermission() != null;

		fakePlayerHandler.load();
		loadAnimations();
		loadListeners();
		registerCommands();
		groups.load();

		getServer().getOnlinePlayers().forEach(this::getOrLoadUser);
		tabManager.toggleBase.load(this);
		getServer().getOnlinePlayers().forEach(this::updateAll);

		UpdateDownloader.checkFromGithub(this);
		beginDataCollection();

		Util.consolePrint(Level.INFO, this, "v{0} on {1} ({2}ms)", getDescription().getVersion(),
				ServerVersion.current().name(), System.currentTimeMillis() - load);
	}

	@Override
	public void onDisable() {
		groups.cancelUpdate();
		tabManager.cancelTask();

		if (objects != null) {
			objects.cancelTask();
		}

		if (isFoliaServer) {
			getServer().getAsyncScheduler().cancelTasks(this);
			getServer().getGlobalRegionScheduler().cancelTasks(this);
		} else {
			getServer().getScheduler().cancelTasks(this);

			// Async tasks can't be cancelled sometimes, with this the active threads will be interrupted
			for (org.bukkit.scheduler.BukkitWorker worker : getServer().getScheduler().getActiveWorkers()) {
				if (equals(worker.getOwner())) {
					worker.getThread().interrupt();
				}
			}
		}

		if (!users.isEmpty()) {
			if (objects != null) {
				for (Objects.ObjectTypes type : Objects.ObjectTypes.values()) {
					if (type == Objects.ObjectTypes.NONE) {
						continue;
					}

					for (TabListUser user : users) {
						objects.unregisterObjective(type, user);
					}
				}
			}

			users.forEach(tlu -> {
				tlu.getTabHandler().sendEmptyTab(tlu.getPlayer());
				tlu.setHidden(false);
			});
		}

		tabManager.toggleBase.save(this);
		fakePlayerHandler.removeAllFakePlayer();
		HandlerList.unregisterAll(this);
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
		try {
			Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
			isFoliaServer = true;
		} catch (ClassNotFoundException ignored) {
		}

		try {
			Class.forName("net.kyori.adventure.text.Component");
			Player.class.getDeclaredMethod("displayName");
			Player.class.getDeclaredMethod("playerListName"); // Extra check

			complement = new Complement2();
		} catch (NoSuchMethodException | ClassNotFoundException e) {
			complement = new Complement1();
		}
	}

	private void beginDataCollection() {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(new File(getDataFolder().getParentFile(),
				"bStats"), "config.yml"));

		if (!config.getBoolean("enabled", true)) {
			return;
		}

		Metrics metrics = new Metrics(this, 1479);

		metrics.addCustomChart(new org.bstats.charts.SimplePie("using_placeholderapi", () -> Boolean.toString(ConfigValues.isPlaceholderAPI())));

		if (TabConfigValues.isEnabled()) {
			metrics.addCustomChart(new org.bstats.charts.SimplePie("tab_interval", () -> Integer.toString(TabConfigValues.getUpdateInterval())));
		}

		metrics.addCustomChart(new org.bstats.charts.SimplePie("enable_tablist", () -> Boolean.toString(TabConfigValues.isEnabled())));

		if (ConfigValues.getObjectType() != Objects.ObjectTypes.NONE) {
			metrics.addCustomChart(new org.bstats.charts.SimplePie("object_type",
					() -> ConfigValues.getObjectType().name().toLowerCase(java.util.Locale.ENGLISH)));
		}

		metrics.addCustomChart(new org.bstats.charts.SimplePie("enable_fake_players", () -> Boolean.toString(ConfigValues.isFakePlayers())));

		metrics.addCustomChart(new org.bstats.charts.SimplePie("enable_groups", () -> Boolean.toString(ConfigValues.isPrefixSuffixEnabled())));
	}

	private void registerCommands() {
		Optional.ofNullable(getCommand("tablist")).ifPresent(tl -> {
			Commands commands = new Commands(this);

			tl.setExecutor(commands);
			tl.setTabCompleter(commands);
		});
	}

	private void loadPacketListener() {
		if (!ConfigValues.isRemoveGrayColorFromTabInSpec() && !ConfigValues.isPrefixSuffixEnabled()) {
			getServer().getOnlinePlayers().forEach(PacketNM.NMS_PACKET::removePlayerChannelListener);
			packetClasses = null;
			return;
		}

		packetClasses = new java.util.ArrayList<>();

		if (ConfigValues.isPrefixSuffixEnabled()) {
			try {
				packetClasses.add(Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam"));
			} catch (ClassNotFoundException ex) {
				try {
					packetClasses.add(Class.forName("net.minecraft.server." + Util.legacyNmsVersion() + ".PacketPlayOutScoreboardTeam"));
				} catch (ClassNotFoundException ignored) {
				}
			}
		}

		if (ConfigValues.isRemoveGrayColorFromTabInSpec()) {
			try {
				packetClasses.add(Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket"));
			} catch (ClassNotFoundException e) {
				try {
					packetClasses.add(Class.forName("net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo"));
				} catch (ClassNotFoundException ex) {
					try {
						packetClasses.add(Class.forName("net.minecraft.server." + Util.legacyNmsVersion() + ".PacketPlayOutPlayerInfo"));
					} catch (ClassNotFoundException ignored) {
					}
				}
			}
		}

		if (packetClasses.isEmpty()) {
			packetClasses = null;
		}
	}

	void loadListeners() {
		HandlerList.unregisterAll(this);

		getServer().getPluginManager().registerEvents(new Listeners(this), this);

		if (ConfigValues.isAfkStatusEnabled() || ConfigValues.isHidePlayerFromTabAfk()) {
			if (isPluginEnabled("Essentials")) {
				new hu.montlikadani.tablist.listeners.resources.EssAfkStatus(this);
			}

			if (isPluginEnabled("CMI")) {
				getServer().getPluginManager().registerEvents(new hu.montlikadani.tablist.listeners.resources.CMIAfkStatus(this), this);
			}

			try {
				Class<?> cl = Class.forName("org.purpurmc.purpur.event.PlayerAFKEvent");
				new hu.montlikadani.tablist.listeners.resources.PurpurAfkStatus(this, cl);
			} catch (ClassNotFoundException ignored) {
			}
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
		loadPacketListener();
		variables.load();
		fakePlayerHandler.load();
		groups.load();

		if (objects != null) {
			objects.load();
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

	/**
	 * Replaces all the animation variables in the specified string.
	 * 
	 * @param str the {@link String} where to replace the variables
	 * @return the {@link String} including the animations
	 */
	public String makeAnim(String str) {
		if (str.isEmpty()) {
			return str;
		}

		StringBuilder builder = new StringBuilder(str);
		int i = 0;

		while (i < animations.size()) {
			for (TextAnimation ac : animations) {
				hu.montlikadani.tablist.Global.replace(builder, "%anim:" + ac.name + "%", ac::next);
			}

			i++;
		}

		return builder.toString();
	}

	private TabListUser getOrLoadUser(Player player) {
		return getUser(player.getUniqueId()).orElseGet(() -> {
			TabListUser tlu = new TabListUser(this, player.getUniqueId());

			if (TabToggleBase.TEMPORAL_PLAYER_CACHE.remove(tlu.getUniqueId())) {
				tlu.setTabVisibility(false);
			}

			users.add(tlu);
			return tlu;
		});
	}

	public void updateAll(Player p) {
		updateAll(p, false);
	}

	void updateAll(Player player, boolean reload) {
		TabListUser user = getOrLoadUser(player);

		if (objects == null && ConfigValues.getObjectType() != Objects.ObjectTypes.NONE) {
			(objects = new Objects(this)).load();
		}

		if (objects != null) {
			if (reload) {
				objects.unregisterObjective(Objects.ObjectTypes.PING, user);
				objects.unregisterObjective(Objects.ObjectTypes.CUSTOM, user);

				// Reset player score for integer objectives
				hu.montlikadani.tablist.user.PlayerScore playerScore = user.getPlayerScore(true);

				if (playerScore != null) {
					playerScore.setLastScore(-1);
				}
			}

			objects.load(player);
		}

		if (ConfigValues.isFakePlayers()) {
			fakePlayerHandler.display();
		}

		tabManager.load(user);

		if (!reload) {
			groups.startTask();
		}

		if (packetClasses != null) {
			PacketNM.NMS_PACKET.addPlayerChannelListener(player, packetClasses);
		}

		if (ConfigValues.isPerWorldPlayerList()) {
			newTLScheduler().submitSync(() -> {
				user.setHidden(true);

				if (user.isHidden()) {
					user.getPlayerList().displayInWorld();
				}
			});
		} else if (user.isHidden()) {
			newTLScheduler().submitSync(() -> user.setHidden(false));
		}
	}

	public void onPlayerQuit(Player player) {
		java.util.Iterator<TabListUser> iterator = users.iterator();
		UUID playerId = player.getUniqueId();

		while (iterator.hasNext()) {
			TabListUser user = iterator.next();

			if (playerId.equals(user.getUniqueId())) {
				if (user.isTabVisible()) {
					user.getTabHandler().sendEmptyTab(player);
				} else {
					TabToggleBase.TEMPORAL_PLAYER_CACHE.add(user.getUniqueId());
				}

				groups.removePlayerGroup(user);

				if (objects != null && ConfigValues.getObjectType() != Objects.ObjectTypes.NONE) {
					objects.unregisterObjective(ConfigValues.getObjectType(), user);
				}

				iterator.remove();
				break;
			}
		}

		PacketNM.NMS_PACKET.removePlayerChannelListener(player);
	}

	private boolean printed;

	public boolean tpsIsUnderValue() {
		double value = ConfigValues.getTpsObservationValue();

		if (value != -1.0 && TabListAPI.getTPS()[0] <= value) {
			if (!printed) {
				Util.consolePrint(Level.INFO, this, "All {0} schedulers has been terminated. (Low tps)", getName());
				printed = true;
			}

			return true;
		}

		return false;
	}

	/**
	 * @param player {@link Player}
	 * @return {@link TabListUser} if present, otherwise {@link Optional#empty()}
	 */
	public Optional<TabListUser> getUser(Player player) {
		return player == null ? Optional.empty() : getUser(player.getUniqueId());
	}

	/**
	 * @param uuid {@link UUID} of specific player
	 * @return {@link TabListUser} if present, otherwise {@link Optional#empty()}
	 */
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

	public TLScheduler newTLScheduler() {
		return isFoliaServer ? new hu.montlikadani.tablist.utils.scheduler.FoliaScheduler(this)
				: new hu.montlikadani.tablist.utils.scheduler.BukkitScheduler(this);
	}

	/**
	 * @return the users whose joined to the server and the plugin recognised.
	 */
	public Set<TabListUser> getUsers() {
		return users;
	}

	/**
	 * @return true if there is a permission service installed
	 */
	public boolean hasPermissionService() {
		return hasPermissionService;
	}

	/**
	 * @return true if the PlaceholderAPI plugin is enabled
	 */
	public boolean hasPapi() {
		return papi != null && papi.isEnabled();
	}

	public boolean isFoliaServer() {
		return isFoliaServer;
	}

	public Variables getPlaceholders() {
		return variables;
	}

	public FakePlayerHandler getFakePlayerHandler() {
		return fakePlayerHandler;
	}

	public Groups getGroups() {
		return groups;
	}

	public Configuration getConf() {
		return conf;
	}

	public PermissionService getPermissionService() {
		return permissionService;
	}

	public Complement getComplement() {
		return complement;
	}

	boolean isPluginEnabled(String name) {
		return getServer().getPluginManager().isPluginEnabled(name);
	}
}
