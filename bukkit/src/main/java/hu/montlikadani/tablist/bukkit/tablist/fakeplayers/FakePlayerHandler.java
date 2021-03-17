package hu.montlikadani.tablist.bukkit.tablist.fakeplayers;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.utils.Util;

public class FakePlayerHandler {

	private TabList plugin;

	private final Set<IFakePlayers> fakePlayers = new HashSet<>();

	public FakePlayerHandler(TabList plugin) {
		this.plugin = plugin;
	}

	public Set<IFakePlayers> getFakePlayers() {
		return fakePlayers;
	}

	public Optional<IFakePlayers> getFakePlayerByName(String name) {
		if (name != null) {
			for (IFakePlayers fp : fakePlayers) {
				if (fp.getName().equalsIgnoreCase(name)) {
					return Optional.of(fp);
				}
			}
		}

		return Optional.empty();
	}

	public Optional<IFakePlayers> getFakePlayerById(int id) {
		for (IFakePlayers fp : fakePlayers) {
			if (fp.getId() == id) {
				return Optional.of(fp);
			}
		}

		return Optional.empty();
	}

	public void load() {
		fakePlayers.clear();

		if (!ConfigValues.isFakePlayers()) {
			return;
		}

		ConfigurationSection cs = plugin.getConf().getFakeplayers().getConfigurationSection("list");
		if (cs == null) {
			cs = plugin.getConf().getFakeplayers().createSection("list");
		}

		// Old
		if (plugin.getConf().getFakeplayers().isList("fakeplayers")) {
			for (String one : plugin.getConf().getFakeplayers().getStringList("fakeplayers")) {
				if (one.contains(";")) {
					String[] split = one.split(";");
					String path = split[0] + ".";
					cs.set(path + "displayname", split[0]);

					if (split.length > 0 && Util.tryParseId(split[1]).isPresent()) {
						cs.set(path + "headuuid", split[1]);
					}

					if (split.length > 1) {
						try {
							cs.set(path + "ping", Integer.parseInt(split[2]));
						} catch (NumberFormatException e) {
						}
					}
				}
			}
		}

		for (String name : cs.getKeys(false)) {
			String displayName = cs.getString(name + ".displayname", "");
			final String headUUID = cs.getString(name + ".headuuid", "");
			final int ping = cs.getInt(name + ".ping", -1);

			final IFakePlayers fp = new FakePlayers();
			fp.setName(name);
			fp.setDisplayName(displayName);

			plugin.getServer().getOnlinePlayers().forEach(all -> fp.createFakePlayer(all, headUUID, ping));
			fakePlayers.add(fp);
		}

		plugin.getConf().getFakeplayers().set("fakeplayers", null);

		try {
			plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void display(Player player) {
		fakePlayers.forEach(fpl -> fpl.createFakePlayer(player));
	}

	public EditingContextError createPlayer(Player p, String name, String displayName) {
		return createPlayer(p, name, displayName, "", -1);
	}

	public EditingContextError createPlayer(Player p, String name, String displayName, int ping) {
		return createPlayer(p, name, displayName, "", ping);
	}

	public EditingContextError createPlayer(final Player p, final String name, String displayName,
			final String headUUID, final int ping) {
		if (name == null || name.trim().isEmpty()) {
			return EditingContextError.UNKNOWN;
		}

		if (getFakePlayerByName(name).isPresent()) {
			return EditingContextError.ALREADY_EXIST;
		}

		String path = "list." + name + ".";
		FileConfiguration c = plugin.getConf().getFakeplayers();

		c.set(path + "headuuid", headUUID);

		if (ping > -1) {
			c.set(path + "ping", ping);
		}

		c.set(path + "displayname", displayName);

		try {
			c.save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingContextError.UNKNOWN;
		}

		IFakePlayers fp = new FakePlayers();
		fp.setName(name);
		fp.setDisplayName(displayName);
		fp.createFakePlayer(p, headUUID, ping);
		fakePlayers.add(fp);
		return EditingContextError.OK;
	}

	public void removeAllFakePlayer() {
		fakePlayers.forEach(IFakePlayers::removeFakePlayer);
		fakePlayers.clear();
	}

	public EditingContextError removePlayer(String name) {
		Optional<IFakePlayers> fp = getFakePlayerByName(name);
		if (name == null || !fp.isPresent() || name.trim().isEmpty()) {
			return EditingContextError.NOT_EXIST;
		}

		for (String sName : plugin.getConf().getFakeplayers().getConfigurationSection("list").getKeys(false)) {
			if (sName.equalsIgnoreCase(name)) {
				plugin.getConf().getFakeplayers().set("list." + sName, null);
				try {
					plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
				} catch (IOException e) {
					e.printStackTrace();
					return EditingContextError.UNKNOWN;
				}

				break;
			}
		}

		fp.get().removeFakePlayer();
		fakePlayers.remove(fp.get());
		return EditingContextError.OK;
	}

	public EditingContextError renamePlayer(final String oldName, final String newName) {
		Optional<IFakePlayers> fp = getFakePlayerByName(oldName);
		if (newName == null || !fp.isPresent()) {
			return EditingContextError.NOT_EXIST;
		}

		FileConfiguration c = plugin.getConf().getFakeplayers();
		if (c.isConfigurationSection("list." + oldName)) {
			c.set("list." + oldName, newName);
		}

		try {
			c.save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingContextError.UNKNOWN;
		}

		fp.get().setName(newName);
		return EditingContextError.OK;
	}

	public EditingContextError setSkin(String name, String uuid) {
		Optional<IFakePlayers> fp = getFakePlayerByName(name);
		if (!fp.isPresent()) {
			return EditingContextError.NOT_EXIST;
		}

		if (!Util.tryParseId(uuid).isPresent()) {
			return EditingContextError.UUID_MATCH_ERROR;
		}

		plugin.getConf().getFakeplayers().set("list." + name + ".headuuid", uuid);

		try {
			plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingContextError.UNKNOWN;
		}

		fp.get().setSkin(UUID.fromString(uuid));
		return EditingContextError.OK;
	}

	public EditingContextError setPing(String name, int amount) {
		Optional<IFakePlayers> fp = getFakePlayerByName(name);
		if (!fp.isPresent()) {
			return EditingContextError.NOT_EXIST;
		}

		if (amount < 0) {
			return EditingContextError.PING_AMOUNT;
		}

		plugin.getConf().getFakeplayers().set("list." + name + ".ping", amount);

		try {
			plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingContextError.UNKNOWN;
		}

		fp.get().setPing(amount);
		return EditingContextError.OK;
	}

	public EditingContextError setDisplayName(String name, String displayName) {
		Optional<IFakePlayers> fp = getFakePlayerByName(name);
		if (!fp.isPresent()) {
			return EditingContextError.NOT_EXIST;
		}

		plugin.getConf().getFakeplayers().set("list." + name + ".displayname", displayName);

		try {
			plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingContextError.UNKNOWN;
		}

		fp.get().setDisplayName(displayName);
		return EditingContextError.OK;
	}

	public enum EditingContextError {
		NOT_EXIST, ALREADY_EXIST, EMPTY_DATA, UUID_MATCH_ERROR, PING_AMOUNT, UNKNOWN, OK;
	}
}
