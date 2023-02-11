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
import java.util.UUID;

public final class LegacyVersion implements IPacketNM {

    private Method playerHandleMethod, sendPacketMethod, getHandleWorldMethod, getServerMethod, interactGameModeMethod, gameProfileMethod;
    private Field playerConnectionField, headerField, footerField, listNameField, playerTeamNameField, networkManager, channel, playerLatency,
            interactManagerField;
    private Constructor<?> playerListHeaderFooterConstructor, entityPlayerConstructor, interactManagerConstructor, packetPlayOutAnimation;
    private Class<?> minecraftServer, interactManager, craftServerClass;

    private final List<Object> playerTeams = new ArrayList<>();

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
    public void addPlayerChannelListener(Player player, Class<?>... classesToListen) {
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
            Object playerConnection = playerConnectionField.get(getPlayerHandle(player));

            if (sendPacketMethod == null) {
                sendPacketMethod = playerConnection.getClass().getDeclaredMethod(ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_18_R1) ? "a" : "sendPacket",
                        ClazzContainer.getPacket());
            }

            sendPacketMethod.invoke(playerConnection, packet);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
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
        try {
            sendPacket(source, removeEntityPlayers(players.stream().map(this::getPlayerHandle).toArray()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void appendPlayerWithoutListed(Player source) {
        Object player = getPlayerHandle(source);

        try {
            Object updatePacket = ClazzContainer.getPlayOutPlayerInfoConstructor().newInstance(ClazzContainer.getAddPlayer(), toArray(player));

            setEntriesField(updatePacket, () -> {
                if (playerLatency == null) {
                    playerLatency = fieldByNameAndType(player.getClass(), int.class, "latency", "ping", "g", "f", "e");
                }

                if (gameProfileMethod == null) {
                    gameProfileMethod = methodByTypeAndName(player.getClass().getSuperclass(), GameProfile.class, "getProfile", "fi", "getGameProfile",
                            "cS", "dH", "fp", "da", "cK", "eA", "ez", "ed", "do", "da", "cP", "cL"); // This is why I hate
                }

                if (interactManagerField == null) {
                    interactManagerField = fieldByNameAndType(player.getClass(), interactManager, "playerInteractManager", "d", "c", "gameMode");
                }

                try {
                    Object interactManagerInstance = interactManagerField.get(player);

                    if (interactGameModeMethod == null) {
                        interactGameModeMethod = methodByTypeAndName(interactManagerInstance.getClass(), ClazzContainer.getGameModeSurvival().getClass(),
                                "b", "getGameModeForPlayer", "getGameMode");
                    }

                    int ping = playerLatency.getInt(player);
                    GameProfile profile = (GameProfile) gameProfileMethod.invoke(player);
                    Object gameMode = interactGameModeMethod.invoke(interactManagerInstance);

                    if (ClazzContainer.getPlayerInfoDataConstructor().getParameterCount() == 5) {
                        if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1)) {
                            return ClazzContainer.getPlayerInfoDataConstructor().newInstance(profile, ping, gameMode,
                                    ReflectionUtils.EMPTY_COMPONENT, null);
                        }

                        return ClazzContainer.getPlayerInfoDataConstructor().newInstance(updatePacket, profile, ping, gameMode, ReflectionUtils.EMPTY_COMPONENT);
                    }

                    return ClazzContainer.getPlayerInfoDataConstructor().newInstance(profile, ping, gameMode, ReflectionUtils.EMPTY_COMPONENT);
                } catch (IllegalAccessException | InstantiationException |
                         java.lang.reflect.InvocationTargetException e) {
                    e.printStackTrace();
                }

                return null;
            });

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

    private Method methodByTypeAndName(Class<?> from, Class<?> type, String... names) {
        Method[] methods = from.getDeclaredMethods();

        for (Method method : methods) {
            if (method.getReturnType() == type) {
                for (String name : names) {
                    if (method.getName().equals(name)) {
                        return method;
                    }
                }
            }
        }

        // If not found by name just return the first method which matches the type
        for (Method method : methods) {
            if (method.getReturnType() == type) {
                return method;
            }
        }

        return null;
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

    private void setEntriesField(Object playerInfoPacket, java.util.function.Supplier<Object> supplier) {
        try {
            ClazzContainer.getInfoList().set(playerInfoPacket, Collections.singletonList(supplier.get()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Object createBoardTeam(Object teamNameComponent, String teamName, Player player, boolean followNameTagVisibility) {
        Object newTeamPacket = null, scoreTeam = null;

        try {
            if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
                scoreTeam = ClazzContainer.getScoreboardTeamConstructor().newInstance(ClazzContainer.getScoreboardConstructor().newInstance(), teamName);

                @SuppressWarnings("unchecked")
                Collection<String> playerNameSet = (Collection<String>) ClazzContainer.getPlayerNameSetMethod().invoke(scoreTeam);
                playerNameSet.add(player.getName());

                ClazzContainer.getScoreboardTeamNames().set(scoreTeam, playerNameSet);
                ClazzContainer.getScoreboardTeamSetDisplayName().invoke(scoreTeam, teamNameComponent);
            } else {
                newTeamPacket = ClazzContainer.getPacketPlayOutScoreboardTeamConstructor().newInstance();

                ClazzContainer.getScoreboardTeamName().set(newTeamPacket, ServerVersion.isCurrentLower(ServerVersion.v1_17_R1) ?
                        teamName : teamNameComponent);
                ClazzContainer.getScoreboardTeamMode().set(newTeamPacket, 0);
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

            // We use indexed loop to prevent concurrent modification exception
            for (int i = 0; i < playerTeams.size(); i++) {
                Object team = playerTeams.get(i);

                if (playerTeamNameField == null) {
                    (playerTeamNameField = fieldByNameAndType(team.getClass(), String.class, "d", "a", "e", "i")).setAccessible(true);
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

    private final class PacketReceivingListener extends io.netty.channel.ChannelDuplexHandler {

        private final UUID listenerPlayerId;
        private final Class<?>[] classesToListen;

        public PacketReceivingListener(UUID listenerPlayerId, Class<?>... classesToListen) {
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

                if (ClazzContainer.getActionField().get(msg) == ClazzContainer.getEnumUpdateGameMode()) {
                    for (Object entry : (List<Object>) ClazzContainer.getInfoList().get(msg)) {
                        if (ClazzContainer.getPlayerInfoDataGameMode().get(entry) != ClazzContainer.getGameModeSpectator()) {
                            continue;
                        }

                        GameProfile profile = ClazzContainer.getPlayerInfoDataProfile(entry);

                        if (profile.getId().equals(listenerPlayerId)) {
                            continue;
                        }

                        setEntriesField(msg, () -> {
                            try {
                                int ping = ClazzContainer.getPlayerInfoDataPing().getInt(entry);
                                Object component = ClazzContainer.getPlayerInfoDisplayName().get(entry);

                                if (ClazzContainer.getPlayerInfoDataConstructor().getParameterCount() == 5) {
                                    if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1)) {
                                        return ClazzContainer.getPlayerInfoDataConstructor().newInstance(profile, ping, ClazzContainer.getGameModeSurvival(), component, null);
                                    }

                                    return ClazzContainer.getPlayerInfoDataConstructor().newInstance(msg, profile, ping, ClazzContainer.getGameModeSurvival(), component);
                                }

                                return ClazzContainer.getPlayerInfoDataConstructor().newInstance(profile, ping, ClazzContainer.getGameModeSurvival(), component);
                            } catch (IllegalAccessException | InstantiationException |
                                     java.lang.reflect.InvocationTargetException e) {
                                e.printStackTrace();
                            }

                            return null;
                        });
                    }
                }

                break;
            }

            super.write(ctx, msg, promise);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            super.channelRead(ctx, msg);
        }
    }
}
