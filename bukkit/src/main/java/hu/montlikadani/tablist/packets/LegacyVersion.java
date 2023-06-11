package hu.montlikadani.tablist.packets;

import com.mojang.authlib.GameProfile;
import hu.montlikadani.api.IPacketNM;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.reflection.ClazzContainer;
import hu.montlikadani.tablist.utils.reflection.ReflectionUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
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
import java.util.Optional;
import java.util.UUID;

public final class LegacyVersion implements IPacketNM {

    private Method playerHandleMethod, sendPacketMethod, getHandleWorldMethod, getServerMethod, interactGameModeMethod, gameProfileMethod, jsonComponentMethod,
            chatSerializerMethodA;
    private Field playerConnectionField, headerField, footerField, listNameField, playerTeamNameField, networkManager, channel, playerLatency,
            interactManagerField;
    private Constructor<?> playerListHeaderFooterConstructor, entityPlayerConstructor, interactManagerConstructor, packetPlayOutAnimation;
    private Class<?> minecraftServer, interactManager, craftServerClass, chatSerializer;

    private final List<Object> playerTeams = new ArrayList<>();
    private final java.util.Set<TagTeam> tagTeams = new java.util.HashSet<>();

    public LegacyVersion() {
        try {
            Class<?> networkManagerClass;

            try {
                networkManagerClass = ClazzContainer.classByName("net.minecraft.server.network", "NetworkManager");
            } catch (ClassNotFoundException ex) {
                networkManagerClass = ClazzContainer.classByName("net.minecraft.network", "NetworkManager");
            }

            networkManager = ClazzContainer.getFieldByType(ClazzContainer.classByName("net.minecraft.server.network", "PlayerConnection"),
                    networkManagerClass);
            channel = ClazzContainer.getFieldByType(networkManagerClass, Channel.class);

            playerConnectionField = ClazzContainer.classByName("net.minecraft.server.level", "EntityPlayer")
                    .getDeclaredField((ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1) ? "b" : "playerConnection"));

            interactManager = ClazzContainer.classByName("net.minecraft.server.level", "PlayerInteractManager");
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        Class<?> playerListHeaderFooter;
        try {
            playerListHeaderFooter = ClazzContainer.classByName("net.minecraft.network.protocol.game", "PacketPlayOutPlayerListHeaderFooter");
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
    public void addPlayerChannelListener(Player player, List<Class<?>> classesToListen) {
        Object entityPlayer = getPlayerHandle(player);
        Channel channel;

        try {
            channel = (Channel) this.channel.get(networkManager.get(playerConnectionField.get(entityPlayer)));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return;
        }

        if (channel.pipeline().get(PACKET_INJECTOR_NAME) == null) {
            try {
                channel.pipeline().addBefore("packet_handler", PACKET_INJECTOR_NAME, new PacketReceivingListener(player.getUniqueId(), classesToListen));
            } catch (java.util.NoSuchElementException ex) {
                // packet_handler not exists, sure then, ignore
            }
        }
    }

    @Override
    public void removePlayerChannelListener(Player player) {
        Object entityPlayer = getPlayerHandle(player);
        Channel channel;

        try {
            channel = (Channel) this.channel.get(networkManager.get(playerConnectionField.get(entityPlayer)));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return;
        }

        if (channel.pipeline().get(PACKET_INJECTOR_NAME) != null) {
            channel.pipeline().remove(PACKET_INJECTOR_NAME);
        }
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
                craftServerClass = Class.forName("org.bukkit.craftbukkit." + ServerVersion.nmsVersion() + ".CraftServer");
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
        sendPacket(getPlayerHandle(player), packet);
    }

    private void sendPacket(Object handle, Object packet) {
        try {
            Object playerConnection = playerConnectionField.get(handle);

            if (sendPacketMethod == null) {
                sendPacketMethod = playerConnection.getClass().getDeclaredMethod(ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_18_R1) ? "a" : "sendPacket",
                        ClazzContainer.getPacket());
            }

            sendPacketMethod.invoke(playerConnection, packet);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException ignored) {
        }
    }

    @Override
    public Object fromJson(String json) {
        try {
            return jsonComponentMethod().invoke(ClazzContainer.getIChatBaseComponent(), json);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            try {
                if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
                    return asChatSerializer(json);
                }

                return jsonComponentMethod().invoke(ClazzContainer.getIChatBaseComponent(), json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private Method jsonComponentMethod() {
        if (jsonComponentMethod != null) {
            return jsonComponentMethod;
        }

        try {
            Class<?>[] declaredClasses = ClazzContainer.getIChatBaseComponent().getDeclaredClasses();

            if (declaredClasses.length != 0) {
                return jsonComponentMethod = declaredClasses[0].getMethod("a", String.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonComponentMethod;
    }

    private Object asChatSerializer(String json) throws Exception {
        if (chatSerializer == null) {
            chatSerializer = Class.forName("net.minecraft.server." + ServerVersion.nmsVersion() + ".ChatSerializer");
        }

        if (chatSerializerMethodA == null) {
            chatSerializerMethodA = chatSerializer.getMethod("a", String.class);
        }

        return ClazzContainer.getIChatBaseComponent().cast(chatSerializerMethodA.invoke(chatSerializer, json));
    }

    @Override
    public void sendTabTitle(Player player, Object header, Object footer) {
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
                minecraftServer = ClazzContainer.classByName("net.minecraft.server", "MinecraftServer");
            } catch (ClassNotFoundException c) {
                try {
                    minecraftServer = ClazzContainer.classByName("net.minecraft.server.dedicated", "DedicatedServer");
                } catch (ClassNotFoundException e) {
                }
            }
        }

        org.bukkit.World world = Bukkit.getServer().getWorlds().get(0);

        try {
            if (getHandleWorldMethod == null) {
                getHandleWorldMethod = world.getClass().getDeclaredMethod("getHandle");
            }

            Object worldServer = getHandleWorldMethod.invoke(world);

            if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
                if (entityPlayerConstructor == null) {
                    if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1)) {
                        entityPlayerConstructor = ClazzContainer.classByName("net.minecraft.server.level", "EntityPlayer").getConstructor(minecraftServer, worldServer.getClass(),
                                profile.getClass(), ClazzContainer.classByName("net.minecraft.world.entity.player", "ProfilePublicKey"));
                    } else {
                        entityPlayerConstructor = ClazzContainer.classByName("net.minecraft.server.level", "EntityPlayer").getConstructor(minecraftServer, worldServer.getClass(),
                                profile.getClass());
                    }
                }

                if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1)) {
                    return entityPlayerConstructor.newInstance(getServer(minecraftServer), worldServer, profile, null);
                }

                return entityPlayerConstructor.newInstance(getServer(minecraftServer), worldServer, profile);
            }

            if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_14_R1)) {
                if (interactManagerConstructor == null) {
                    interactManagerConstructor = interactManager.getConstructor(worldServer.getClass());
                }
            } else if (interactManagerConstructor == null) {
                interactManagerConstructor = interactManager.getConstructors()[0];
            }

