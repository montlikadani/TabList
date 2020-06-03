package hu.montlikadani.tablist.bukkit.tablist;

import static hu.montlikadani.tablist.bukkit.utils.Util.logConsole;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.tablist.tabEntry.Rows;
import hu.montlikadani.tablist.bukkit.tablist.tabEntry.TabEntry;
import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;
import hu.montlikadani.tablist.bukkit.utils.Variables;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public class TabHandler implements ITabHandler {

	private final TabList plugin;
	private final Rows[] rows = new Rows[80];

	private Player player;

	private List<String> header;
	private List<String> footer;

	private BukkitTask task;

	public TabHandler(TabList plugin, Player player) {
		this(plugin, player, null, null);
	}

	public TabHandler(TabList plugin, Player player, List<String> header, List<String> footer) {
		this.plugin = plugin;
		this.player = player;
		this.header = header;
		this.footer = footer;
	}

	@Override
	public Player getPlayer() {
		return player;
	}

	@Override
	public List<String> getHeader() {
		return header;
	}

	@Override
	public void setHeader(List<String> header) {
		this.header = header;
	}

	@Override
	public List<String> getFooter() {
		return footer;
	}

	@Override
	public void setFooter(List<String> footer) {
		this.footer = footer;
	}

	@Override
	public BukkitTask getTask() {
		return task;
	}

	public void updateEntries() {
		if (player == null || !player.isOnline()) {
			return;
		}

		final FileConfiguration c = plugin.getTabC();
		if (c == null || !c.getBoolean("enabled")) {
			return;
		}

		try {
			Class<?> packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo");
			Constructor<?> infoData = null;
			c: for (Class<?> clazz : packetPlayOutPlayerInfo.getClasses()) {
				for (Constructor<?> co : (Constructor[]) clazz.getConstructors()) {
					if (co.getParameterCount() == 5) {
						infoData = co;
						break c;
					}
				}
			}

			if (infoData == null)
				return;

			infoData.setAccessible(true);

			Class<?> server = ReflectionUtils.Classes.getMinecraftServer();
			Object serverIns = ReflectionUtils.Classes.getServer(server);
			GameProfile profile = new GameProfile(UUID.randomUUID(), player.getName());
			Class<?> manager = ReflectionUtils.getNMSClass("PlayerInteractManager");

			Object managerIns = null;
			Object world;
			if (Version.isCurrentEqualOrHigher(Version.v1_14_R1)) {
				world = ReflectionUtils.getHandle(player.getWorld());
				managerIns = manager.getConstructor(world.getClass()).newInstance(world);
			} else if (Version.isCurrentEqual(Version.v1_13_R1) || Version.isCurrentEqual(Version.v1_13_R2)) {
				world = ReflectionUtils.getHandle(player.getWorld());
			} else {
				world = server.getDeclaredMethod("getWorldServer", int.class).invoke(serverIns, 0);
			}

			if (managerIns == null) {
				managerIns = manager.getConstructors()[0].newInstance(world);
			}

			Object constPlayer = ReflectionUtils.getHandle(player).getClass()
					.getConstructor(server, world.getClass(), profile.getClass(), manager)
					.newInstance(serverIns, world, profile, managerIns);

			Class<?> enumPlayerInfoAction = ReflectionUtils.Classes.getEnumPlayerInfoAction();

			Object entityPlayerArray = Array.newInstance(constPlayer.getClass(), 1);
			Array.set(entityPlayerArray, 0, constPlayer);

			Object insPacket = packetPlayOutPlayerInfo
					.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass()).newInstance(ReflectionUtils
							.getFieldObject(enumPlayerInfoAction, enumPlayerInfoAction.getDeclaredField("ADD_PLAYER")),
							entityPlayerArray);

			Class<?> enumGameMode = ReflectionUtils.getNMSClass("EnumGamemode");
			if (enumGameMode == null) {
				enumGameMode = ReflectionUtils.getNMSClass("WorldSettings$EnumGamemode");
			}

			Object gmNotSet = ReflectionUtils.getField(enumGameMode, "NOT_SET").get(enumGameMode);
			Field infoList = ReflectionUtils.getField(insPacket, "b");

			TabEntry[] entries = fillRows();

			@SuppressWarnings("unchecked")
			List<Object> playerInfoDataList = (List<Object>) infoList.get(insPacket);
			for (Rows row : this.rows) {
				playerInfoDataList.add(infoData.newInstance(insPacket, row, row.getPing(), gmNotSet,
						ReflectionUtils.getAsIChatBaseComponent(row.getText())));
				hu.montlikadani.tablist.bukkit.utils.Util.logConsole(row.getText() + " " + row.getPing());
			}

			ReflectionUtils.sendPacket(player, insPacket);

			for (int i = 0; i < 80; i++) {
				Rows row = this.rows[i];
				String fieldName = "";
				/*if (row.setHead(entries[i].getHeadSkin())) {
					fieldName = "ADD_PLAYER";
				} else {*/
					if (row.setText(entries[i].getText())) {
						fieldName = "UPDATE_DISPLAY_NAME";
					}

					if (row.setPing(entries[i].getPing())) {
						fieldName = "UPDATE_LATENCY";
					}
				//}

				if (!fieldName.isEmpty()) {
					Object insPacket2 = packetPlayOutPlayerInfo
							.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass())
							.newInstance(ReflectionUtils.getFieldObject(enumPlayerInfoAction,
									enumPlayerInfoAction.getDeclaredField(fieldName)), entityPlayerArray);

					@SuppressWarnings("unchecked")
					List<Object> playerInfoData = (List<Object>) infoList.get(insPacket2);
					for (Rows r : this.rows) {
						playerInfoData.add(infoData.newInstance(insPacket2, r, r.getPing(), gmNotSet,
								ReflectionUtils.getAsIChatBaseComponent(r.getText())));
					}

					ReflectionUtils.sendPacket(player, insPacket2);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateTab() {
		if (player == null || !player.isOnline()) {
			return;
		}

		unregisterTab();

		if (plugin.getConf().getTablistFile() == null || !plugin.getConf().getTablistFile().exists()) {
			return;
		}

		final FileConfiguration c = plugin.getTabC();
		if (c == null || !c.getBoolean("enabled")) {
			return;
		}

		final UUID uuid = player.getUniqueId();
		final String world = player.getWorld().getName();
		final String pName = player.getName();

		if (c.getStringList("disabled-worlds").contains(world) || c.getStringList("blacklisted-players").contains(pName)
				|| (TabManager.TABENABLED.containsKey(uuid) && TabManager.TABENABLED.get(uuid))
				|| plugin.isHookPreventTask(player)) {
			return;
		}

		List<String> header = null;
		List<String> footer = null;

		boolean worldEnable = false;

		final List<String> worldList = new ArrayList<>();

		if (c.contains("per-world")) {
			if (c.contains("per-world." + world + ".per-player." + pName)) {
				String path = "per-world." + world + ".per-player." + pName + ".";
				header = c.isList(path + "header") ? c.getStringList(path + "header")
						: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header")) : null;
				footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
						: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer")) : null;

				worldEnable = true;
			}

			if (header == null && footer == null) {
				if (c.isConfigurationSection("per-world")) {
					for (String s : c.getConfigurationSection("per-world").getKeys(false)) {
						worldList.add(s);
					}
				}

				if (worldList.isEmpty()) {
					if (c.contains("per-world." + world)) {
						String path = "per-world." + world + ".";
						header = c.isList(path + "header") ? c.getStringList(path + "header")
								: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header")) : null;
						footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
								: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer")) : null;

						worldEnable = true;
					}
				} else {
					t: for (String w : worldList) {
						for (String split : w.split(", ")) {
							if (world.equals(split)) {
								String path = "per-world." + w + ".";

								header = c.isList(path + "header") ? c.getStringList(path + "header")
										: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header"))
												: null;
								footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
										: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer"))
												: null;

								worldEnable = true;
								break t;
							}
						}
					}
				}
			}

			if ((header == null && footer == null) && c.contains("per-world." + world + ".per-group")) {
				String group = null;

				if (plugin.isPluginEnabled("Vault")) {
					try {
						group = plugin.getVaultPerm().getPrimaryGroup(world, player);
					} catch (UnsupportedOperationException e) {
						logConsole(Level.WARNING, "You not using any permission manager plugin!");
					}
				}

				if (group != null) {
					if (c.contains("per-world." + world + ".per-group." + group)) {
						String path = "per-world." + world + ".per-group." + group + ".";
						header = c.isList(path + "header") ? c.getStringList(path + "header")
								: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header")) : null;
						footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
								: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer")) : null;

						worldEnable = true;
					}
				}
			}
		}

		if ((header == null && footer == null) && c.contains("per-player")) {
			if (c.contains("per-player." + pName)) {
				String path = "per-player." + pName + ".";
				header = c.isList(path + "header") ? c.getStringList(path + "header")
						: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header")) : null;
				footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
						: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer")) : null;
			}
		}

		if ((header == null && footer == null) && c.contains("per-group")) {
			String group = null;

			if (plugin.isPluginEnabled("Vault")) {
				try {
					group = plugin.getVaultPerm().getPrimaryGroup(player);
				} catch (UnsupportedOperationException e) {
					logConsole(Level.WARNING, "You not using any permission manager plugin!");
				}
			}

			if (group != null) {
				if (c.contains("per-group." + group)) {
					String path = "per-group." + group + ".";
					header = c.isList(path + "header") ? c.getStringList(path + "header")
							: c.isString(path + "header") ? Arrays.asList(c.getString(path + "header")) : null;
					footer = c.isList(path + "footer") ? c.getStringList(path + "footer")
							: c.isString(path + "footer") ? Arrays.asList(c.getString(path + "footer")) : null;
				}
			}
		}

		if (header == null && footer == null) {
			header = c.isList("header") ? c.getStringList("header")
					: c.isString("header") ? Arrays.asList(c.getString("header")) : null;
			footer = c.isList("footer") ? c.getStringList("footer")
					: c.isString("footer") ? Arrays.asList(c.getString("footer")) : null;
		}

		setHeader(header);
		setFooter(footer);

		// Splitting the ", " from names
		List<String> newWorlds = worldList;
		worldList.clear();
		for (String w : newWorlds) {
			for (String split : w.split(", ")) {
				worldList.add(split);
			}
		}

		final int refreshTime = plugin.getTabRefreshTime();

		if (refreshTime < 1) {
			cancelTask();
			sendTab(worldEnable, worldList);
			return;
		}

		final boolean enableW = worldEnable;

		task = createTask(() -> {
			if (player == null || !player.isOnline()) {
				unregisterTab();
				return;
			}

			if (c.getStringList("disabled-worlds").contains(world)
					|| c.getStringList("blacklisted-players").contains(pName) || plugin.isHookPreventTask(player)
					|| (TabManager.TABENABLED.containsKey(uuid) && TabManager.TABENABLED.get(uuid))) {
				unregisterTab();
				return;
			}

			sendTab(enableW, worldList);
		}, refreshTime);
	}

	private BukkitTask createTask(Runnable run, int interval) {
		return plugin.isSpigot() ? Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, run, interval, interval)
				: Bukkit.getScheduler().runTaskTimer(plugin, run, interval, interval);
	}

	private void sendTab(boolean yesWorld, List<String> otherWorlds) {
		if (plugin.isVanished(player, false) && plugin.getTabC().getBoolean("hide-tab-when-player-vanished")) {
			TabTitle.sendTabTitle(player, "", "");
			return;
		}

		int r = 0;
		String he = "";

		if (header != null) {
			if (plugin.getTabC().getBoolean("random", false)) {
				he = header.get(ThreadLocalRandom.current().nextInt(header.size()));
			}

			if (he.isEmpty()) {
				for (String line : header) {
					r++;

					if (r > 1) {
						he = he + "\n\u00a7r";
					}

					he = he + line;
				}
			}
		}

		String fo = "";

		if (footer != null) {
			if (plugin.getTabC().getBoolean("random", false)) {
				fo = footer.get(ThreadLocalRandom.current().nextInt(footer.size()));
			}

			if (fo.isEmpty()) {
				r = 0;

				for (String line : footer) {
					r++;

					if (r > 1) {
						fo = fo + "\n\u00a7r";
					}

					fo = fo + line;
				}
			}
		}

		if (!he.trim().isEmpty()) {
			he = plugin.makeAnim(he);
		}

		if (!fo.trim().isEmpty()) {
			fo = plugin.makeAnim(fo);
		}

		Variables v = plugin.getPlaceholders();

		if (!yesWorld) {
			TabTitle.sendTabTitle(player, v.replaceVariables(player, he), v.replaceVariables(player, fo));
			return;
		}

		if (otherWorlds.isEmpty()) {
			for (Player player : player.getWorld().getPlayers()) {
				TabTitle.sendTabTitle(player, v.replaceVariables(player, he), v.replaceVariables(player, fo));
			}

			return;
		}

		for (String l : otherWorlds) {
			for (Player player : Bukkit.getWorld(l).getPlayers()) {
				TabTitle.sendTabTitle(player, v.replaceVariables(player, he), v.replaceVariables(player, fo));
			}
		}
	}

	public void unregisterTab() {
		cancelTask();

		TabTitle.sendTabTitle(player, "", "");
	}

	public void cancelTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	public TabEntry[] getTabEntries() {
		TabEntry[] tabArray = new TabEntry[80];

		ConfigurationSection section = plugin.getTabC().getConfigurationSection("rows");
		if (section == null) {
			return tabArray;
		}

		for (String key : section.getKeys(false)) {
			int index = 0;
			try {
				index = Integer.parseInt(key);
			} catch (NumberFormatException e) {
				continue;
			}

			if (index < 0 || index > 79) {
				continue;
			}

			TabEntry entry = new TabEntry().setText(section.getString(key + ".text", ""))
					.setPing(section.getInt(key + ".ping", -1));
					/*.setHead(new Property("textures", section.getString(key + ".head.value", ""),
							section.getString(key + ".head.signature", "")));*/
			tabArray[index] = entry;
		}

		for (int i = 0; i < tabArray.length; i++) {
			if (tabArray[i] == null) {
				TabEntry entry = new TabEntry().setText(plugin.getTabC().getString("default-row.text", ""))
						.setPing(plugin.getTabC().getInt("default-row.ping", -1));
						/*.setHead(new Property("textures", plugin.getTabC().getString("default-row.head.value", ""),
								plugin.getTabC().getString("default-row.head.signature", ""))*/
				tabArray[i] = entry;
			}
		}

		return tabArray;
	}

	public TabEntry getEntry(int index) {
		return getTabEntries()[(index > 79 || index < 0) ? 79 : index];
	}

	public TabEntry[] fillRows() {
		TabEntry[] entries = getTabEntries();

		for (int i = 0; i < 80; i++) {
			TabEntry entry = entries[i];
			Rows row = new Rows(TabManager.ROWUUIDS[i], i, entry.getText(), entry.getPing(), entry.getHeadSkin());
			this.rows[i] = row;
		}

		return entries;
	}
}
