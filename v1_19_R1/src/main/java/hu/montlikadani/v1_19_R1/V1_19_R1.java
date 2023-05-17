package hu.montlikadani.v1_19_R1;

import com.mojang.authlib.GameProfile;
import hu.montlikadani.api.IPacketNM;
import io.netty.channel.ChannelHandlerContext;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.PacketPlayOutAnimation;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo;
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
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class V1_19_R1 implements IPacketNM {

    private Field entriesField;
    private final IChatBaseComponent emptyComponent = IChatBaseComponent.ChatSerializer.a("");

    private final Scoreboard scoreboard = new Scoreboard();

    private final Set<ScoreboardTeam> scoreboardTeams = new HashSet<>();
    private final Set<TagTeam> tagTeams = new HashSet<>();

    @Override
    public void sendPacket(Player player, Object packet) {
        getPlayerHandle(player).b.a((Packet<?>) packet);
    }

    private void sendPacket(EntityPlayer player, Packet<?> packet) {
        player.b.a(packet);
    }

    @Override
    public void addPlayerChannelListener(Player player, List<Class<?>> classesToListen) {
        EntityPlayer entityPlayer = getPlayerHandle(player);

        if (entityPlayer.b.b.m.pipeline().get(PACKET_INJECTOR_NAME) == null) {
            try {
                entityPlayer.b.b.m.pipeline().addBefore("packet_handler", PACKET_INJECTOR_NAME, new PacketReceivingListener(entityPlayer.fy().getId(), classesToListen));
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
    }

    @Override
    public EntityPlayer getPlayerHandle(Player player) {
        return ((org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer) player).getHandle();
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

        return new EntityPlayer(server, server.C(), profile, null);
    }

    @Override
    public void addPlayersToTab(Player source, Player... targets) {
        List<EntityPlayer> players = new ArrayList<>(targets.length);

        for (Player player : targets) {
            players.add(getPlayerHandle(player));
        }

        sendPacket(source, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a, players));
    }

    @Override
    public void removePlayersFromTab(Player source, Collection<? extends Player> players) {
        EntityPlayer entityPlayer = getPlayerHandle(source);

        sendPacket(entityPlayer, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.e,
                players.stream().map(this::getPlayerHandle).collect(Collectors.toList())));
        sendUpdatePacket(entityPlayer);
    }

    @Override
    public void appendPlayerWithoutListed(Player source) {
        sendUpdatePacket(getPlayerHandle(source));
    }

    private void sendUpdatePacket(EntityPlayer from) {
        PacketPlayOutPlayerInfo updatePacket = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a, Collections.singletonList(from));

        setEntriesField(updatePacket, Collections.singletonList(new PacketPlayOutPlayerInfo.PlayerInfoData(from.fy(), from.e, from.d.b(),
                emptyComponent, from.fz() == null ? null : from.fz().b())));

        PacketPlayOutAnimation animatePacket = new PacketPlayOutAnimation(from, 0);

        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            EntityPlayer entityPlayer = getPlayerHandle(player);

            sendPacket(entityPlayer, updatePacket);
            sendPacket(entityPlayer, animatePacket);
        }
    }

    @Override
    public PacketPlayOutPlayerInfo updateDisplayNamePacket(Object entityPlayer, Object component, boolean listName) {
        if (listName) {
            setListName(entityPlayer, component);
        }

        return new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.d, (EntityPlayer) entityPlayer);
    }

    @Override
    public void setListName(Object entityPlayer, Object component) {
        ((EntityPlayer) entityPlayer).listName = (IChatBaseComponent) component;
    }

    @Override
    public PacketPlayOutPlayerInfo newPlayerInfoUpdatePacketAdd(Object... entityPlayers) {
        List<EntityPlayer> players = new ArrayList<>(entityPlayers.length);

        for (Object one : entityPlayers) {
            players.add((EntityPlayer) one);
        }

        return new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a, players);
    }

    @Override
    public PacketPlayOutPlayerInfo updateLatency(Object entityPlayer) {
        return new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.c, (EntityPlayer) entityPlayer);
    }

    @Override
    public PacketPlayOutPlayerInfo removeEntityPlayers(Object... entityPlayers) {
        List<EntityPlayer> players = new ArrayList<>(entityPlayers.length);

        for (Object one : entityPlayers) {
            players.add(((EntityPlayer) one));
        }

        return new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.e, players);
    }

    @Override
    public void setInfoData(Object info, UUID id, int ping, Object component) {
        PacketPlayOutPlayerInfo update = (PacketPlayOutPlayerInfo) info;

        for (PacketPlayOutPlayerInfo.PlayerInfoData playerInfo : update.b()) {
            if (playerInfo.a().getId().equals(id)) {
                setEntriesField(update, Collections.singletonList(new PacketPlayOutPlayerInfo.PlayerInfoData(playerInfo.a(), ping == -2 ? playerInfo.b() : ping, playerInfo.c(),
                        (IChatBaseComponent) component, playerInfo.e())));
                break;
            }
        }
    }

    private void setEntriesField(PacketPlayOutPlayerInfo playerInfoPacket, List<PacketPlayOutPlayerInfo.PlayerInfoData> list) {
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
        ScoreboardTeam playerTeam = new ScoreboardTeam(scoreboard, teamName);

        playerTeam.g().add(player.getName());

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

        scoreboardTeams.add(playerTeam);

        if (tagTeams.isEmpty()) {
            sendPacket(getPlayerHandle(player), PacketPlayOutScoreboardTeam.a(playerTeam, true));
            return;
        }

        for (TagTeam tagTeam : tagTeams) {
            if (!tagTeam.playerName.equals(player.getName())) {
                continue;
            }

            tagTeam.scoreboardTeam.a(playerTeam.c());
            tagTeam.scoreboardTeam.a(playerTeam.j());

            EntityPlayer handle = getPlayerHandle(player);

            sendPacket(handle, PacketPlayOutScoreboardTeam.a(playerTeam, true));
            sendPacket(handle, PacketPlayOutScoreboardTeam.a(tagTeam.scoreboardTeam, true));
            break;
        }
    }

    @Override
    public PacketPlayOutScoreboardTeam unregisterBoardTeam(Object playerTeam) {
        ScoreboardTeam team = (ScoreboardTeam) playerTeam;
        scoreboardTeams.remove(team);

        return PacketPlayOutScoreboardTeam.a(team);
    }

    @Override
    public ScoreboardTeam findBoardTeamByName(String teamName) {
        for (ScoreboardTeam team : scoreboardTeams) {
            if (team.b().equals(teamName)) {
                return team;
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

        public PacketReceivingListener(UUID listenerPlayerId, List<Class<?>> classesToListen) {
            this.listenerPlayerId = listenerPlayerId;
            this.classesToListen = classesToListen;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, io.netty.channel.ChannelPromise promise) throws Exception {
            Class<?> receivingClass = msg.getClass();

            for (Class<?> cl : classesToListen) {
                if (cl != receivingClass) {
                    continue;
                }

                // Temporal and disgusting solution to fix players name tag overwriting
                if (cl == PacketPlayOutScoreboardTeam.class) {
                    PacketPlayOutScoreboardTeam packetScoreboardTeam = (PacketPlayOutScoreboardTeam) msg;

                    if (!packetScoreboardTeam.e().isEmpty()) {
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

                                ScoreboardTeam scoreboardTeam = new ScoreboardTeam(((org.bukkit.craftbukkit.v1_19_R1.scoreboard.CraftScoreboard) player.getScoreboard()).getHandle(),
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

                if (cl == ClientboundPlayerChatPacket.class) {
                    Player player = Bukkit.getPlayer(listenerPlayerId);

                    if (player == null) {
                        break;
                    }

                    ClientboundPlayerChatPacket chatPacket = (ClientboundPlayerChatPacket) msg;
                    IChatBaseComponent content = chatPacket.b().c();

                    if (content == null) {
                        content = IChatBaseComponent.b(chatPacket.b().b().b());
                    }

                    java.util.Optional<net.minecraft.network.chat.ChatMessageType.a> chatType = chatPacket.c().a(((CraftServer) Bukkit.getServer()).getServer().aX());

                    if (chatType.isPresent()) {
                        sendPacket(player, new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(chatType.get().a(content), false));
                    }

                    return;
                }

                if (cl == PacketPlayOutPlayerInfo.class) {
                    PacketPlayOutPlayerInfo playerInfoPacket = (PacketPlayOutPlayerInfo) msg;

                    if (playerInfoPacket.c() == PacketPlayOutPlayerInfo.EnumPlayerInfoAction.b) {
                        Player player = Bukkit.getPlayer(listenerPlayerId);

                        if (player == null) {
                            break;
                        }

                        PacketPlayOutPlayerInfo updatePacket = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.c, Collections.emptyList());
                        List<PacketPlayOutPlayerInfo.PlayerInfoData> players = new ArrayList<>();

                        for (PacketPlayOutPlayerInfo.PlayerInfoData entry : playerInfoPacket.b()) {
                            if (entry.c() == EnumGamemode.d && !entry.a().getId().equals(listenerPlayerId)) {
                                players.add(new PacketPlayOutPlayerInfo.PlayerInfoData(entry.a(), entry.b(), EnumGamemode.a, entry.d(), entry.e()));
                            }
                        }

                        setEntriesField(updatePacket, players);
                        sendPacket(player, updatePacket);
                    }

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
