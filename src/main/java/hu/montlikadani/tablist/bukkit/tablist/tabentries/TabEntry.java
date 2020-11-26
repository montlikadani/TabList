package hu.montlikadani.tablist.bukkit.tablist.tabentries;

import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.CommentedConfig;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.FakePlayers;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.IFakePlayers;
import hu.montlikadani.tablist.bukkit.tablist.tabentries.TabEntry.Entry.EntryBuilder;
import hu.montlikadani.tablist.bukkit.utils.Util;

public class TabEntry {

	private UUID playerUuid;

	private Entry[] entries = new Entry[80];

	private static final UUID[] UUIDS = new UUID[80];

	static {
		for (int i = 0; i < 80; i++) {
			UUIDS[i] = UUID.fromString(
					String.format("00000000-0000-00%s-0000-000000000000", (i < 10) ? ("0" + i) : Integer.toString(i)));
		}
	}

	public TabEntry(UUID playerUuid) {
		this.playerUuid = playerUuid;
	}

	public UUID getPlayerUuid() {
		return playerUuid;
	}

	public Entry[] getEntries() {
		return Arrays.copyOf(entries, entries.length);
	}

	public Entry getEntry(int index) {
		return (index < 0 || index >= entries.length) ? null : entries[index];
	}

	public void modifyEntry(int index, Entry newEntry) {
		if (index >= 0 && index < entries.length) {
			entries[index] = newEntry;
		}
	}

	// 1 column = 20 row

	public Entry[] fillEntries() {
		removeEntries();
		entries = new Entry[80];

		final CommentedConfig tabConf = TabList.getInstance().getConf().getTablist();
		final ConfigurationSection section = tabConf.getConfigurationSection("rows");
		if (section == null) {
			return entries;
		}

		EntryBuilder builder = Entry.of();
		for (String key : section.getKeys(false)) {
			int index;
			try {
				index = Integer.parseInt(key);
			} catch (NumberFormatException e) {
				continue;
			}

			if (index >= 0 && index < 80) {
				entries[index] = builder.setText(section.getString(key + ".text", ""))
						.setHeadUuid(section.getString(key + ".head.uuid", "")).withIndex(index)
						.setPing(section.getInt(key + ".ping", -1)).toPlayer(Bukkit.getPlayer(playerUuid)).build();
			}
		}

		for (int i = 0; i < entries.length; i++) {
			if (entries[i] == null) {
				entries[i] = builder.setText(tabConf.getString("default-row.text", ""))
						.setPing(tabConf.getInt("default-row.ping", -1))
						.setHeadUuid(tabConf.getString("default-row.head.uuid", "")).withIndex(i)
						.toPlayer(Bukkit.getPlayer(playerUuid)).build();
			}
		}

		return entries;
	}

	public void removeEntries() {
		for (Entry entry : entries) {
			if (entry != null && entry.getColumn() != null) {
				entry.getColumn().removeFakePlayer();
			}
		}
	}

	public static class Entry {

		private IFakePlayers column;

		private Entry() {
		}

		public IFakePlayers getColumn() {
			return column;
		}

		public static EntryBuilder of() {
			return new EntryBuilder();
		}

		public static class EntryBuilder {

			private String text;
			private String headUuid;
			private int ping;
			private int index = 0;
			private Player player;

			public EntryBuilder toPlayer(Player player) {
				this.player = player;
				return this;
			}

			public EntryBuilder setText(String text) {
				if (text != null) {
					this.text = Util.colorMsg(TabList.getInstance().getPlaceholders().replaceVariables(player, text));
				}

				return this;
			}

			public EntryBuilder setHeadUuid(String headUuid) {
				if (!StringUtils.isBlank(headUuid)) {
					this.headUuid = headUuid;
				}

				return this;
			}

			public EntryBuilder setPing(int ping) {
				this.ping = ping < 0 ? 0 : ping;
				return this;
			}

			public EntryBuilder withIndex(int index) {
				this.index = (index >= 0 && index < 80) ? index : this.index++;
				return this;
			}

			public Entry build() {
				Entry entry = new Entry();
				IFakePlayers row = new FakePlayers(UUIDS[index], Integer.toString(index));
				row.createFakePlayer(player, headUuid, ping);
				row.setName(text);
				entry.column = row;
				return entry;
			}
		}
	}
}
