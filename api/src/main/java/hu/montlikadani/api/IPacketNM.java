package hu.montlikadani.api;

import org.bukkit.entity.Player;

public interface IPacketNM {

    String PACKET_INJECTOR_NAME = "TLPacketInjector";

    void sendPacket(Player player, Object packet);

    void addPlayerChannelListener(Player player, java.util.List<Class<?>> classesToListen);

    void removePlayerChannelListener(Player player);

    Object getPlayerHandle(Player player);

    Object fromJson(String json);

    void sendTabTitle(Player player, Object header, Object footer);

    Object getNewEntityPlayer(com.mojang.authlib.GameProfile profile);

    Object updateDisplayNamePacket(Object entityPlayer, Object component, boolean listName);

    void setListName(Object entityPlayer, Object component);

    Object newPlayerInfoUpdatePacketAdd(Object... entityPlayer);

    Object updateLatency(Object entityPlayer);

    Object removeEntityPlayers(Object... entityPlayers);

    void setInfoData(Object info, java.util.UUID id, int ping, Object component);

    void createBoardTeam(String teamName, Player player, boolean followNameTagVisibility);

    Object unregisterBoardTeam(String teamName);

    Object createObjectivePacket(String objectiveName, Object nameComponent);

    Object scoreboardObjectivePacket(Object objective, int mode);

    Object scoreboardDisplayObjectivePacket(Object objective, int slot);

    Object changeScoreboardScorePacket(String objectiveName, String scoreName, int score);

    Object removeScoreboardScorePacket(String objectiveName, String scoreName, int score);

    Object createScoreboardHealthObjectivePacket(String objectiveName, Object nameComponent);

    default double serverTps() {
        return -1;
    }

    default int playerPing(Player player) {
        return -1;
    }
}
