package hu.montlikadani.tablist.bukkit;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.tablist.bukkit.utils.Util.logConsole;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scoreboard.Scoreboard;

import com.google.common.reflect.TypeToken;

import hu.montlikadani.tablist.TextAnimation;
import hu.montlikadani.tablist.bukkit.commands.Commands;
import hu.montlikadani.tablist.bukkit.config.CommentedConfig;
import hu.montlikadani.tablist.bukkit.config.Configuration;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.bukkit.listeners.Listeners;
import hu.montlikadani.tablist.bukkit.listeners.plugins.CMIAfkStatus;
import hu.montlikadani.tablist.bukkit.listeners.plugins.EssAfkStatus;
import hu.montlikadani.tablist.bukkit.tablist.TabManager;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.FakePlayerHandler;
import hu.montlikadani.tablist.bukkit.tablist.groups.Groups;
import hu.montlikadani.tablist.bukkit.user.TabListPlayer;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.UpdateDownloader;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.plugin.VaultPermission;
import hu.montlikadani.tablist.bukkit.utils.stuff.Complement;
import hu.montlikadani.tablist.bukkit.utils.stuff.Complement1;
import hu.montlikadani.tablist.bukkit.utils.stuff.Complement2;
import hu.montlikadani.tablist.bukkit.utils.task.DelayedPermissionCheck;
import hu.montlikadani.tablist.bukkit.utils.task.Tasks;
import hu.montlikadani.tablist.bukkit.utils.variables.Variables;

public final class TabList extends org.bukkit.plugin.java.JavaPlugin {

	private VaultPermission vaultPermission;
	private Objects objects;
	private Variables variables;
	private Groups groups;
	private Configuration conf;
	private TabManager tabManager;
	private FakePlayerHandler fakePlayerHandler;
	private Complement complement;

	private boolean isPaper = false, hasVault = false;

	private final Set<TextAnimation> animations = new HashSet<>();
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

		verifyServerSoftware();

		conf = new Configuration(this);
		objects = new Objects(this);
		groups = new Groups(this);
		variables = new Variables(this);
		tabManager = new TabManager(this);
		fakePlayerHandler = new FakePlayerHandler(this);

		conf.loadFiles();
		variables.load();

		if (ConfigValues.isPlaceholderAPI()) {
			org.bukkit.plugin.Plugin papi = getServer().getPluginManager().getPlugin("PlaceholderAPI");

			if (papi != null && papi.isEnabled()) {
				logConsole("Hooked " + papi.getName() + " version: " + papi.getDescription().getVersion());
			}
		}

		hasVault = initVaultPerm();

		fakePlayerHandler.load();
		loadAnimations();
		loadListeners();
		registerCommands();
		tabManager.getToggleBase().loadToggledTabs();
		groups.load();

		getServer().getOnlinePlayers().forEach(this::updateAll);

		UpdateDownloader.checkFromGithub(getServer().getConsoleSender());

		beginDataCollection();

