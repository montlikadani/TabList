package hu.montlikadani.tablist.bungee.tablist.groups;

import java.util.List;
import java.util.concurrent.TimeUnit;

import hu.montlikadani.tablist.bungee.Misc;
import hu.montlikadani.tablist.bungee.TabList;
import hu.montlikadani.tablist.bungee.tablist.ITask;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PlayerListItem.Action;
import net.md_5.bungee.protocol.packet.PlayerListItem.Item;

public class Groups implements ITask {

	private TabList plugin;

	private ScheduledTask task;

	private int y = 0;

	private final Item items = new Item();
	private final PlayerListItem listItem = new PlayerListItem();

	public Groups(TabList plugin) {
		this.plugin = plugin;
	}

	@Override
	public void start() {
		if (!plugin.getConf().getBoolean("tablist-groups.enabled", false)) {
			cancel();
			return;
		}

		if (task != null) {
			cancel();
		}

		task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
			if (plugin.getProxy().getPlayers().isEmpty() || !plugin.getConf().contains("groups")) {
				cancel();
				return;
			}

			plugin.getProxy().getPlayers().forEach(this::update);
		}, 0L, plugin.getConf().getInt("tablist-groups.refresh-time"), TimeUnit.MILLISECONDS);
	}

	@Override
	public void update(final ProxiedPlayer pl) {
		final Configuration c = plugin.getConf();

		String name = "";
		for (String num : c.getSection("groups").getKeys()) {
			String perm = c.getString("groups." + num + ".permission", "");
			if (!perm.trim().isEmpty() && !pl.hasPermission(perm)) {
				continue;
			}

			List<String> list = c.getStringList("groups." + num + ".name");
			if (!list.isEmpty()) {
				int gSize = list.size() - 1;
				if (y < gSize) {
					y++;
				} else {
					y = 0;
				}

				name = list.get(y);
			} else {
				name = c.getString("groups." + num + ".name", "");
			}

			break;
		}

		if (name.trim().isEmpty()) {
			return;
		}

		sendPacket(pl, name);
	}

	@Override
	public ScheduledTask getTask() {
		return task;
	}

	@Override
	public void cancel() {
		if (task != null) {
			task.cancel();
			task = null;
		}

		plugin.getProxy().getPlayers().forEach(p -> sendPacket(p, p.getName()));
	}

	private void sendPacket(ProxiedPlayer p, String name) {
		if (listItem.getAction() != Action.UPDATE_DISPLAY_NAME) {
			listItem.setAction(Action.UPDATE_DISPLAY_NAME);
		}

		items.setUuid(p.getUniqueId());
		items.setDisplayName(
				ComponentSerializer.toString(TextComponent.fromLegacyText(Misc.replaceVariables(name, p))));

		listItem.setItems(new Item[] { items });

		for (ProxiedPlayer pl : plugin.getProxy().getPlayers()) {
			pl.unsafe().sendPacket(listItem);
		}
	}
}
