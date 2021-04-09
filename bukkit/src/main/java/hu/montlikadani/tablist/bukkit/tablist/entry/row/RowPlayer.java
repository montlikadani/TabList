package hu.montlikadani.tablist.bukkit.tablist.entry.row;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.tablist.entry.TabEntries;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.variable.VariableReplacer;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;

public class RowPlayer implements IRowPlayer {

	public final TabEntries root;
	public final VariableReplacer replacer;

	public int rowIndex = 0, columnIndex = 0;

	private final TabList plugin = TabListAPI.getPlugin();
	private final InfoName infoName;

	private UUID skinId;
	private String text = " ";
	private Player player;

	public RowPlayer(TabEntries root) {
		this.root = root;

		infoName = new InfoName(plugin);
		replacer = new VariableReplacer(this);
	}

	public final InfoName getInfoName() {
		return infoName;
	}

	@Override
	public Optional<Player> asPlayer() {
		return Optional.ofNullable(player);
	}

	@Override
	public String getText() {
		return text;
	}

	/**
	 * Checks whenever this row text is empty
	 * 
	 * @return true if empty, otherwise false
	 */
	public final boolean isTextEmpty() {
		return text.trim().isEmpty();
	}

	/**
	 * Checks whenever the text is empty, skin is not set and another alive player
	 * is not exist. More specifically, this row text is empty, skin uuid is not set
	 * and player is also not set.
	 * 
	 * @return true if empty, otherwise false
	 */
	public final boolean isEmpty() {
		return skinId == null && player == null && isTextEmpty();
	}

	@Override
	public int getPingLatency() {
		return infoName.getPing();
	}

	@Override
	public void setText(String text) {
		this.text = text == null ? " " : text;
	}

	@Override
	public void setPing(int ping) {
		infoName.setPing(ping);
	}

	@Override
	public void setPlayer(Player player) {
		if ((this.player = player) != null) {
			infoName.movePlayer(player, rowIndex);
		}
	}

	@Override
	public UUID getHeadId() {
		return skinId;
	}

	@Override
	public void create(int rowIndex) {
		if (player != null) {
			return;
		}

		String name = String.format("%03d", this.rowIndex = rowIndex); // 00 + index - sort by row index
		infoName.create(name, skinId, text);
	}

	public void show(Player player) {
		if (plugin.getUsers().size() == 1 && infoName.getRowPlayer() != null && infoName.getPacket() != null) {
			ReflectionUtils.sendPacket(player, infoName.getPacket());
			ReflectionUtils.sendPacket(player, infoName.getRowPlayer());
		} else {
			infoName.addPlayer(null, text, player);
		}
	}

	@Override
	public void remove() {
		infoName.remove(player);
	}

	@Override
	// Thread-blocking method
	public synchronized String updateText(Player player, String text) {
		if (infoName.getRowPlayer() == null || player == null || this.player != null || text.trim().isEmpty()) {
			// Player names should only be changed in groups
			//
			// Do not update empty entries too frequently
			return text;
		}

		text = plugin.makeAnim(text);
		text = replacer.replaceVariables(text);
		text = plugin.getPlaceholders().replaceVariables(player, text);

		if (ServerVersion.isCurrentLower(ServerVersion.v1_16_R1)) {
			text = Util.colorMsg(text);
		}

		infoName.updateDisplayName(null, text, player);
		return text;
	}

	@Override
	public void setSkin(UUID skinId) {
		if (player != null || skinId == null || skinId.equals(this.skinId)) {
			return;
		}

		this.skinId = skinId;

		if (Bukkit.getServer().getOnlineMode() && ServerVersion.isCurrentHigher(ServerVersion.v1_8_R2)) {
			infoName.setSkin(text, skinId);
		}
	}
}
