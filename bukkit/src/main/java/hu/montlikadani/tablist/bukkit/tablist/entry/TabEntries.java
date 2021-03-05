package hu.montlikadani.tablist.bukkit.tablist.entry;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabEntryValues;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabEntryValues.ColumnValues;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.EntryPlayer;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.IRowPlayer;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.RowPlayer;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.task.Tasks;

public final class TabEntries {

	// Breh this thing is back
	public static final Scoreboard BOARD = Bukkit.getScoreboardManager().getNewScoreboard();

	private TabList plugin;
	private Entry[][] entries;

	private BukkitTask task;

	public TabEntries(TabList plugin) {
		this.plugin = plugin;
	}

	public Entry[][] getEntries() {
		return Arrays.copyOf(entries, entries.length);
	}

	public BukkitTask getTask() {
		return task;
	}

	public void cancelTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	public Optional<Entry> getEntry(Predicate<Entry> condition) {
		if (entries == null || condition == null) {
			return Optional.empty();
		}

		for (int column = 0; column < entries.length; column++) {
			for (int row = 0; row < entries[column].length; row++) {
				Entry entry = entries[column][row];
				if (entry != null && condition.test(entry)) {
					return Optional.of(entry);
				}
			}
		}

		return Optional.empty();
	}

	public void beginUpdate(TabListUser user) {
		if (!TabEntryValues.isEnabled()) {
			return;
		}

		appendEntries();
		EntryPlayer.removePlayer(user.getPlayer());

		if (task == null) {
			createRows();

			task = Tasks.submitAsync(() -> {
				if (plugin.getUsers().isEmpty()) {
					removeAll();
					return;
				}

				updateEntries();
			}, 4L, TabEntryValues.getRefreshRate());
		} else {
			loopEntries(entry -> {
				RowPlayer row = (RowPlayer) entry.row;
				row.show(user.getPlayer()); // Show the rows to the player
				row.replacer.requestUpdate(); // Request to update existing entry variables
			});
		}
	}

	public void removePlayer(UUID uuid) {
		EntryPlayer.removePlayer(org.bukkit.Bukkit.getPlayer(uuid));

		getEntry(entry -> entry.row.asPlayer().isPresent() && entry.row.asPlayer().get().getUniqueId().equals(uuid))
				.ifPresent(entry -> entry.row.setPlayer(null));
	}

	public void removeAll() {
		cancelTask();
		loopEntries(entry -> entry.row.remove());
		entries = null;
	}

	public void updateEntries() {
		loopEntries(entry -> {
			for (TabListUser user : plugin.getUsers()) {
				entry.row.updateText(user.getPlayer(), entry.row.getText());
			}
		});
	}

	public void fillWithDefault() {
		for (int column = 0; column < entries.length; column++) {
			for (int row = 0; row < entries[column].length; row++) {
				if (entries[column][row] != null) {
					continue;
				}

				Entry.EntryBuilder builder = Entry.of();

				Optional.ofNullable(TabEntryValues.COLUMN_SECTION.get(TabEntryValues.ColumnValues.ConfigType.DEFAULT))
						.filter(list -> !list.isEmpty()).ifPresent(list -> {
							ColumnValues value = list.get(0);
							builder.setPing(value.getPing()).setHeadId(value.getHeadId()).setText(value.getText());
						});

				entries[column][row] = builder.build(this);
			}
		}
	}

	private void createRows() {
		if (entries != null) {
			int rowIndex = 0;

			for (int column = 0; column < entries.length; column++) {
				for (int row = 0; row < entries[column].length; row++) {
					Entry entry = entries[column][row];
					if (entry != null) {
						entry.row.create(rowIndex);
						((RowPlayer) entry.row).columnIndex = column;
						rowIndex++;
					}
				}
			}
		}
	}

	private void appendEntries() {
		if (entries != null) {
			return;
		}

		int columns = TabEntryValues.getColumns();
		if (entries != null && entries.length == columns) {
			return;
		}

		entries = new Entry[columns][20];

		Optional.ofNullable(TabEntryValues.COLUMN_SECTION.get(TabEntryValues.ColumnValues.ConfigType.NORMAL))
				.ifPresent(list -> {
					int rowIndex = 0, column = 0;

					for (ColumnValues value : list) {
						Entry.EntryBuilder builder = Entry.of().setHeadId(value.getHeadId()).setPing(value.getPing())
								.setText(value.getText());

						if (column != value.getColumn()) {
							rowIndex = 0;
						}

						if ((column = value.getColumn()) > entries.length) {
							continue;
						}

						entries[column - 1][rowIndex] = builder.build(this);
						rowIndex++;
					}
				});

		fillWithDefault();
	}

	private void loopEntries(Consumer<Entry> consumer) {
		if (entries != null) {
			for (int column = 0; column < entries.length; column++) {
				for (int row = 0; row < entries[column].length; row++) {
					Entry entry = entries[column][row];
					if (entry != null) {
						consumer.accept(entry);
					}
				}
			}
		}
	}

	public static final class Entry {

		private IRowPlayer row;

		private Entry() {
		}

		public IRowPlayer getRow() {
			return row;
		}

		public static EntryBuilder of() {
			return new EntryBuilder();
		}

		public EntryBuilder toBuilder() {
			return new EntryBuilder(this);
		}

		protected static final class EntryBuilder {

			private Entry entry;

			private UUID headId;
			private String text = " ";
			private int ping = 0;

			private EntryBuilder() {
			}

			private EntryBuilder(Entry entry) {
				this.entry = entry;
			}

			public EntryBuilder setText(String text) {
				this.text = text;
				return this;
			}

			public EntryBuilder setHeadId(UUID headId) {
				this.headId = headId;
				return this;
			}

			public EntryBuilder setPing(int ping) {
				this.ping = ping;
				return this;
			}

			protected Entry build(TabEntries root) {
				if (entry == null) {
					entry = new Entry();
				}

				IRowPlayer row = entry.row == null ? new RowPlayer(root) : entry.row;

				row.setText(text);
				row.setPing(ping);
				row.setSkin(headId);

				entry.row = row;
				return entry;
			}
		}
	}
}
