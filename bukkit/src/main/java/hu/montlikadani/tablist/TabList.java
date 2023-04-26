package hu.montlikadani.tablist;

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

import hu.montlikadani.tablist.tablist.TabToggleBase;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import hu.montlikadani.tablist.commands.Commands;
import hu.montlikadani.tablist.config.CommentedConfig;
import hu.montlikadani.tablist.config.Configuration;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.listeners.HidePlayerListener;
import hu.montlikadani.tablist.listeners.Listeners;
import hu.montlikadani.tablist.packets.PacketNM;
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
	private List<Class<?>> packetClasses;

	@Override
	public void onEnable() {
		long load = System.currentTimeMillis();

		if (ServerVersion.getCurrent() == null) {
			logConsole(Level.SEVERE, "Your server version does not supported " + ServerVersion.getArrayVersion()[3]);
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		verifyServerSoftware();

		conf = new Configuration(this);
		objects = new Objects(this);
		groups = new Groups(this);
		variables = new Variables(this);
		tabManager = new TabManager(this);
		fakePlayerHandler = new FakePlayerHandler(this);

		// Load static references
		try {
			Class.forName("hu.montlikadani.tablist.packets.PacketNM");
		} catch (ClassNotFoundException e) {
		}
		hu.montlikadani.tablist.api.TabListAPI.getTPS();

		conf.loadFiles();
		variables.load();
		loadPacketListener();

		if (ConfigValues.isPlaceholderAPI() && (papi = getServer().getPluginManager().getPlugin("PlaceholderAPI")) != null && papi.isEnabled()) {
			logConsole("Hooked " + papi.getName() + " version: " + papi.getDescription().getVersion());
		}

		hasVault = isPluginEnabled("Vault") && (vaultPermission = new VaultPermission()).getPermission() != null;

		fakePlayerHandler.load();
		loadAnimations();
		loadListeners();
		registerCommands();
		groups.load();

		getServer().getOnlinePlayers().forEach(this::getOrLoadUser);
		tabManager.toggleBase.loadToggledTabs(this);
		getServer().getOnlinePlayers().forEach(this::updateAll);

		UpdateDownloader.checkFromGithub(this);
		beginDataCollection();

		if (ConfigValues.isLogConsole()) {
			getServer().getConsoleSender().sendMessage(hu.montlikadani.tablist.utils.Util
					.colorText("&6[&5Tab&cList&6]&7 >&a Enabled&6 v" + getDescription().getVersion() + "&a (" + (System.currentTimeMillis() - load) + "ms)"));
		}
	}

	@Override
	public void onDisable() {
		groups.cancelUpdate();
		objects.cancelTask();
		tabManager.cancelTask();

		getServer().getScheduler().cancelTasks(this);

		// Async tasks can't be cancelled sometimes, with this we forcedly interrupts the active ones
		for (org.bukkit.scheduler.BukkitWorker worker : getServer().getScheduler().getActiveWorkers()) {
			if (equals(worker.getOwner())) {
				worker.getThread().interrupt();
			}
		}

		if (!users.isEmpty()) {
			for (Objects.ObjectTypes t : Objects.ObjectTypes.values()) {
				if (t == Objects.ObjectTypes.NONE) {
					continue;
				}

				for (TabListUser user : users) {
					objects.unregisterObjective(t, user);
				}
			}

			users.forEach(tlu -> {
				tlu.getTabHandler().sendEmptyTab(tlu.getPlayer());
				tlu.setHidden(false);
			});
		}

		tabManager.toggleBase.saveToggledTabs(this);
		fakePlayerHandler.removeAllFakePlayer();

		conf.deleteEmptyFiles();
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
			Class.forName("net.kyori.adventure.text.Component");
			Player.class.getDeclaredMethod("displayName");

			complement = ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_16_R3) ? new Complement2() : new Complement1();
		} catch (NoSuchMethodException | ClassNotFoundException e) {
			complement = new Complement1();
		}
	}

	private void beginDataCollection() {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(new File(getDataFolder().getParentFile(), "bStats"), "config.yml"));

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
			metrics.addCustomChart(new org.bstats.charts.SimplePie("object_type", () -> ConfigValues.getObjectType().loweredName));
		}

		metrics.addCustomChart(new org.bstats.charts.SimplePie("enable_fake_players", () -> Boolean.toString(ConfigValues.isFakePlayers())));

		metrics.addCustomChart(new org.bstats.charts.SimplePie("enable_groups", () -> Boolean.toString(ConfigValues.isPrefixSuffixEnabled())));
	}

	private void registerCommands() {
		Optional.ofNullable(getCommand("tablist")).ifPresent(tl -> {
			Commands cmds = new Commands(this);

			tl.setExecutor(cmds);
			tl.setTabCompleter(cmds);
		});
	}

	private void loadPacketListener() {
		if (!ConfigValues.isRemoveGrayColorFromTabInSpec() && !ConfigValues.isHidePlayersFromTab()) {
			getServer().getOnlinePlayers().forEach(PacketNM.NMS_PACKET::removePlayerChannelListener);
			packetClasses = null;
			return;
		}

		packetClasses = new java.util.ArrayList<>(1);

		try {
			packetClasses.add(Class.forName("net.minecraft.network.protocol.game." + (ConfigValues.isHidePlayersFromTab() ? "ClientboundPlayerChatPacket"
					: "ClientboundPlayerInfoUpdatePacket")));
		} catch (ClassNotFoundException e) {
			try {
				packetClasses.add(Class.forName("net.minecraft.network.protocol.game." + (ConfigValues.isHidePlayersFromTab() ? "ClientboundPlayerChatPacket"
						: "PacketPlayOutPlayerInfo")));
			} catch (ClassNotFoundException ex) {
				try {
					packetClasses.add(Class.forName("net.minecraft.server." + ServerVersion.getArrayVersion()[3] + ".PacketPlayOutPlayerInfo"));
				} catch (ClassNotFoundException c) {
					packetClasses = null;
				}
			}
		}
	}

	void loadListeners() {
		HandlerList.unregisterAll(this);

		getServer().getPluginManager().registerEvents(new Listeners(this), this);

		if (ConfigValues.isHidePlayersFromTab()) {
			new HidePlayerListener(this);
		}

		if (ConfigValues.isAfkStatusEnabled() || ConfigValues.isHidePlayerFromTabAfk()) {
			if (isPluginEnabled("Essentials")) {
				new hu.montlikadani.tablist.listeners.resources.EssAfkStatus(this);
			}

			if (isPluginEnabled("CMI")) {
				getServer().getPluginManager().registerEvents(new hu.montlikadani.tablist.listeners.resources.CMIAfkStatus(this), this);
			}

			if (PluginUtils.isPurpur()) {
				try {
					Class.forName("org.purpurmc.purpur.event.PlayerAFKEvent");
					new hu.montlikadani.tablist.listeners.resources.PurpurAfkStatus(this);
				} catch (ClassNotFoundException e) {
				}
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

				animations.add(new TextAnimation(name, list, section.getInt(name + ".interval", 200), section.getBoolean(name + ".random", false)));
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
				hu.montlikadani.tablist.Global.replace(builder, "%anim:" + ac.name + "%", ac::getText);
			}

			i++;
		}

		return builder.toString();
	}

	private TabListUser getOrLoadUser(Player player) {
		return getUser(player.getUniqueId()).orElseGet(() -> {
			TabListUser tlu = new TabListPlayer(this, player.getUniqueId());

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

		tabManager.load(user);

		if (!reload) {
			groups.startTask();
		}

		if (packetClasses != null) {
			PacketNM.NMS_PACKET.addPlayerChannelListener(player, packetClasses);
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
				if (user.isTabVisible()) {
					user.getTabHandler().sendEmptyTab(player);
				} else {
					TabToggleBase.TEMPORAL_PLAYER_CACHE.add(user.getUniqueId());
				}

				groups.removePlayerGroup(user);
				objects.unregisterObjective(ConfigValues.getObjectType(), user);

				iterator.remove();
				break;
			}
		}

		PacketNM.NMS_PACKET.removePlayerChannelListener(player);
	}

	private boolean printed = false;

	public boolean performanceIsUnderValue() {
		if (ConfigValues.getTpsPerformanceObservationValue() != -1.0 && hu.montlikadani.tablist.api.TabListAPI.getTPS() <= ConfigValues.getTpsPerformanceObservationValue()) {
			if (!printed) {
				getLogger().log(Level.INFO, "All {0} schedulers has been cancelled. (Low performance)", getName());
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

	/**
	 * @return the users whose joined to the server and the plugin recognised.
	 */
	public Set<TabListUser> getUsers() {
		return users;
	}

	/**
	 * @return true if the Vault resource is installed and recognised by this plugin.
	 */
	public boolean hasVault() {
		return hasVault;
	}

	/**
	 * @return true if the PlaceholderAPI plugin is enabled
	 */
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
}
