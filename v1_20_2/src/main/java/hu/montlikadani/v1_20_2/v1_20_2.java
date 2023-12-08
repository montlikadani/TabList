package hu.montlikadani.v1_20_2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelMetadata;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardScore;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ScoreboardServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class v1_20_2 implements hu.montlikadani.api.IPacketNM {

    private Field entriesField, playerNetworkManagerField;

    private final Scoreboard scoreboard = new Scoreboard();

    private final Set<TagTeam> tagTeams = new HashSet<>();

    @Override
    public void sendPacket(Player player, Object packet) {
        getPlayerHandle(player).c.b((Packet<?>) packet);
    }

    private void sendPacket(EntityPlayer player, Packet<?> packet) {
        player.c.b(packet);
    }

    @Override
    public void addPlayerChannelListener(Player player, List<Class<?>> classesToListen) {
        Channel channel = playerChannel(getPlayerHandle(player).c);

        if (channel != null && channel.pipeline().get(PACKET_INJECTOR_NAME) == null) {
            try {
                channel.pipeline().addBefore("packet_handler", PACKET_INJECTOR_NAME,
                        new PacketReceivingListener(player.getUniqueId(), classesToListen));
            } catch (NoSuchElementException ignored) {
            }
        }
    }

    private Channel playerChannel(PlayerConnection connection) {
        if (playerNetworkManagerField == null && (playerNetworkManagerField = fieldByType(connection.getClass().getSuperclass(),
                NetworkManager.class)) == null) {
            return null;
        }

        try {
            return ((NetworkManager) playerNetworkManagerField.get(connection)).n;
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private Field fieldByType(Class<?> where, Class<?> type) {
        for (Field field : where.getDeclaredFields()) {
            if (field.getType() == type) {
                field.setAccessible(true);
                return field;
            }
        }

        return null;
    }

    @Override
    public void removePlayerChannelListener(Player player) {
        Channel channel = playerChannel(getPlayerHandle(player).c);

        if (channel != null) {
            try {
                channel.pipeline().remove(PACKET_INJECTOR_NAME);
            } catch (NoSuchElementException ignored) {
            }
        }
    }

    @Override
    public EntityPlayer getPlayerHandle(Player player) {
        return ((org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer) player).getHandle();
    }

    @Override
    public IChatBaseComponent fromJson(String json) {
        return IChatBaseComponent.ChatSerializer.a(json);
    }

    @Override
    public void sendTabTitle(Player player, Object header, Object footer) {
        sendPacket(player, new PacketPlayOutPlayerListHeaderFooter((IChatBaseComponent) header, (IChatBaseComponent) footer));
    }

    private MinecraftServer minecraftServer() {
        return ((org.bukkit.craftbukkit.v1_20_R2.CraftServer) Bukkit.getServer()).getServer();
    }

    @Override
    public EntityPlayer getNewEntityPlayer(com.mojang.authlib.GameProfile profile) {
        MinecraftServer server = minecraftServer();
        ClientInformation clientInfo = ClientInformation.a();
        EntityPlayer entityPlayer = new EntityPlayer(server, server.D(), profile, clientInfo);

        entityPlayer.c = new EmptyPacketListener(server, new EmptyConnection(EnumProtocolDirection.b), entityPlayer,
                new CommonListenerCookie(profile, 0, clientInfo));

        return entityPlayer;
    }

    @Override
    public double[] serverTps() {
        return minecraftServer().recentTps;
    }

    @Override
    public ClientboundPlayerInfoUpdatePacket updateDisplayNamePacket(Object entityPlayer, Object component, boolean listName) {
        if (listName) {
            setListName(entityPlayer, component);
        }

        return new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.a.f, (EntityPlayer) entityPlayer);
    }

    @Override
    public void setListName(Object entityPlayer, Object component) {
        ((EntityPlayer) entityPlayer).listName = (IChatBaseComponent) component;
    }

    @Override
    public ClientboundPlayerInfoUpdatePacket newPlayerInfoUpdatePacketAdd(Object... entityPlayers) {
        List<EntityPlayer> players = new ArrayList<>(entityPlayers.length);

        for (Object one : entityPlayers) {
            players.add((EntityPlayer) one);
        }

        return new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.a.a,
                ClientboundPlayerInfoUpdatePacket.a.d, ClientboundPlayerInfoUpdatePacket.a.e, ClientboundPlayerInfoUpdatePacket.a.f), players);
    }

    @Override
    public ClientboundPlayerInfoUpdatePacket updateLatency(Object entityPlayer) {
        return new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.a.e, (EntityPlayer) entityPlayer);
    }

    @Override
    public ClientboundPlayerInfoRemovePacket removeEntityPlayers(Object... entityPlayers) {
        List<UUID> players = new ArrayList<>(entityPlayers.length);

        for (Object one : entityPlayers) {
            players.add(((EntityPlayer) one).fQ().getId());
        }

        return new ClientboundPlayerInfoRemovePacket(players);
    }

    @Override
    public void setInfoData(Object info, UUID id, int ping, Object component) {
        ClientboundPlayerInfoUpdatePacket update = (ClientboundPlayerInfoUpdatePacket) info;

        for (ClientboundPlayerInfoUpdatePacket.b playerInfo : update.d()) {
            if (playerInfo.a().equals(id)) {
                setEntriesField(update, Collections.singletonList(new ClientboundPlayerInfoUpdatePacket.b(playerInfo.a(), playerInfo.b(), playerInfo.c(),
                        ping == -2 ? playerInfo.d() : ping, playerInfo.e(), (IChatBaseComponent) component, playerInfo.g())));
                break;
            }
        }
    }

    private void setEntriesField(ClientboundPlayerInfoUpdatePacket playerInfoPacket, List<ClientboundPlayerInfoUpdatePacket.b> list) {
        try {

            // Entries list is unmodifiable, so use reflection to bypass
            if (entriesField == null) {
                entriesField = playerInfoPacket.getClass().getDeclaredField("b");
                entriesField.setAccessible(true);
            }

            entriesField.set(playerInfoPacket, list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createBoardTeam(String teamName, Player player, boolean followNameTagVisibility) {
        ScoreboardTeam playerTeam = scoreboard.e(teamName);

        scoreboard.a(player.getName(), playerTeam);

        if (followNameTagVisibility) {
            ScoreboardTeam.EnumNameTagVisibility visibility = null;

            for (Team team : player.getScoreboard().getTeams()) {
                Team.OptionStatus optionStatus = team.getOption(Team.Option.NAME_TAG_VISIBILITY);

                switch (optionStatus) {
                    case FOR_OTHER_TEAMS:
                        visibility = ScoreboardTeam.EnumNameTagVisibility.c;
                        break;
                    case FOR_OWN_TEAM:
                        visibility = ScoreboardTeam.EnumNameTagVisibility.d;
                        break;
                    default:
                        if (optionStatus != Team.OptionStatus.ALWAYS) {
                            visibility = ScoreboardTeam.EnumNameTagVisibility.b;
                        }

                        break;
                }
            }

            if (visibility != null) {
                playerTeam.a(visibility);
            }
        }

        if (tagTeams.isEmpty()) {
            for (Player one : Bukkit.getOnlinePlayers()) {
                sendPacket(getPlayerHandle(one), PacketPlayOutScoreboardTeam.a(playerTeam, true));
            }
        } else {
            for (TagTeam tagTeam : tagTeams) {
                if (!tagTeam.playerName.equals(player.getName())) {
                    continue;
                }

                tagTeam.scoreboardTeam.a(playerTeam.c());
                tagTeam.scoreboardTeam.a(playerTeam.j());

                for (Player one : Bukkit.getOnlinePlayers()) {
                    EntityPlayer handle = getPlayerHandle(one);

                    sendPacket(handle, PacketPlayOutScoreboardTeam.a(playerTeam, true));
                    sendPacket(handle, PacketPlayOutScoreboardTeam.a(tagTeam.scoreboardTeam, true));
                }

                break;
            }
        }
    }

    @Override
    public PacketPlayOutScoreboardTeam unregisterBoardTeamPacket(String teamName) {
        java.util.Collection<ScoreboardTeam> teams = scoreboard.g();

        synchronized (teams) {
            for (ScoreboardTeam team : new ArrayList<>(teams)) {
                if (team.b().equals(teamName)) {
                    scoreboard.d(team);
                    return PacketPlayOutScoreboardTeam.a(team);
                }
            }
        }

        return null;
    }

    @Override
    public ScoreboardObjective createObjectivePacket(String objectiveName, Object nameComponent,
                                                     ObjectiveFormat objectiveFormat, Object formatComponent) {
        return new ScoreboardObjective(null, objectiveName, IScoreboardCriteria.a, (IChatBaseComponent) nameComponent,
                IScoreboardCriteria.EnumScoreboardHealthDisplay.a);
    }

    @Override
    public PacketPlayOutScoreboardObjective scoreboardObjectivePacket(Object objective, int mode) {
        return new PacketPlayOutScoreboardObjective((ScoreboardObjective) objective, mode);
    }

    @Override
    public PacketPlayOutScoreboardDisplayObjective scoreboardDisplayObjectivePacket(Object objective, int slot) {
        DisplaySlot ds = DisplaySlot.a;

        if (slot != 0) {
            for (DisplaySlot displaySlot : DisplaySlot.values()) {
                if (displaySlot.a() == slot) {
                    ds = displaySlot;
                    break;
                }
            }
        }

        return new PacketPlayOutScoreboardDisplayObjective(ds, (ScoreboardObjective) objective);
    }

    @Override
    public PacketPlayOutScoreboardScore changeScoreboardScorePacket(String objectiveName, String scoreName, int score) {
        return new PacketPlayOutScoreboardScore(ScoreboardServer.Action.a, objectiveName, scoreName, score);
    }

    @Override
    public PacketPlayOutScoreboardScore removeScoreboardScorePacket(String objectiveName, String scoreName, int score) {
        return new PacketPlayOutScoreboardScore(ScoreboardServer.Action.b, objectiveName, scoreName, score);
    }

    @Override
    public ScoreboardObjective createScoreboardHealthObjectivePacket(String objectiveName, Object nameComponent) {
        return new ScoreboardObjective(null, objectiveName, IScoreboardCriteria.a, (IChatBaseComponent) nameComponent,
                IScoreboardCriteria.EnumScoreboardHealthDisplay.b);
    }

    private final class PacketReceivingListener extends io.netty.channel.ChannelDuplexHandler {

        private final UUID listenerPlayerId;
        private final List<Class<?>> classesToListen;

        public PacketReceivingListener(UUID listenerPlayerId, List<Class<?>> classesToListen) {
            this.listenerPlayerId = listenerPlayerId;
            this.classesToListen = classesToListen;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, io.netty.channel.ChannelPromise promise) throws Exception {
            Class<?> receivingClass = msg.getClass();

            if (!classesToListen.contains(receivingClass)) {
                super.write(ctx, msg, promise);
                return;
            }

            if (receivingClass == PacketPlayOutScoreboardTeam.class) {
                scoreboardTeamPacket((PacketPlayOutScoreboardTeam) msg);
            } else if (receivingClass == ClientboundPlayerInfoUpdatePacket.class) {
                playerInfoUpdatePacket((ClientboundPlayerInfoUpdatePacket) msg);
            }

            super.write(ctx, msg, promise);
        }

        private void playerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket playerInfoPacket) {
            if (!playerInfoPacket.a().contains(ClientboundPlayerInfoUpdatePacket.a.c)) {
                return;
            }

            Player player = Bukkit.getPlayer(listenerPlayerId);

            if (player == null) {
                return;
            }

            for (ClientboundPlayerInfoUpdatePacket.b entry : playerInfoPacket.d()) {
                if (entry.e() == EnumGamemode.d && !entry.a().equals(listenerPlayerId)) {
                    ClientboundPlayerInfoUpdatePacket updatePacket = new ClientboundPlayerInfoUpdatePacket(
                            EnumSet.of(ClientboundPlayerInfoUpdatePacket.a.c), Collections.emptyList());

                    setEntriesField(updatePacket, Collections.singletonList(new ClientboundPlayerInfoUpdatePacket.b(
                            entry.a(), entry.b(), entry.c(), entry.d(), EnumGamemode.a, entry.f(), entry.g())));
                    sendPacket(player, updatePacket);
                }
            }
        }

        private void scoreboardTeamPacket(PacketPlayOutScoreboardTeam packetScoreboardTeam) {

            // Some plugins are using this packet in wrong way and the return value of this method "f" is null
            // which shouldn't be that way but ok, nothing I can do about this only to add an extra condition
            if (packetScoreboardTeam.f() == null || packetScoreboardTeam.f().isEmpty()) {
                return;
            }

            packetScoreboardTeam.g().ifPresent(packetTeam -> {
                ScoreboardTeamBase.EnumNameTagVisibility enumNameTagVisibility = ScoreboardTeamBase.EnumNameTagVisibility.a(packetTeam.d());

                if (enumNameTagVisibility == null) {
                    enumNameTagVisibility = ScoreboardTeamBase.EnumNameTagVisibility.a;
                } else if (enumNameTagVisibility == ScoreboardTeamBase.EnumNameTagVisibility.b) {
                    return;
                }

                IChatBaseComponent prefix = packetTeam.f();
                IChatBaseComponent suffix = packetTeam.g();

                if ((prefix != null && !prefix.getString().isEmpty()) || (suffix != null && !suffix.getString().isEmpty())) {
                    String playerName = packetScoreboardTeam.f().iterator().next();

                    for (TagTeam team : tagTeams) {
                        if (team.playerName.equals(playerName)) {
                            return;
                        }
                    }

                    Player player = Bukkit.getPlayer(playerName);

                    if (player == null) {
                        return;
                    }

                    ScoreboardTeamBase.EnumTeamPush enumTeamPush = ScoreboardTeamBase.EnumTeamPush.a(packetTeam.e());

                    if (enumTeamPush == null) {
                        enumTeamPush = ScoreboardTeamBase.EnumTeamPush.a;
                    }

                    ScoreboardTeam scoreboardTeam = new ScoreboardTeam(((org.bukkit.craftbukkit.v1_20_R2.scoreboard.CraftScoreboard)
                            player.getScoreboard()).getHandle(), packetTeam.a().getString());
                    scoreboardTeam.b(prefix);
                    scoreboardTeam.c(suffix);
                    scoreboardTeam.a(enumNameTagVisibility);
                    scoreboardTeam.a(enumTeamPush);
                    scoreboardTeam.a(packetTeam.c());
                    scoreboardTeam.g().add(playerName);

                    tagTeams.add(new TagTeam(playerName, scoreboardTeam));
                }
            });
        }
    }

    private static class TagTeam {

        public final String playerName;
        public final ScoreboardTeam scoreboardTeam;

        public TagTeam(String playerName, ScoreboardTeam scoreboardTeam) {
            this.playerName = playerName;
            this.scoreboardTeam = scoreboardTeam;
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

    private static class EmptyPacketListener extends PlayerConnection {

        public EmptyPacketListener(MinecraftServer minecraftserver, NetworkManager networkmanager, EntityPlayer entityplayer,
                                   CommonListenerCookie commonlistenercookie) {
            super(minecraftserver, networkmanager, entityplayer, commonlistenercookie);
        }

        @Override
        public void h() {
        }

        @Override
        public void b(Packet<?> packet) {
        }
    }

    private static class EmptyConnection extends NetworkManager {

        public EmptyConnection(EnumProtocolDirection enumprotocoldirection) {
            super(enumprotocoldirection);

            n = new EmptyChannel(null);
            o = new SocketAddress() {
            };
        }

        @Override
        public void c() {
        }

        @Override
        public boolean k() {
            return true;
        }

        @Override
        public void a(Packet packet) {
        }

        @Override
        public void a(Packet packet, net.minecraft.network.PacketSendListener genericfuturelistener) {
        }

        @Override
        public void a(Packet packet, net.minecraft.network.PacketSendListener genericfuturelistener, boolean flag) {
        }

        private Field packetListenerField, disconnectListenerField;

        @Override
        public void a(net.minecraft.network.PacketListener pl) {
            if (packetListenerField == null) {
                try {
                    (packetListenerField = NetworkManager.class.getDeclaredField("q")).setAccessible(true);
                } catch (NoSuchFieldException ex) {
                    ex.printStackTrace();
                }
            }

            try {
                packetListenerField.set(this, pl);
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }

            if (disconnectListenerField == null) {
                try {
                    (disconnectListenerField = NetworkManager.class.getDeclaredField("p")).setAccessible(true);
                } catch (NoSuchFieldException ex) {
                    ex.printStackTrace();
                }
            }

            try {
                disconnectListenerField.set(this, null);
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static class EmptyChannel extends io.netty.channel.AbstractChannel {

        private final ChannelConfig config = new io.netty.channel.DefaultChannelConfig(this);

        protected EmptyChannel(Channel parent) {
            super(parent);
        }

        @Override
        protected AbstractUnsafe newUnsafe() {
            return null;
        }

        @Override
        protected boolean isCompatible(io.netty.channel.EventLoop loop) {
            return false;
        }

        @Override
        protected SocketAddress localAddress0() {
            return null;
        }

        @Override
        protected SocketAddress remoteAddress0() {
            return null;
        }

        @Override
        protected void doBind(SocketAddress localAddress) throws Exception {

        }

        @Override
        protected void doDisconnect() throws Exception {

        }

        @Override
        protected void doClose() throws Exception {

        }

        @Override
        protected void doBeginRead() throws Exception {

        }

        @Override
        protected void doWrite(io.netty.channel.ChannelOutboundBuffer in) throws Exception {

        }

        @Override
        public ChannelConfig config() {
            config.setAutoRead(true);
            return config;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public ChannelMetadata metadata() {
            return new ChannelMetadata(true);
        }
    }
}
