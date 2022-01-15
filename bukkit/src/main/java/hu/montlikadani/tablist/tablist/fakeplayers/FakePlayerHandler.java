package hu.montlikadani.tablist.tablist.fakeplayers;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.utils.Util;

public final class FakePlayerHandler {

	protected final TabList plugin;
	private final Set<IFakePlayer> fakePlayers = new java.util.HashSet<>();

	public FakePlayerHandler(TabList plugin) {
		this.plugin = plugin;
	}

	public Set<IFakePlayer> getFakePlayers() {
		return fakePlayers;
	}

	public Optional<IFakePlayer> getFakePlayerByName(String name) {
		if (name.isEmpty()) {
			throw new IllegalArgumentException("The fake player name can not be empty");
		}

		for (IFakePlayer fp : fakePlayers) {
			if (fp.getName().equalsIgnoreCase(name)) {
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

		for (String name : cs.getKeys(false)) {
			FakePlayer fp = new FakePlayer(plugin, name, cs.getString(name + ".displayname", ""),
					cs.getString(name + ".headuuid", ""), cs.getInt(name + ".ping", -1));
			fp.show();
			fakePlayers.add(fp);
		}

		plugin.getConf().getFakeplayers().set("fakeplayers", null);

		try {
			plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void display() {
		for (IFakePlayer fp : fakePlayers) {
			fp.show();
		}
	}

	public EditingResult createPlayer(final String name, String displayName, final String headUUID, int ping) {
		if (name.isEmpty()) {
			return EditingResult.UNKNOWN;
		}

		if (getFakePlayerByName(name).isPresent()) {
			return EditingResult.ALREADY_EXIST;
		}

		String path = "list." + name + ".";
		FileConfiguration c = plugin.getConf().getFakeplayers();

		c.set(path + "headuuid", headUUID);

		if (ping < -1) {
			ping = -1;
		}

		c.set(path + "ping", ping);
		c.set(path + "displayname", displayName);

		try {
			c.save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingResult.UNKNOWN;
		}

		IFakePlayer fp = new FakePlayer(plugin, name, displayName, headUUID, ping);
		fp.show();
		fakePlayers.add(fp);
		return EditingResult.OK;
	}

	public void removeAllFakePlayer() {
		fakePlayers.forEach(IFakePlayer::remove);
		fakePlayers.clear();
	}

	public EditingResult removePlayer(String name) {
		Optional<IFakePlayer> fp = getFakePlayerByName(name);

		if (!fp.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		ConfigurationSection section = plugin.getConf().getFakeplayers().getConfigurationSection("list");
		if (section == null) {
			return EditingResult.NOT_EXIST;
		}

		for (String sName : section.getKeys(false)) {
			if (sName.equalsIgnoreCase(name)) {
				section.set(sName, null);

				try {
					plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
				} catch (IOException e) {
					e.printStackTrace();
					return EditingResult.UNKNOWN;
				}

				break;
			}
		}

		IFakePlayer fpl = fp.get();

		fpl.remove();
		fakePlayers.remove(fpl);
		return EditingResult.OK;
	}

	public EditingResult renamePlayer(final String oldName, final String newName) {
		Optional<IFakePlayer> fp = getFakePlayerByName(oldName);

		if (!fp.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		FileConfiguration c = plugin.getConf().getFakeplayers();

		ConfigurationSection section = c.getConfigurationSection("list");
		if (section == null) {
			return EditingResult.NOT_EXIST;
		}

		section.set(oldName, newName);

		try {
			c.save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingResult.UNKNOWN;
		}

		fp.get().setName(newName);
		return EditingResult.OK;
	}

	public EditingResult setSkin(String name, String uuid) {
		Optional<IFakePlayer> fp = getFakePlayerByName(name);

		if (!fp.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		Optional<UUID> id = Util.tryParseId(uuid);
		if (!id.isPresent()) {
			return EditingResult.UUID_MATCH_ERROR;
		}

		plugin.getConf().getFakeplayers().set("list." + name + ".headuuid", uuid);

		try {
			plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingResult.UNKNOWN;
		}

		fp.get().setSkin(id.get());
		return EditingResult.OK;
	}

	public EditingResult setPing(String name, int amount) {
		if (amount < -1) {
			return EditingResult.PING_AMOUNT;
		}

		Optional<IFakePlayer> fp = getFakePlayerByName(name);

		if (!fp.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		plugin.getConf().getFakeplayers().set("list." + name + ".ping", amount);

		try {
			plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingResult.UNKNOWN;
		}

		fp.get().setPing(amount);
		return EditingResult.OK;
	}

	public EditingResult setDisplayName(String name, String displayName) {
		Optional<IFakePlayer> fp = getFakePlayerByName(name);

		if (!fp.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		plugin.getConf().getFakeplayers().set("list." + name + ".displayname", displayName);

		try {
			plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingResult.UNKNOWN;
		}

		fp.get().setDisplayName(displayName);
		return EditingResult.OK;
	}

	public enum EditingResult {
		NOT_EXIST, ALREADY_EXIST, EMPTY_DATA, UUID_MATCH_ERROR, PING_AMOUNT, UNKNOWN, OK;
	}
}
