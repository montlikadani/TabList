package hu.montlikadani.tablist.tablist.groups;

import java.util.UUID;

import hu.montlikadani.tablist.Misc;
import hu.montlikadani.tablist.config.ConfigConstants;
import hu.montlikadani.tablist.tablist.text.LegacyTextConverter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PlayerListItemUpdate;

public class PlayerGroup {

	private final UUID playerId;

	private int y = 0;

	private final PlayerListItem.Item item = new PlayerListItem.Item();
	private final PlayerListItemUpdate listItemUpdate = new PlayerListItemUpdate();

	public PlayerGroup(UUID playerId) {
		this.playerId = playerId;

		item.setUuid(playerId);
	}

	public UUID getPlayerId() {
		return playerId;
	}

	public void update() {
		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerId);

		if (player == null) {
			return;
		}

		ConfigConstants.GroupSettings group = null;

		for (ConfigConstants.GroupSettings setting : ConfigConstants.GROUP_SETTINGS) {
			if ((setting.permission.isEmpty() || player.hasPermission(setting.permission)) && setting.texts.length != 0) {
				group = setting;
			}
		}

		if (group == null) {
			return;
		}

		if (y >= group.texts.length) {
			y = 0;
		}

		sendPacket(player, group.texts[y]);

		if (y < group.texts.length - 1) {
			y++;
		} else {
			y = 0;
		}
	}

	void sendPacket(ProxiedPlayer player, String text) {
		if (text.isEmpty()) {
			item.setDisplayName(LegacyTextConverter.EMPTY_JSON);
		} else {
			item.setDisplayName(LegacyTextConverter.toBaseComponent(Misc.replaceVariables(text, player)));
		}

		listItemUpdate.setActions(java.util.EnumSet.of(PlayerListItemUpdate.Action.UPDATE_DISPLAY_NAME));
		listItemUpdate.setItems(new PlayerListItem.Item[] { item });

		for (ProxiedPlayer pl : ProxyServer.getInstance().getPlayers()) {
			pl.unsafe().sendPacket(listItemUpdate);
		}
	}
}
