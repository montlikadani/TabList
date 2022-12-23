package hu.montlikadani.tablist.packets;

import com.mojang.authlib.GameProfile;
import hu.montlikadani.api.IPacketNM;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.reflection.ClazzContainer;
import hu.montlikadani.tablist.utils.reflection.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// TODO get rid from this, I am starting to hate this now, I have a lot of work with this
public final class LegacyVersion implements IPacketNM {

    private Method playerHandleMethod, sendPacketMethod, getHandleWorldMethod, getServerMethod;
    private Field playerConnectionField, headerField, footerField, listNameField, playerTeamNameField;
    private Constructor<?> playerListHeaderFooterConstructor, entityPlayerConstructor, interactManagerConstructor;
    private Class<?> minecraftServer, interactManager, craftServerClass;

    private final List<Object> playerTeams = new ArrayList<>();

    public LegacyVersion() {
        Class<?> playerListHeaderFooter;

        try {
            playerListHeaderFooter = ReflectionUtils.classByName("net.minecraft.network.protocol.game", "PacketPlayOutPlayerListHeaderFooter");
        } catch (ClassNotFoundException e) {
            return;
        }

        try {
            playerListHeaderFooterConstructor = playerListHeaderFooter.getConstructor();
        } catch (NoSuchMethodException s) {
            try {
                playerListHeaderFooterConstructor = playerListHeaderFooter.getConstructor(ClazzContainer.getIChatBaseComponent(), ClazzContainer.getIChatBaseComponent());
            } catch (NoSuchMethodException e) {
                try {
                    playerListHeaderFooterConstructor = playerListHeaderFooter.getConstructor(ClazzContainer.getIChatBaseComponent());
                } catch (NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public Object getPlayerHandle(Player player) {
        try {
            if (playerHandleMethod == null) {
                playerHandleMethod = player.getClass().getDeclaredMethod("getHandle");
            }

            return playerHandleMethod.invoke(player);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void addPlayerChannelListener(Player player, Class<?>... classesToListen) {
    }

    @Override
    public void removePlayerChannelListener(Player player) {
    }

    private Object getServer(Class<?> server) {
        if (getServerMethod == null) {
            try {
                getServerMethod = server.getMethod("getServer");
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        try {
            if (craftServerClass == null) {
                craftServerClass = Class.forName("org.bukkit.craftbukkit." + ServerVersion.getArrayVersion()[3] + ".CraftServer");
            }

            return getServerMethod.invoke(craftServerClass.cast(Bukkit.getServer()));
        } catch (Exception x) {
            try {
                return getServerMethod.invoke(server);
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public void sendPacket(Player player, Object packet) {
        try {
            Object playerHandle = getPlayerHandle(player);

            if (playerConnectionField == null) {
                playerConnectionField = playerHandle.getClass().getDeclaredField((ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1) ? "b" : "playerConnection"));
            }

            Object playerConnection = playerConnectionField.get(playerHandle);

            if (sendPacketMethod == null) {
                sendPacketMethod = playerConnection.getClass().getDeclaredMethod(ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_18_R1) ? "a" : "sendPacket",
                        ClazzContainer.getPacket());
            }

            sendPacketMethod.invoke(playerConnection, packet);
        } catch (Exception e) {
        }
    }

    @Override
    public Object fromJson(String json) {
        try {
            return ReflectionUtils.jsonComponentMethod().invoke(ClazzContainer.getIChatBaseComponent(), json);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void sendTabTitle(Player player, Object header, Object footer) {
        if (header == null || footer == null) {
            return;
        }

        try {
            Object packet;

            if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
                packet = playerListHeaderFooterConstructor.newInstance(header, footer);
            } else {
                packet = playerListHeaderFooterConstructor.newInstance();

                if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
                    if (headerField == null) {
                        (headerField = packet.getClass().getDeclaredField("header")).setAccessible(true);
                    }

                    if (footerField == null) {
                        (footerField = packet.getClass().getDeclaredField("footer")).setAccessible(true);
                    }
                } else {
                    if (headerField == null) {
                        (headerField = packet.getClass().getDeclaredField("a")).setAccessible(true);
                    }

                    if (footerField == null) {
                        (footerField = packet.getClass().getDeclaredField("b")).setAccessible(true);
                    }
                }

                headerField.set(packet, header);
                footerField.set(packet, footer);
            }

            sendPacket(player, packet);
        } catch (Exception f) {
            Object packet = null;

            try {
                try {
                    packet = playerListHeaderFooterConstructor.newInstance(header);
                } catch (IllegalArgumentException e) {
                    try {
                        packet = playerListHeaderFooterConstructor.newInstance();
                    } catch (IllegalArgumentException ex) {
                    }
                }

                if (packet != null) {
                    if (footerField == null) {
                        (footerField = packet.getClass().getDeclaredField("b")).setAccessible(true);
                    }

                    footerField.set(packet, footer);
                    sendPacket(player, packet);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Object getNewEntityPlayer(GameProfile profile) {
        if (minecraftServer == null) {
            try {
                minecraftServer = ReflectionUtils.classByName("net.minecraft.server", "MinecraftServer");
            } catch (ClassNotFoundException c) {
                try {
                    minecraftServer = ReflectionUtils.classByName("net.minecraft.server.dedicated", "DedicatedServer");
                } catch (ClassNotFoundException e) {
                }
            }
        }

        try {
            // Only get the first world
            org.bukkit.World world = Bukkit.getServer().getWorlds().get(0);

            if (getHandleWorldMethod == null) {
                getHandleWorldMethod = world.getClass().getDeclaredMethod("getHandle");
            }

            Object worldServer = getHandleWorldMethod.invoke(world);

            if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
                if (entityPlayerConstructor == null) {
                    if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1)) {
                        entityPlayerConstructor = ReflectionUtils.classByName("net.minecraft.server.level", "EntityPlayer").getConstructor(minecraftServer, worldServer.getClass(),
                                profile.getClass(), ReflectionUtils.classByName("net.minecraft.world.entity.player", "ProfilePublicKey"));
                    } else {
                        entityPlayerConstructor = ReflectionUtils.classByName("net.minecraft.server.level", "EntityPlayer").getConstructor(minecraftServer, worldServer.getClass(),
                                profile.getClass());
                    }
                }

                if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1)) {
                    return entityPlayerConstructor.newInstance(getServer(minecraftServer), worldServer, profile, null);
                }

                return entityPlayerConstructor.newInstance(getServer(minecraftServer), worldServer, profile);
            }

            if (interactManager == null) {
                interactManager = ReflectionUtils.classByName("net.minecraft.server.level", "PlayerInteractManager");
            }

            if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_14_R1)) {
                if (interactManagerConstructor == null) {
                    interactManagerConstructor = interactManager.getConstructor(worldServer.getClass());
                }
            } else if (interactManagerConstructor == null) {
                interactManagerConstructor = interactManager.getConstructors()[0];
            }

            if (entityPlayerConstructor == null) {
                entityPlayerConstructor = ReflectionUtils.classByName("net.minecraft.server.level", "EntityPlayer").getConstructor(minecraftServer, worldServer.getClass(),
                        profile.getClass(), interactManager);
            }

            return entityPlayerConstructor.newInstance(getServer(minecraftServer), worldServer, profile, interactManagerConstructor.newInstance(worldServer));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void addPlayersToTab(Player source, Player... targets) {
        List<Object> players = new ArrayList<>(targets.length);

        for (Player player : targets) {
            players.add(getPlayerHandle(player));
        }

        try {
            sendPacket(source, newPlayerInfoUpdatePacketAdd(players));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removePlayersFromTab(Player source, Collection<? extends Player> players) {
        try {
            sendPacket(source, removeEntityPlayers(players.stream().map(this::getPlayerHandle).toArray()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void appendPlayerWithoutListed(Player source) {
        // TODO
    }

    @Override
    public Object updateDisplayNamePacket(Object entityPlayer, Object component, boolean listName) {
        try {
            if (listName) {
                setListName(entityPlayer, component);
            }

            Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
            Array.set(entityPlayerArray, 0, entityPlayer);

            return ClazzContainer.getPlayOutPlayerInfoConstructor().newInstance(ClazzContainer.getUpdateDisplayName(), entityPlayerArray);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void setListName(Object entityPlayer, Object component) {
        try {
            if (listNameField == null) {
                (listNameField = entityPlayer.getClass().getDeclaredField("listName")).setAccessible(true);
            }

            listNameField.set(entityPlayer, component);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Object newPlayerInfoUpdatePacketAdd(Object... entityPlayers) {
        try {
            return ClazzContainer.getPlayOutPlayerInfoConstructor().newInstance(ClazzContainer.getAddPlayer(), entityPlayers);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Object updateLatency(Object entityPlayer) {
        try {
            Object entityPlayerArray = Array.newInstance(entityPlayer.getClass(), 1);
            Array.set(entityPlayerArray, 0, entityPlayer);

            return ClazzContainer.getPlayOutPlayerInfoConstructor().newInstance(ClazzContainer.getUpdateLatency(), entityPlayerArray);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Object removeEntityPlayers(Object... entityPlayer) {
        try {
            return ClazzContainer.getPlayOutPlayerInfoConstructor().newInstance(ClazzContainer.getRemovePlayer(), entityPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setInfoData(Object info, java.util.UUID id, int ping, Object component) {
        try {
            for (Object infoData : (List<Object>) ClazzContainer.getInfoList().get(info)) {
                GameProfile profile;

                if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
                    profile = (GameProfile) ClazzContainer.getPlayerInfoDataProfileField().get(infoData);
                } else {
                    profile = (GameProfile) ClazzContainer.getPlayerInfoDataProfileMethod().invoke(infoData);
                }

                if (!profile.getId().equals(id)) {
                    continue;
                }

                Constructor<?> playerInfoDataConstr = ClazzContainer.getPlayerInfoDataConstructor();
                Object gameMode = ClazzContainer.getPlayerInfoDataGameMode().get(infoData);
                Object packet;

                if (playerInfoDataConstr.getParameterCount() == 5) {
                    if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1)) {
                        packet = playerInfoDataConstr.newInstance(profile, ping, gameMode, component, null);
                    } else {
                        packet = playerInfoDataConstr.newInstance(info, profile, ping, gameMode, component);
                    }
                } else {
                    packet = playerInfoDataConstr.newInstance(profile, ping, gameMode, component);
                }

                ClazzContainer.getInfoList().set(info, Collections.singletonList(packet));
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Object createBoardTeam(Object teamName, Player player, boolean followNameTagVisibility) {
        Object newTeamPacket = null, scoreTeam = null;

        try {
            if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
                scoreTeam = ClazzContainer.getScoreboardTeamConstructor().newInstance(ClazzContainer.getScoreboardConstructor().newInstance(), teamName.toString());

                @SuppressWarnings("unchecked")
                Collection<String> playerNameSet = (Collection<String>) ClazzContainer.getPlayerNameSetMethod().invoke(scoreTeam);
                playerNameSet.add(player.getName());

                ClazzContainer.getScoreboardTeamNames().set(scoreTeam, playerNameSet);

                ClazzContainer.getScoreboardTeamSetDisplayName().invoke(scoreTeam, teamName);
            } else {
                newTeamPacket = ClazzContainer.getPacketPlayOutScoreboardTeamConstructor().newInstance();

                ClazzContainer.getScoreboardTeamName().set(newTeamPacket, teamName);
                ClazzContainer.getScoreboardTeamMode().set(newTeamPacket, 0);
                ClazzContainer.getScoreboardTeamDisplayName().set(newTeamPacket, ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R1) ? teamName : teamName.toString());
                ClazzContainer.getScoreboardTeamNames().set(newTeamPacket, Collections.singletonList(player.getName()));
            }

            if (followNameTagVisibility) {
                String optionName = null;

                for (Team team : player.getScoreboard().getTeams()) {
                    if (ClazzContainer.isTeamOptionStatusEnumExist()) {
                        Team.OptionStatus optionStatus = team.getOption(Team.Option.NAME_TAG_VISIBILITY);

                        switch (optionStatus) {
                            case FOR_OTHER_TEAMS:
                                optionName = "hideForOtherTeams";
                                break;
                            case FOR_OWN_TEAM:
                                optionName = "hideForOwnTeam";
                                break;
                            default:
                                if (optionStatus != Team.OptionStatus.ALWAYS) {
                                    optionName = optionStatus.name().toLowerCase(Locale.ENGLISH);
                                }

                                break;
                        }
                    } else {
                        org.bukkit.scoreboard.NameTagVisibility visibility = team.getNameTagVisibility();

                        switch (visibility) {
                            case HIDE_FOR_OTHER_TEAMS:
                                optionName = "hideForOtherTeams";
                                break;
                            case HIDE_FOR_OWN_TEAM:
                                optionName = "hideForOwnTeam";
                                break;
                            default:
                                if (visibility != org.bukkit.scoreboard.NameTagVisibility.ALWAYS) {
                                    optionName = visibility.name().toLowerCase(Locale.ENGLISH);
                                }

                                break;
                        }
                    }
                }

                if (optionName != null) {
                    if (scoreTeam != null) {
                        for (Object f : ClazzContainer.getScoreboardNameTagVisibilityEnumConstants()) {
                            if (optionName.equalsIgnoreCase((String) ClazzContainer.getNameTagVisibilityNameField().get(f))) {
                                ClazzContainer.getScoreboardTeamSetNameTagVisibility().invoke(scoreTeam, f);
                                break;
                            }
                        }
                    } else {
                        ClazzContainer.getNameTagVisibility().set(newTeamPacket, optionName);
                    }
                }
            }

            if (newTeamPacket == null) {
                newTeamPacket = ClazzContainer.scoreboardTeamPacketByAction(scoreTeam, 0);
            }

            playerTeams.add(scoreTeam == null ? newTeamPacket : scoreTeam);
            return newTeamPacket;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Object unregisterBoardTeam(Object playerTeam) {
        playerTeams.remove(playerTeam);

        try {
            if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
                return ClazzContainer.scoreboardTeamPacketByAction(playerTeam, 1);
            }

            Object oldTeamPacket = ClazzContainer.getPacketPlayOutScoreboardTeamConstructor().newInstance();

            if (playerTeamNameField == null) {
                (playerTeamNameField = fieldByNameAndType(playerTeam.getClass(), String.class, "d", "a")).setAccessible(true);
            }

            ClazzContainer.getScoreboardTeamName().set(oldTeamPacket, playerTeamNameField.get(playerTeam));
            ClazzContainer.getScoreboardTeamMode().set(oldTeamPacket, 1);

            return oldTeamPacket;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Object findBoardTeamByName(String teamName) {
        try {
            for (Object team : playerTeams) {
                if (playerTeamNameField == null) {
                    (playerTeamNameField = fieldByNameAndType(team.getClass(), String.class, "d", "a")).setAccessible(true);
                }

                if (playerTeamNameField.get(team).equals(teamName)) {
                    return team;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private Field fieldByNameAndType(Class<?> where, Class<?> type, String... names) {
        for (Field field : where.getDeclaredFields()) {
            if (field.getType() == type) {
                for (String name : names) {
                    if (field.getName().equals(name)) {
                        return field;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Object createObjectivePacket(String objectiveName, Object nameComponent) {
        try {
            return ClazzContainer.getFirstScoreboardObjectiveConstructor().newInstance(null, objectiveName, ClazzContainer.getiScoreboardCriteriaDummy(), nameComponent,
                    ClazzContainer.getEnumScoreboardHealthDisplayInteger());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Object scoreboardObjectivePacket(Object objective, int mode) {
        try {
            return ClazzContainer.getPacketPlayOutScoreboardObjectiveConstructor().newInstance(objective, mode);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Object scoreboardDisplayObjectivePacket(Object objective, int slot) {
        try {
            return ClazzContainer.getPacketPlayOutScoreboardDisplayObjectiveConstructor().newInstance(slot, objective);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Object changeScoreboardScorePacket(String objectiveName, String scoreName, int score) {
        try {
            return ClazzContainer.getPacketPlayOutScoreboardScoreConstructor().newInstance(ClazzContainer.getEnumScoreboardActionChange(), objectiveName, scoreName, score);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Object removeScoreboardScorePacket(String objectiveName, String scoreName, int score) {
        try {
            return ClazzContainer.getPacketPlayOutScoreboardScoreConstructor().newInstance(ClazzContainer.getEnumScoreboardActionRemove(), objectiveName, scoreName, score);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Object createScoreboardHealthObjectivePacket(String objectiveName, Object nameComponent) {
        try {
            return ClazzContainer.getFirstScoreboardObjectiveConstructor().newInstance(null, objectiveName, ClazzContainer.getiScoreboardCriteriaDummy(), nameComponent,
                    ClazzContainer.getEnumScoreboardHealthDisplayInteger());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
