package hu.montlikadani.tablist.tablist.groups;

import java.util.UUID;

import hu.montlikadani.tablist.Misc;
import hu.montlikadani.tablist.config.ConfigConstants;
import hu.montlikadani.tablist.tablist.text.LegacyTextConverter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PlayerListItem.Action;
import net.md_5.bungee.protocol.packet.PlayerListItem.Item;

public class PlayerGroup {

	private final UUID playerId;

	private int y = 0;

	private final Item item = new Item();
	private final PlayerListItem listItem = new PlayerListItem();

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
			String perm = setting.getPermission();

			if ((perm.isEmpty() || player.hasPermission(perm)) && setting.getTextArray().length != 0) {
				group = setting;
			}
		}

		if (group == null) {
			return;
		}

		String[] texts = group.getTextArray();

		if (y >= texts.length) {
			y = 0;
		}

		sendPacket(player, texts[y]);

		if (y < texts.length - 1) {
			y++;
		} else {
			y = 0;
		}
	}

	void sendPacket(ProxiedPlayer player, String text) {
		if (text.isEmpty()) {
			item.setDisplayName(LegacyTextConverter.EMPTY_JSON);
		} else {
			item.setDisplayName(Misc.replaceVariables(LegacyTextConverter.toJson(text), player));
		}

		listItem.setItems(new Item[] { item });

		if (listItem.getAction() != Action.UPDATE_DISPLAY_NAME) {
			listItem.setAction(Action.UPDATE_DISPLAY_NAME);
		}

		for (ProxiedPlayer pl : ProxyServer.getInstance().getPlayers()) {
			pl.unsafe().sendPacket(listItem);
		}
	}
}