		if (ConfigValues.isLogConsole()) {
			Util.sendMsg(getServer().getConsoleSender(), colorMsg("&6[&5Tab&cList&6]&7 >&a Enabled&6 v"
					+ getDescription().getVersion() + "&a (" + (System.currentTimeMillis() - load) + "ms)"));
		}
	}

	@Override
	public void onDisable() {
		groups.cancelUpdate();
		objects.cancelTask();
		objects.unregisterObjectivesForEveryone();

		tabManager.getToggleBase().saveToggledTabs();
		tabManager.removeAll();

		fakePlayerHandler.removeAllFakePlayer();

		users.forEach(tlu -> tlu.setHidden(false));

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
		try {
			Class.forName("com.destroystokyo.paper.PaperConfig");
			isPaper = true;
		} catch (ClassNotFoundException e) {
			isPaper = false;
		}

		boolean kyoriSupported = false;
		try {
			Class.forName("net.kyori.adventure.text.Component");
			Player.class.getDeclaredMethod("displayName");
			kyoriSupported = true;
		} catch (NoSuchMethodException | ClassNotFoundException e) {
		}

		complement = (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_16_R3) && kyoriSupported)
				? new Complement2()
				: new Complement1();
	}

	private void beginDataCollection() {
		Metrics metrics = new Metrics(this, 1479);

		metrics.addCustomChart(new org.bstats.charts.SimplePie("using_placeholderapi",
				() -> Boolean.toString(ConfigValues.isPlaceholderAPI())));

		if (TabConfigValues.isEnabled()) {
			metrics.addCustomChart(new org.bstats.charts.SimplePie("tab_interval",
					() -> Integer.toString(TabConfigValues.getUpdateInterval())));
		}

		metrics.addCustomChart(
				new org.bstats.charts.SimplePie("enable_tablist", () -> Boolean.toString(TabConfigValues.isEnabled())));

		if (ConfigValues.isTablistObjectiveEnabled()) {
			metrics.addCustomChart(new org.bstats.charts.SimplePie("object_type",
					ConfigValues.getObjectType().toString()::toLowerCase));
		}

		metrics.addCustomChart(new org.bstats.charts.SimplePie("enable_fake_players",
				() -> Boolean.toString(ConfigValues.isFakePlayers())));

		metrics.addCustomChart(new org.bstats.charts.SimplePie("enable_groups",
				() -> Boolean.toString(ConfigValues.isPrefixSuffixEnabled())));
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

		if (isPluginEnabled("Essentials")) {
			getServer().getPluginManager().registerEvents(new EssAfkStatus(), this);
		}

		if (isPluginEnabled("CMI")) {
			getServer().getPluginManager().registerEvents(new CMIAfkStatus(), this);
		}

		if (isPluginEnabled("ProtocolLib")) {
			ProtocolPackets.onSpectatorChange();
		}
	}

	public void reload() {
		tabManager.removeAll();
		groups.cancelUpdate();
		fakePlayerHandler.removeAllFakePlayer();
		DelayedPermissionCheck.clear();

		loadListeners();
		conf.loadFiles();
		loadAnimations();
		variables.load();
		fakePlayerHandler.load();
		groups.load();

		if (!ConfigValues.isTablistObjectiveEnabled()) {
			objects.cancelTask();
		}

		getServer().getOnlinePlayers().forEach(pl -> updateAll(pl, true));
	}

	private void loadAnimations() {
		animations.clear();

		org.bukkit.configuration.ConfigurationSection section = conf.getAnimCreator()
				.getConfigurationSection("animations");
		if (section == null) {
			return;
		}

		for (String name : section.getKeys(false)) {
			List<String> list = section.getStringList(name + ".texts");

			if (!list.isEmpty()) {
				animations.add(new TextAnimation(name, list, section.getInt(name + ".interval", 200),
						section.getBoolean(name + ".random")));
			}
		}
	}

	private boolean initVaultPerm() {
		return isPluginEnabled("Vault") && (vaultPermission = new VaultPermission()).getPermission() != null;
	}

	public String makeAnim(String name) {
		if (name.isEmpty()) {
			return name;
		}

		int a = 0; // Make sure we're not generates infinite loop

		while (a < 100 && !animations.isEmpty() && name.indexOf("%anim:") >= 0) { // when using multiple animations
			for (TextAnimation ac : animations) {
				name = StringUtils.replace(name, "%anim:" + ac.getName() + "%",
						ac.getTime() > 0 ? ac.getText() : ac.getTexts()[0]);
			}

			a++;
		}

		return name;
	}

	public void updateAll(Player p) {
		updateAll(p, false);
	}

	void updateAll(final Player player, boolean reload) {
		TabListUser user = getUser(player.getUniqueId()).orElseGet(() -> {
			TabListUser tlu = new TabListPlayer(this, player.getUniqueId());
			users.add(tlu);
			return tlu;
		});

		if (!ConfigValues.isTablistObjectiveEnabled()) {
			if (reload) {
				Scoreboard board = player.getScoreboard();

				for (Objects.ObjectTypes type : Objects.ObjectTypes.values()) {
					objects.unregisterObjective(objects.getObject(board, type));
				}
			}
		} else {
			switch (ConfigValues.getObjectType()) {
			case PING:
			case CUSTOM:
				if (reload) {
					objects.unregisterObjective(objects.getObject(player.getScoreboard(), Objects.ObjectTypes.HEALTH));
				}

				if (objects.isCancelled()) {
					objects.startTask();
				}

				break;
			case HEALTH:
				if (reload) {
					objects.cancelTask();

					Scoreboard board = player.getScoreboard();

					objects.unregisterObjective(objects.getObject(board, Objects.ObjectTypes.PING));
					objects.unregisterObjective(objects.getObject(board, Objects.ObjectTypes.CUSTOM));
				} else {
					objects.registerHealthTab(player);
				}

				break;
			default:
				break;
			}
		}

		if (ConfigValues.isFakePlayers()) {
			fakePlayerHandler.display();
		}

		tabManager.addPlayer(user);
		groups.startTask();

		if (ConfigValues.isHidePlayersFromTab()) {
			user.removeFromPlayerList();
		} else {
			user.addToPlayerList();

			if (ConfigValues.isPerWorldPlayerList()) {
				Tasks.submitSync(() -> {
					user.setHidden(true);

					if (user.isHidden()) {
						((TabListPlayer) user).getPlayerList().showForWorld();
					}

					return 1;
				});
			} else if (user.isHidden()) {
				Tasks.submitSync(() -> {
					TabListPlayer tlp = (TabListPlayer) user;

					tlp.getPlayerList().showEveryone();
					tlp.removeCache();
					return 1;
				});
			}
		}
	}

	public void onPlayerQuit(Player player) {
		users.removeIf(user -> {
			user.getTabHandler().sendEmptyTab(player);
			groups.removePlayerGroup(user);

			Scoreboard board = player.getScoreboard();

			objects.unregisterObjective(objects.getObject(board, Objects.ObjectTypes.PING));
			objects.unregisterObjective(objects.getObject(board, Objects.ObjectTypes.CUSTOM));
			return player.getUniqueId().equals(user.getUniqueId());
		});
	}

	public String getMsg(String key, Object... placeholders) {
		return getMsg(TypeToken.of(String.class), key, placeholders);
	}

	// TODO optimise or get rid from this entirely
	@SuppressWarnings("unchecked")
	public <T> T getMsg(TypeToken<T> type, String key, Object... placeholders) {
		if (key == null) {
			return (T) "null";
		}

		Class<? super T> rawType = type.getRawType();

		if (rawType.isAssignableFrom(String.class)) {
			String text = conf.getMessages().getString(key, "");

			if (text.isEmpty()) {
				return (T) "";
			}

			String msg = colorMsg(text);

			for (int i = 0; i < placeholders.length; i++) {
				if (placeholders.length >= i + 2) {
					msg = msg.replace(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
				}

				i++;
			}

			return (T) msg;
		}

		if (rawType.isAssignableFrom(List.class)) {
			List<String> list = conf.getMessages().getStringList(key);

			for (int a = 0; a < list.size(); a++) {
				String one = colorMsg(list.get(a));

				for (int i = 0; i < placeholders.length; i++) {
					if (placeholders.length >= i + 2) {
						one = StringUtils.replace(one, String.valueOf(placeholders[i]),
								String.valueOf(placeholders[i + 1]));
					}

					i++;
				}

				list.set(a, one);
			}

			return (T) list;
		}

		return (T) "no msg";
	}

	public Optional<TabListUser> getUser(Player player) {
		return player == null ? Optional.empty() : getUser(player.getUniqueId());
	}

	public Optional<TabListUser> getUser(UUID uuid) {
		if (uuid == null) {
			return Optional.empty();
		}

		for (TabListUser tlp : users) {
			if (uuid.equals(tlp.getUniqueId())) {
				return Optional.of(tlp);
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

	public boolean isPaper() {
		return isPaper;
	}

	public Variables getPlaceholders() {
		return variables;
	}

	public Set<TextAnimation> getAnimations() {
		return animations;
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

	public boolean isPluginEnabled(String name) {
		return getServer().getPluginManager().isPluginEnabled(name);
	}

	public File getFolder() {
		File dataFolder = getDataFolder();
		dataFolder.mkdirs();
		return dataFolder;
	}
}
