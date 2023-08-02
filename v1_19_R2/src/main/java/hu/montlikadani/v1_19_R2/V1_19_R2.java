package hu.montlikadani.v1_19_R2;

import com.mojang.authlib.GameProfile;
import hu.montlikadani.api.IPacketNM;
import io.netty.channel.ChannelHandlerContext;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.chat.IChatBaseComponent;
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
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R2.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class V1_19_R2 implements IPacketNM {

    private Field entriesField;

    private final Scoreboard scoreboard = new Scoreboard();

    private final Set<TagTeam> tagTeams = new HashSet<>();

    private final List<PacketReceivingListener> packetReceivingListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Override
    public void packetListeningAllowed(Player player) {
        PacketReceivingListener receivingListener = listenerByPlayer(player.getUniqueId());

        if (receivingListener != null) {
            receivingListener.packetListeningAllowed = !receivingListener.packetListeningAllowed;
        }
    }

    private PacketReceivingListener listenerByPlayer(UUID playerId) {
        for (PacketReceivingListener receivingListener : packetReceivingListeners) {
            if (receivingListener.listenerPlayerId.equals(playerId)) {
                return receivingListener;
            }
        }

        return null;
    }

    @Override
    public void sendPacket(Player player, Object packet) {
        getPlayerHandle(player).b.a((Packet<?>) packet);
    }

    private void sendPacket(EntityPlayer player, Packet<?> packet) {
        player.b.a(packet);
    }

    @Override
    public void addPlayerChannelListener(Player player, List<Class<?>> classesToListen) {
        UUID playerId = player.getUniqueId();

        if (listenerByPlayer(playerId) != null) {
            return;
        }

        EntityPlayer entityPlayer = getPlayerHandle(player);

        if (entityPlayer.b.b.m.pipeline().get(PACKET_INJECTOR_NAME) == null) {
            PacketReceivingListener packetReceivingListener = new PacketReceivingListener(playerId, classesToListen);

            packetReceivingListeners.add(packetReceivingListener);

            try {
                entityPlayer.b.b.m.pipeline().addBefore("packet_handler", PACKET_INJECTOR_NAME, packetReceivingListener);
            } catch (java.util.NoSuchElementException ex) {
                // packet_handler not exists, sure then, ignore
            }
        }
    }

    @Override
    public void removePlayerChannelListener(Player player) {
        EntityPlayer entityPlayer = getPlayerHandle(player);

        if (entityPlayer.b.b.m.pipeline().get(PACKET_INJECTOR_NAME) != null) {
            entityPlayer.b.b.m.pipeline().remove(PACKET_INJECTOR_NAME);
        }

        packetReceivingListeners.removeIf(pr -> pr.listenerPlayerId.equals(player.getUniqueId()));
    }

    @Override
    public EntityPlayer getPlayerHandle(Player player) {
        return ((org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer) player).getHandle();
    }

    @Override
    public IChatBaseComponent fromJson(String json) {
        return IChatBaseComponent.ChatSerializer.a(json);
    }

    @Override
    public void sendTabTitle(Player player, Object header, Object footer) {
        sendPacket(player, new PacketPlayOutPlayerListHeaderFooter((IChatBaseComponent) header, (IChatBaseComponent) footer));
    }

    @Override
    public EntityPlayer getNewEntityPlayer(GameProfile profile) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();

        return new EntityPlayer(server, server.C(), profile);
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

        return new ClientboundPlayerInfoUpdatePacket(java.util.EnumSet.of(ClientboundPlayerInfoUpdatePacket.a.a,
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
            players.add(((EntityPlayer) one).fD().getId());
        }

        return new ClientboundPlayerInfoRemovePacket(players);
    }

    @Override
    public void setInfoData(Object info, UUID id, int ping, Object component) {
        ClientboundPlayerInfoUpdatePacket update = (ClientboundPlayerInfoUpdatePacket) info;

        for (ClientboundPlayerInfoUpdatePacket.b playerInfo : update.c()) {
            if (playerInfo.a().equals(id)) {
                setEntriesField(update, Collections.singletonList(new ClientboundPlayerInfoUpdatePacket.b(playerInfo.a(), playerInfo.b(), playerInfo.c(),
                        ping == -2 ? playerInfo.d() : ping, playerInfo.e(), (IChatBaseComponent) component, playerInfo.g())));
                break;
            }
        }
    }

    private void setEntriesField(ClientboundPlayerInfoUpdatePacket playerInfoPacket, List<ClientboundPlayerInfoUpdatePacket.b> list) {
        try {

            // Entries list is immutable, so use reflection to bypass
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
        ScoreboardTeam playerTeam = scoreboard.g(teamName);

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

        EntityPlayer handle = getPlayerHandle(player);

        if (tagTeams.isEmpty()) {
            sendPacket(handle, PacketPlayOutScoreboardTeam.a(playerTeam, true));
        } else {
            for (TagTeam tagTeam : tagTeams) {
                if (!tagTeam.playerName.equals(player.getName())) {
                    continue;
                }

                tagTeam.scoreboardTeam.a(playerTeam.c());
                tagTeam.scoreboardTeam.a(playerTeam.j());

                sendPacket(handle, PacketPlayOutScoreboardTeam.a(playerTeam, true));
                sendPacket(handle, PacketPlayOutScoreboardTeam.a(tagTeam.scoreboardTeam, true));
                break;
            }
        }
    }

    @Override
    public PacketPlayOutScoreboardTeam unregisterBoardTeam(String teamName) {
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
    public ScoreboardObjective createObjectivePacket(String objectiveName, Object nameComponent) {
        return new ScoreboardObjective(null, objectiveName, IScoreboardCriteria.a, (IChatBaseComponent) nameComponent, IScoreboardCriteria.EnumScoreboardHealthDisplay.a);
    }

    @Override
    public PacketPlayOutScoreboardObjective scoreboardObjectivePacket(Object objective, int mode) {
        return new PacketPlayOutScoreboardObjective((ScoreboardObjective) objective, mode);
    }

    @Override
    public PacketPlayOutScoreboardDisplayObjective scoreboardDisplayObjectivePacket(Object objective, int slot) {
        return new PacketPlayOutScoreboardDisplayObjective(slot, (ScoreboardObjective) objective);
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
        return new ScoreboardObjective(null, objectiveName, IScoreboardCriteria.a, (IChatBaseComponent) nameComponent, IScoreboardCriteria.EnumScoreboardHealthDisplay.b);
    }

    private final class PacketReceivingListener extends io.netty.channel.ChannelDuplexHandler {

        private final UUID listenerPlayerId;
        private final List<Class<?>> classesToListen;

        private boolean packetListeningAllowed = true;

        public PacketReceivingListener(UUID listenerPlayerId, List<Class<?>> classesToListen) {
            this.listenerPlayerId = listenerPlayerId;
            this.classesToListen = classesToListen;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, io.netty.channel.ChannelPromise promise) throws Exception {
            if (!packetListeningAllowed) {
                super.write(ctx, msg, promise);
                return;
            }

            Class<?> receivingClass = msg.getClass();

            for (Class<?> cl : classesToListen) {
                if (cl != receivingClass) {
                    continue;
                }

                // Temporal and disgusting solution to fix players name tag overwriting
                if (cl == PacketPlayOutScoreboardTeam.class) {
                    PacketPlayOutScoreboardTeam packetScoreboardTeam = (PacketPlayOutScoreboardTeam) msg;

                    if (packetScoreboardTeam.e() != null && !packetScoreboardTeam.e().isEmpty()) {
                        packetScoreboardTeam.f().ifPresent(packetTeam -> {
                            ScoreboardTeamBase.EnumNameTagVisibility enumNameTagVisibility = ScoreboardTeamBase.EnumNameTagVisibility.a(packetTeam.d());

                            if (enumNameTagVisibility == null) {
                                enumNameTagVisibility = ScoreboardTeamBase.EnumNameTagVisibility.a;
                            }

                            if (enumNameTagVisibility == ScoreboardTeamBase.EnumNameTagVisibility.b) {
                                return;
                            }

                            IChatBaseComponent prefix = packetTeam.f();
                            IChatBaseComponent suffix = packetTeam.g();

                            if ((prefix != null && !prefix.getString().isEmpty()) || (suffix != null && !suffix.getString().isEmpty())) {
                                String playerName = packetScoreboardTeam.e().iterator().next();

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

                                ScoreboardTeam scoreboardTeam = new ScoreboardTeam(((org.bukkit.craftbukkit.v1_19_R2.scoreboard.CraftScoreboard) player.getScoreboard()).getHandle(),
                                        packetTeam.a().getString());
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

                    super.write(ctx, msg, promise);
                    return;
                }

                if (cl == ClientboundPlayerInfoUpdatePacket.class) {
                    ClientboundPlayerInfoUpdatePacket playerInfoPacket = (ClientboundPlayerInfoUpdatePacket) msg;

                    if (!playerInfoPacket.b().contains(ClientboundPlayerInfoUpdatePacket.a.c)) {
                        break;
                    }

                    Player player = Bukkit.getPlayer(listenerPlayerId);

                    if (player == null) {
                        break;
                    }

                    ClientboundPlayerInfoUpdatePacket updatePacket = new ClientboundPlayerInfoUpdatePacket(
                            java.util.EnumSet.of(ClientboundPlayerInfoUpdatePacket.a.c), java.util.Collections.emptyList());
                    List<ClientboundPlayerInfoUpdatePacket.b> players = new ArrayList<>();

                    for (ClientboundPlayerInfoUpdatePacket.b entry : playerInfoPacket.c()) {
                        if (entry.e() == EnumGamemode.d && !entry.a().equals(listenerPlayerId)) {
                            players.add(new ClientboundPlayerInfoUpdatePacket.b(entry.a(), entry.b(), entry.c(), entry.d(),
                                    EnumGamemode.a, entry.f(), entry.g()));
                        }
                    }

                    setEntriesField(updatePacket, players);
                    sendPacket(player, updatePacket);
                    break;
                }
            }

            super.write(ctx, msg, promise);
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
}
