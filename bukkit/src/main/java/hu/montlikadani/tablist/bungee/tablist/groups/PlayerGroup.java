package hu.montlikadani.tablist.bungee.tablist.groups;

import java.util.UUID;

import hu.montlikadani.tablist.bungee.Misc;
import hu.montlikadani.tablist.bungee.TabList;
import hu.montlikadani.tablist.bungee.config.ConfigConstants;
import hu.montlikadani.tablist.bungee.config.ConfigConstants.GroupSettings;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PlayerListItem.Action;
import net.md_5.bungee.protocol.packet.PlayerListItem.Item;

public class PlayerGroup {

	private final TabList plugin;
	private final UUID playerUUID;

	private int y = 0;

	private final Item items = new Item();
	private final PlayerListItem listItem = new PlayerListItem();

	public PlayerGroup(TabList plugin, UUID playerUUID) {
		this.plugin = plugin;
		this.playerUUID = playerUUID;
	}

	public UUID getPlayerUUID() {
		return playerUUID;
	}

	public void update() {
		final ProxiedPlayer player = plugin.getProxy().getPlayer(playerUUID);
		if (player == null) {
			return;
		}

		for (String one : ConfigConstants.getGroupsKeys()) {
			GroupSettings settings = ConfigConstants.GROUP_SETTINGS.get(one);

			if (settings == null) {
				continue;
			}

			String perm = settings.getPermission();

			if (!perm.isEmpty() && !player.hasPermission(perm)) {
				continue;
			}

			String[] texts = settings.getTextArray();

			if (texts.length > 0) {
				if (y < texts.length - 1) {
					y++;
				} else {
					y = 0;
				}

				sendPacket(player, texts[y]);
			}

			break;
		}
	}

	void sendPacket(ProxiedPlayer p, String text) {
		if (!p.getUniqueId().equals(items.getUuid())) {
			items.setUuid(p.getUniqueId());
		}

		items.setDisplayName(
				ComponentSerializer.toString(TextComponent.fromLegacyText(Misc.replaceVariables(text, p))));

		listItem.setItems(new Item[] { items });

		if (listItem.getAction() != Action.UPDATE_DISPLAY_NAME) {
			listItem.setAction(Action.UPDATE_DISPLAY_NAME);
		}

		for (ProxiedPlayer pl : plugin.getProxy().getPlayers()) {
			pl.unsafe().sendPacket(listItem);
		}
	}
}