            if (entityPlayerConstructor == null) {
                entityPlayerConstructor = ClazzContainer.classByName("net.minecraft.server.level", "EntityPlayer").getConstructor(minecraftServer, worldServer.getClass(),
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
        Object handle = getPlayerHandle(source);

        sendPacket(handle, removeEntityPlayers(players.stream().map(this::getPlayerHandle).toArray()));
        sendUpdatePacket(handle);
    }

    @Override
    public void appendPlayerWithoutListed(Player source) {
        sendUpdatePacket(getPlayerHandle(source));
    }

    private void sendUpdatePacket(Object player) {
        try {
            Object updatePacket = ClazzContainer.getPlayOutPlayerInfoConstructor().newInstance(ClazzContainer.getAddPlayer(), toArray(player));

            if (playerLatency == null && (playerLatency = fieldByNameAndType(player.getClass(), int.class, "latency", "ping", "g", "f", "e")) == null) {
                return;
            }

            // This is why I hate
            if (gameProfileMethod == null && (gameProfileMethod = ClazzContainer.methodByTypeAndName(player.getClass().getSuperclass(), GameProfile.class, null,
                    "getProfile", "fi", "getGameProfile", "cS", "dH", "fp", "da", "cK", "eA", "ez", "ed", "do", "da", "cP", "cL")) == null) {
                return;
            }

            if (interactManagerField == null && (interactManagerField = fieldByNameAndType(player.getClass(), interactManager,
                    "playerInteractManager", "d", "c", "gameMode")) == null) {
                return;
            }

            Object infoData = null;

            try {
                Object interactManagerInstance = interactManagerField.get(player);

                if (interactGameModeMethod == null) {
                    interactGameModeMethod = ClazzContainer.methodByTypeAndName(interactManagerInstance.getClass(), ClazzContainer.getGameModeSurvival().getClass(), null,
                            "b", "getGameModeForPlayer", "getGameMode");
                }

                int ping = playerLatency.getInt(player);
                GameProfile profile = (GameProfile) gameProfileMethod.invoke(player);
                Object gameMode = interactGameModeMethod.invoke(interactManagerInstance);

                if (ClazzContainer.getPlayerInfoDataConstructor().getParameterCount() == 5) {
                    if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1)) {
                        infoData = ClazzContainer.getPlayerInfoDataConstructor().newInstance(profile, ping, gameMode,
                                ReflectionUtils.EMPTY_COMPONENT, null);
                    } else {
                        infoData = ClazzContainer.getPlayerInfoDataConstructor().newInstance(updatePacket, profile, ping, gameMode, ReflectionUtils.EMPTY_COMPONENT);
                    }
                } else {
                    infoData = ClazzContainer.getPlayerInfoDataConstructor().newInstance(profile, ping, gameMode, ReflectionUtils.EMPTY_COMPONENT);
                }
            } catch (IllegalAccessException | InstantiationException |
                     java.lang.reflect.InvocationTargetException e) {
                e.printStackTrace();
            }

            setEntriesField(updatePacket, Collections.singletonList(infoData));

            if (packetPlayOutAnimation == null) {
                packetPlayOutAnimation = ClazzContainer.classByName("net.minecraft.network.protocol.game", "PacketPlayOutAnimation")
                        .getConstructors()[1];
            }

            Object animatePacket = packetPlayOutAnimation.newInstance(player, 0);

            for (Player pl : Bukkit.getOnlinePlayers()) {
                sendPacket(pl, updatePacket);
                sendPacket(pl, animatePacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object updateDisplayNamePacket(Object entityPlayer, Object component, boolean listName) {
        try {
            if (listName) {
                setListName(entityPlayer, component);
            }

            return ClazzContainer.getPlayOutPlayerInfoConstructor().newInstance(ClazzContainer.getUpdateDisplayName(), toArray(entityPlayer));
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

            // Weird reflection behaviour: this sometimes work and not
            try {
                return ClazzContainer.getPlayOutPlayerInfoConstructor().newInstance(ClazzContainer.getAddPlayer(), toArray(entityPlayers));
            } catch (IllegalArgumentException ex) {
                return ClazzContainer.getPlayOutPlayerInfoConstructor().newInstance(ClazzContainer.getAddPlayer(), entityPlayers);
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Object updateLatency(Object entityPlayer) {
        try {

            // Weird reflection behaviour: this sometimes work and not
            try {
                return ClazzContainer.getPlayOutPlayerInfoConstructor().newInstance(ClazzContainer.getUpdateLatency(), toArray(entityPlayer));
            } catch (IllegalArgumentException ex) {
                return ClazzContainer.getPlayOutPlayerInfoConstructor().newInstance(ClazzContainer.getUpdateLatency(), entityPlayer);
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Object removeEntityPlayers(Object... entityPlayers) {
        try {
            return ClazzContainer.getPlayOutPlayerInfoConstructor().newInstance(ClazzContainer.getRemovePlayer(), toArray(entityPlayers));
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    // don't know why this required, but without this "argument type mismatch"
    private Object toArray(Object... arr) {
        Object entityPlayerArray = Array.newInstance(arr[0].getClass(), arr.length);

        for (int i = 0; i < arr.length; i++) {
            Array.set(entityPlayerArray, i, arr[i]);
        }

        return entityPlayerArray;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setInfoData(Object info, UUID id, int ping, Object component) {
        try {
            for (Object infoData : (List<Object>) ClazzContainer.getInfoList().get(info)) {
                GameProfile profile = ClazzContainer.getPlayerInfoDataProfile(infoData);

                if (profile == null || !profile.getId().equals(id)) {
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

    private void setEntriesField(Object playerInfoPacket, List<Object> list) {
        try {
            ClazzContainer.getInfoList().set(playerInfoPacket, list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void createBoardTeam(String teamName, Player player, boolean followNameTagVisibility) {
        Object newTeamPacket = null, scoreTeam = null;

        try {
            if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
                scoreTeam = ClazzContainer.getScoreboardTeamConstructor().newInstance(ClazzContainer.getScoreboardConstructor().newInstance(), teamName);

                @SuppressWarnings("unchecked")
                Collection<String> playerNameSet = (Collection<String>) ClazzContainer.getPlayerNameSetMethod().invoke(scoreTeam);
                playerNameSet.add(player.getName());

                ClazzContainer.getScoreboardTeamNames().set(scoreTeam, playerNameSet);
            } else {
                newTeamPacket = ClazzContainer.getPacketPlayOutScoreboardTeamConstructor().newInstance();

                Object teamNameComponent = ReflectionUtils.asComponent(teamName);

                ClazzContainer.getPacketScoreboardTeamName().set(newTeamPacket, ServerVersion.isCurrentLower(ServerVersion.v1_17_R1) ?
                        teamName : teamNameComponent);
                ClazzContainer.getPacketScoreboardTeamMode().set(newTeamPacket, 0);
                ClazzContainer.getScoreboardTeamDisplayName().set(newTeamPacket, ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R1)
                        ? teamNameComponent : teamName);
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
                        Object visibility = ClazzContainer.getNameTagVisibilityByNameMethod().invoke(null, optionName);

                        if (visibility != null) {
                            ClazzContainer.getScoreboardTeamSetNameTagVisibility().invoke(scoreTeam, visibility);
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

            if (tagTeams.isEmpty()) {
                sendPacket(getPlayerHandle(player), newTeamPacket);
                return;
            }

            for (TagTeam tagTeam : tagTeams) {
                if (!tagTeam.playerName.equals(player.getName())) {
                    continue;
                }

                ClazzContainer.getScoreboardTeamSetDisplayName().invoke(tagTeam.scoreboardTeam, tagTeam.scoreboardTeamDisplayNameMethod.invoke(tagTeam.scoreboardTeam));
                ClazzContainer.getScoreboardTeamSetNameTagVisibility().invoke(tagTeam.scoreboardTeam, tagTeam.scoreboardTeamNameTagVisibilityMethod.invoke(tagTeam.scoreboardTeam));

                Object handle = getPlayerHandle(player);

                sendPacket(handle, newTeamPacket);
                sendPacket(handle, ClazzContainer.scoreboardTeamPacketByAction(tagTeam.scoreboardTeam, 0));
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object unregisterBoardTeam(String teamName) {
        try {

            // We use indexed loop to prevent concurrent modification exception
            for (int i = 0; i < playerTeams.size(); i++) {
                Object team = playerTeams.get(i);
                Object playerTeamName = playerTeamName(team);

                if (playerTeamName.equals(teamName)) {
                    if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
                        return ClazzContainer.scoreboardTeamPacketByAction(team, 1);
                    }

                    Object oldTeamPacket = ClazzContainer.getPacketPlayOutScoreboardTeamConstructor().newInstance();

                    ClazzContainer.getPacketScoreboardTeamName().set(oldTeamPacket, playerTeamName);
                    ClazzContainer.getPacketScoreboardTeamMode().set(oldTeamPacket, 1);

                    return oldTeamPacket;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private Object playerTeamName(Object team) throws IllegalAccessException {
        if (playerTeamNameField == null) {
            (playerTeamNameField = fieldByNameAndType(team.getClass(), String.class, "d", "a", "e", "i")).setAccessible(true);
        }

        return playerTeamNameField.get(team);
    }

    private Field fieldByNameAndType(Class<?> where, Class<?> type, String... names) {
        for (Field field : where.getDeclaredFields()) {
            if (type == null || field.getType() == type) {
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
            if (ClazzContainer.getFirstScoreboardObjectiveConstructor().getParameterCount() == 3) {
                return ClazzContainer.getFirstScoreboardObjectiveConstructor().newInstance(null, objectiveName, ClazzContainer.getiScoreboardCriteriaDummy());
            }

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
        return ClazzContainer.newInstanceOfPacketPlayOutScoreboardScore(ClazzContainer.getEnumScoreboardActionChange(), objectiveName, scoreName, score);
    }

    @Override
    public Object removeScoreboardScorePacket(String objectiveName, String scoreName, int score) {
        return ClazzContainer.newInstanceOfPacketPlayOutScoreboardScore(ClazzContainer.getEnumScoreboardActionRemove(), objectiveName, scoreName, score);
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

    private final class PacketReceivingListener extends io.netty.channel.ChannelDuplexHandler {

        private final UUID listenerPlayerId;
        private final List<Class<?>> classesToListen;
        private Method scoreboardHandle;

        public PacketReceivingListener(UUID listenerPlayerId, List<Class<?>> classesToListen) {
            this.listenerPlayerId = listenerPlayerId;
            this.classesToListen = classesToListen;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, io.netty.channel.ChannelPromise promise) throws Exception {
            Class<?> receivingClass = msg.getClass();

            for (Class<?> cl : classesToListen) {
                if (cl != receivingClass) {
                    continue;
                }

                // Temporal and disgusting solution to fix players name tag overwriting
                if (cl == ClazzContainer.packetPlayOutScoreboardTeam()) {
                    Collection<Object> players = (Collection<Object>) ClazzContainer.getScoreboardTeamNames().get(msg);

                    if (!players.isEmpty()) {
                        if (ClazzContainer.getPacketScoreboardTeamParametersMethod() == null) {
                            Object nameTagVisibility = ClazzContainer.getNameTagVisibility().get(msg);

                            if (nameTagVisibility == null) {
                                nameTagVisibility = ClazzContainer.getNameTagVisibilityAlways();
                            }

                            if (nameTagVisibility == ClazzContainer.getNameTagVisibilityNever()) {
                                return;
                            }

                            String prefix, suffix;
                            try {
                                prefix = (String) ClazzContainer.getPacketScoreboardTeamPrefix().get(msg);
                                suffix = (String) ClazzContainer.getPacketScoreboardTeamSuffix().get(msg);
                            } catch (ClassCastException ex) {
                                prefix = (String) ClazzContainer.getiChatBaseComponentGetStringMethod().invoke(ClazzContainer.getPacketScoreboardTeamPrefix().get(msg));
                                suffix = (String) ClazzContainer.getiChatBaseComponentGetStringMethod().invoke(ClazzContainer.getPacketScoreboardTeamSuffix().get(msg));
                            }

                            if (!prefix.isEmpty() || !suffix.isEmpty()) {
                                String playerName = (String) players.iterator().next();

                                for (TagTeam team : tagTeams) {
                                    if (team.playerName.equals(playerName)) {
                                        return;
                                    }
                                }

                                Player player = Bukkit.getPlayer(playerName);

                                if (player == null) {
                                    return;
                                }

                                Object chatFormat = ClazzContainer.getPacketScoreboardTeamChatFormatColorField().get(msg);
                                try {
                                    chatFormat = ClazzContainer.getEnumChatFormatByIntMethod().invoke(null, chatFormat);
                                } catch (Throwable ignored) {
                                }

                                org.bukkit.scoreboard.Scoreboard scoreboard = player.getScoreboard();

                                if (scoreboardHandle == null) {
                                    scoreboardHandle = scoreboard.getClass().getDeclaredMethod("getHandle");
                                }

                                Object scoreboardTeam = ClazzContainer.getScoreboardTeamConstructor().newInstance(scoreboardHandle.invoke(scoreboard),
                                        ClazzContainer.getPacketScoreboardTeamName().get(msg));

                                ClazzContainer.getScoreboardTeamSetPrefix().invoke(scoreboardTeam, prefix);
                                ClazzContainer.getScoreboardTeamSetSuffix().invoke(scoreboardTeam, suffix);
                                ClazzContainer.getScoreboardTeamSetNameTagVisibility().invoke(scoreboardTeam, nameTagVisibility);
                                ClazzContainer.getScoreboardTeamSetChatFormat().invoke(scoreboardTeam, chatFormat);
                                ((Collection<String>) ClazzContainer.getPlayerNameSetMethod().invoke(scoreboardTeam)).add(playerName);
                                tagTeams.add(new TagTeam(playerName, scoreboardTeam));
                            }
                        } else {
                            ((Optional<?>) ClazzContainer.getPacketScoreboardTeamParametersMethod().invoke(msg)).ifPresent(packetTeam -> {
                                try {
                                    Object nameTagVisibility = ClazzContainer.getNameTagVisibilityByNameMethod().invoke(packetTeam,
                                            ClazzContainer.getParametersNameTagVisibility().invoke(packetTeam));

                                    if (nameTagVisibility == null) {
                                        nameTagVisibility = ClazzContainer.getNameTagVisibilityAlways();
                                    }

                                    if (nameTagVisibility == ClazzContainer.getNameTagVisibilityNever()) {
                                        return;
                                    }

                                    String prefix = (String) ClazzContainer.getiChatBaseComponentGetStringMethod().invoke(ClazzContainer.getParametersTeamPrefix()
                                            .invoke(packetTeam));
                                    String suffix = (String) ClazzContainer.getiChatBaseComponentGetStringMethod().invoke(ClazzContainer.getParametersTeamSuffix()
                                            .invoke(packetTeam));

                                    if (!prefix.isEmpty() || !suffix.isEmpty()) {
                                        String playerName = (String) players.iterator().next();

                                        for (TagTeam team : tagTeams) {
                                            if (team.playerName.equals(playerName)) {
                                                return;
                                            }
                                        }

                                        Player player = Bukkit.getPlayer(playerName);

                                        if (player == null) {
                                            return;
                                        }

                                        org.bukkit.scoreboard.Scoreboard scoreboard = player.getScoreboard();

                                        if (scoreboardHandle == null) {
                                            scoreboardHandle = scoreboard.getClass().getDeclaredMethod("getHandle");
                                        }

                                        Object scoreboardTeam = ClazzContainer.getScoreboardTeamConstructor().newInstance(scoreboardHandle.invoke(scoreboard),
                                                ClazzContainer.getScoreboardTeamName().invoke(packetTeam));

                                        ClazzContainer.getScoreboardTeamSetPrefix().invoke(scoreboardTeam, prefix);
                                        ClazzContainer.getScoreboardTeamSetSuffix().invoke(scoreboardTeam, suffix);
                                        ClazzContainer.getScoreboardTeamSetNameTagVisibility().invoke(scoreboardTeam, nameTagVisibility);
                                        ClazzContainer.getScoreboardTeamSetChatFormat().invoke(scoreboardTeam, ClazzContainer.getScoreboardTeamColor().invoke(packetTeam));
                                        ((Collection<String>) ClazzContainer.getPlayerNameSetMethod().invoke(scoreboardTeam)).add(playerName);
                                        tagTeams.add(new TagTeam(playerName, scoreboardTeam));
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            });
                        }
                    }

                    super.write(ctx, msg, promise);
                    return;
                }

                if (ClazzContainer.getActionField().get(msg) == ClazzContainer.getEnumUpdateGameMode()) {
                    Player player = Bukkit.getPlayer(listenerPlayerId);

                    if (player == null) {
                        break;
                    }

                    Object updatePacket = ClazzContainer.getPlayOutPlayerInfoConstructor().newInstance(ClazzContainer.getUpdateLatency(), new Object[0]);
                    List<Object> players = new ArrayList<>();

                    for (Object entry : (List<Object>) ClazzContainer.getInfoList().get(msg)) {
                        if (ClazzContainer.getPlayerInfoDataGameMode().get(entry) != ClazzContainer.getGameModeSpectator()) {
                            continue;
                        }

                        GameProfile profile = ClazzContainer.getPlayerInfoDataProfile(entry);

                        if (profile == null || profile.getId().equals(listenerPlayerId)) {
                            continue;
                        }

                        try {
                            int ping = ClazzContainer.getPlayerInfoDataPing().getInt(entry);
                            Object component = ClazzContainer.getPlayerInfoDisplayName().get(entry);

                            if (ClazzContainer.getPlayerInfoDataConstructor().getParameterCount() == 5) {
                                if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1)) {
                                    players.add(ClazzContainer.getPlayerInfoDataConstructor().newInstance(profile, ping, ClazzContainer.getGameModeSurvival(), component, null));
                                } else {
                                    players.add(ClazzContainer.getPlayerInfoDataConstructor().newInstance(msg, profile, ping, ClazzContainer.getGameModeSurvival(), component));
                                }
                            } else {
                                players.add(ClazzContainer.getPlayerInfoDataConstructor().newInstance(profile, ping, ClazzContainer.getGameModeSurvival(), component));
                            }
                        } catch (IllegalAccessException | InstantiationException |
                                 java.lang.reflect.InvocationTargetException e) {
                            e.printStackTrace();
                        }

                        setEntriesField(updatePacket, players);
                        sendPacket(player, updatePacket);
                    }
                }

                break;
            }

            super.write(ctx, msg, promise);
        }
    }

    private static class TagTeam {

        public final String playerName;
        public final Object scoreboardTeam;
        protected Method scoreboardTeamDisplayNameMethod, scoreboardTeamNameTagVisibilityMethod;

        public TagTeam(String playerName, Object scoreboardTeam) {
            this.playerName = playerName;
            this.scoreboardTeam = scoreboardTeam;

            Class<?> clazz = scoreboardTeam.getClass();

            try {
                scoreboardTeamDisplayNameMethod = clazz.getDeclaredMethod("c");
                scoreboardTeamNameTagVisibilityMethod = clazz.getDeclaredMethod("j");
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public boolean equals(Object other) {
            return other != null && getClass() == other.getClass() && playerName.equals(((TagTeam) other).playerName);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(playerName);
        }
    }
}
