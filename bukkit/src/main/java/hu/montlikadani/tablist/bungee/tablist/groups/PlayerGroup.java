package hu.montlikadani.tablist.bungee.tablist.groups;

import java.util.List;
import java.util.UUID;

import hu.montlikadani.tablist.bungee.Misc;
import hu.montlikadani.tablist.bungee.TabList;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PlayerListItem.Action;
import net.md_5.bungee.protocol.packet.PlayerListItem.Item;

public class PlayerGroup {

	private UUID playerUUID;

	private int y = 0;

	private final Item items = new Item();
	private final PlayerListItem listItem = new PlayerListItem();

	public PlayerGroup(UUID playerUUID) {
		this.playerUUID = playerUUID;
	}

	public UUID getPlayerUUID() {
		return playerUUID;
	}

	public void update() {
		final ProxiedPlayer player = TabList.getInstance().getProxy().getPlayer(playerUUID);
		if (player == null) {
			return;
		}

		final Configuration c = TabList.getInstance().getConf();

		String name = "";
		for (String num : c.getSection("groups").getKeys()) {
			String perm = c.getString("groups." + num + ".permission", "");
			if (!perm.trim().isEmpty() && !player.hasPermission(perm)) {
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

		sendPacket(player, name);
	}

	void sendPacket(ProxiedPlayer p, String name) {
		if (!p.getUniqueId().equals(items.getUuid())) {
			items.setUuid(p.getUniqueId());
		}

		items.setDisplayName(
				ComponentSerializer.toString(TextComponent.fromLegacyText(Misc.replaceVariables(name, p))));

		listItem.setItems(new Item[] { items });

		if (listItem.getAction() != Action.UPDATE_DISPLAY_NAME) {
			listItem.setAction(Action.UPDATE_DISPLAY_NAME);
		}

		for (ProxiedPlayer pl : TabList.getInstance().getProxy().getPlayers()) {
			pl.unsafe().sendPacket(listItem);
		}
	}
}
