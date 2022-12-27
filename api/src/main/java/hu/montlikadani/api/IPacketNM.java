package hu.montlikadani.api;

import org.bukkit.entity.Player;

import java.util.Collection;

public interface IPacketNM {

    void sendPacket(Player player, Object packet);

    void addPlayerChannelListener(Player player, Class<?>... classesToListen);

    void removePlayerChannelListener(Player player);

    Object getPlayerHandle(Player player);

    Object fromJson(String json);

    void sendTabTitle(Player player, Object header, Object footer);

    Object getNewEntityPlayer(com.mojang.authlib.GameProfile profile);

    void addPlayersToTab(Player source, Player... targets);

    void appendPlayerWithoutListed(Player source);

    void removePlayersFromTab(Player source, Collection<? extends Player> players);

    Object updateDisplayNamePacket(Object entityPlayer, Object component, boolean listName);

    void setListName(Object entityPlayer, Object component);

    Object newPlayerInfoUpdatePacketAdd(Object... entityPlayer);

    Object updateLatency(Object entityPlayer);

    Object removeEntityPlayers(Object... entityPlayers);

    void setInfoData(Object info, java.util.UUID id, int ping, Object component);

    Object createBoardTeam(Object teamNameComponent, String teamName, Player player, boolean followNameTagVisibility);

    Object unregisterBoardTeam(Object playerTeam);

    Object findBoardTeamByName(String teamName);

    Object createObjectivePacket(String objectiveName, Object nameComponent);

    Object scoreboardObjectivePacket(Object objective, int mode);

    Object scoreboardDisplayObjectivePacket(Object objective, int slot);

    Object changeScoreboardScorePacket(String objectiveName, String scoreName, int score);

    Object removeScoreboardScorePacket(String objectiveName, String scoreName, int score);

    Object createScoreboardHealthObjectivePacket(String objectiveName, Object nameComponent);

}
