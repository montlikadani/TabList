package hu.montlikadani.tablist.packets;

import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.tablist.tablist.TabText;

public interface IPacketNM {

	void sendPacket(Player player, Object packet);

	void addPlayerChannelListener(Player player);

	void removePlayerChannelListener(Player player);

	Object getPlayerHandle(Player player);

	Object fromJson(String json);

	void sendTabTitle(Player player, TabText header, TabText footer);

	Object getNewEntityPlayer(GameProfile profile);

	void addPlayerToTab(Player source, Player target);

	void removePlayerFromTab(Player source, Player target);

	Object updateDisplayNamePacket(Object entityPlayer, String component, boolean listName);

	void setListName(Object entityPlayer, String component);

	Object newPlayerInfoUpdatePacketAdd(Object entityPlayer);

	Object updateLatency(Object entityPlayer);

	Object removeEntityPlayer(Object entityPlayer);

	void setInfoData(Object info, java.util.UUID id, int ping, Object component);

	Object createBoardTeam(String teamName, Player player);

	Object unregisterBoardTeam(Object playerTeam);

	Object findBoardTeamByName(String teamName);

	Object createObjectivePacket(String objectiveName, Object nameComponent);

	Object scoreboardObjectivePacket(Object objective, int mode);

	Object scoreboardDisplayObjectivePacket(Object objective, int slot);

	Object changeScoreboardScorePacket(String objectiveName, String scoreName, int score);

	Object removeScoreboardScorePacket(String objectiveName, String scoreName, int score);

	Object createScoreboardHealthObjectivePacket(String objectiveName, Object nameComponent);

}
