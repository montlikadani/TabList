package hu.montlikadani.v1_17_R1;

import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutAnimation;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardScore;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import net.minecraft.server.ScoreboardServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class V1_17_R1 implements hu.montlikadani.api.IPacketNM {

    private final IChatBaseComponent emptyComponent = IChatBaseComponent.ChatSerializer.a("");

    private final Scoreboard scoreboard = new Scoreboard();

    private final Set<ScoreboardTeam> scoreboardTeams = new HashSet<>();
    private final Set<TagTeam> tagTeams = new HashSet<>();

    @Override
    public void sendPacket(Player player, Object packet) {
        getPlayerHandle(player).b.sendPacket((Packet<?>) packet);
    }

    private void sendPacket(EntityPlayer player, Packet<?> packet) {
        player.b.sendPacket(packet);
    }

    @Override
    public void addPlayerChannelListener(Player player, List<Class<?>> classesToListen) {
        EntityPlayer entityPlayer = getPlayerHandle(player);

        if (entityPlayer.b.a.k.pipeline().get(PACKET_INJECTOR_NAME) == null) {
            try {
                entityPlayer.b.a.k.pipeline().addBefore("packet_handler", PACKET_INJECTOR_NAME, new PacketReceivingListener(entityPlayer.getProfile().getId(), classesToListen));
            } catch (java.util.NoSuchElementException ex) {
                // packet_handler not exists, sure then, ignore
            }
        }
    }

    @Override
    public void removePlayerChannelListener(Player player) {
        EntityPlayer entityPlayer = getPlayerHandle(player);

        if (entityPlayer.b.a.k.pipeline().get(PACKET_INJECTOR_NAME) != null) {
            entityPlayer.b.a.k.pipeline().remove(PACKET_INJECTOR_NAME);
        }
    }

    @Override
    public EntityPlayer getPlayerHandle(Player player) {
        return ((org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer) player).getHandle();
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
    public EntityPlayer getNewEntityPlayer(com.mojang.authlib.GameProfile profile) {
        net.minecraft.server.MinecraftServer server = ((org.bukkit.craftbukkit.v1_17_R1.CraftServer) Bukkit.getServer()).getServer();

        return new EntityPlayer(server, server.E(), profile);
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
        sendPacket(getPlayerHandle(source), new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.e,
                players.stream().map(this::getPlayerHandle).collect(Collectors.toList())));
    }

    @Override
    public void appendPlayerWithoutListed(Player source) {
        EntityPlayer from = getPlayerHandle(source);
        PacketPlayOutPlayerInfo updatePacket = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a, Collections.singletonList(from));

        setEntriesField(updatePacket, Collections.singletonList(new PacketPlayOutPlayerInfo.PlayerInfoData(from.getProfile(), from.e, from.d.c(), emptyComponent)));

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
                setEntriesField(update, Collections.singletonList(new PacketPlayOutPlayerInfo.PlayerInfoData(playerInfo.a(), ping == -2 ? playerInfo.b() : ping,
                        playerInfo.c(), (IChatBaseComponent) component)));
                break;
            }
        }
    }

    private void setEntriesField(PacketPlayOutPlayerInfo playerInfoPacket, List<PacketPlayOutPlayerInfo.PlayerInfoData> list) {
        playerInfoPacket.b().clear();
        playerInfoPacket.b().addAll(list);
    }

    @Override
    public void createBoardTeam(String teamName, Player player, boolean followNameTagVisibility) {
        ScoreboardTeam playerTeam = new ScoreboardTeam(scoreboard, teamName);

        playerTeam.getPlayerNameSet().add(player.getName());

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
                playerTeam.setNameTagVisibility(visibility);
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

            tagTeam.scoreboardTeam.setDisplayName(playerTeam.getDisplayName());
            tagTeam.scoreboardTeam.setNameTagVisibility(playerTeam.getNameTagVisibility());

            EntityPlayer handle = getPlayerHandle(player);

            sendPacket(handle, PacketPlayOutScoreboardTeam.a(playerTeam, true));
            sendPacket(handle, PacketPlayOutScoreboardTeam.a(tagTeam.scoreboardTeam, true));
            break;
        }
    }

    @Override
    public PacketPlayOutScoreboardTeam unregisterBoardTeam(String teamName) {
        synchronized (scoreboardTeams) {
            for (ScoreboardTeam team : new HashSet<>(scoreboardTeams)) {
                if (team.getName().equals(teamName)) {
                    scoreboardTeams.remove(team);
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

        public PacketReceivingListener(UUID listenerPlayerId, List<Class<?>> classesToListen) {
            this.listenerPlayerId = listenerPlayerId;
            this.classesToListen = classesToListen;
        }

        @Override
        public void write(io.netty.channel.ChannelHandlerContext ctx, Object msg, io.netty.channel.ChannelPromise promise) throws Exception {
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

                                ScoreboardTeam scoreboardTeam = new ScoreboardTeam(((org.bukkit.craftbukkit.v1_17_R1.scoreboard.CraftScoreboard) player.getScoreboard()).getHandle(),
                                        packetTeam.a().getString());
                                scoreboardTeam.setPrefix(prefix);
                                scoreboardTeam.setSuffix(suffix);
                                scoreboardTeam.setNameTagVisibility(enumNameTagVisibility);
                                scoreboardTeam.setCollisionRule(enumTeamPush);
                                scoreboardTeam.setColor(packetTeam.c());
                                scoreboardTeam.getPlayerNameSet().add(playerName);

                                tagTeams.add(new TagTeam(playerName, scoreboardTeam));
                            }
                        });
                    }

                    super.write(ctx, msg, promise);
                    return;
                }

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
                            players.add(new PacketPlayOutPlayerInfo.PlayerInfoData(entry.a(), entry.b(), EnumGamemode.a, entry.d()));
                        }
                    }

                    setEntriesField(updatePacket, players);
                    sendPacket(player, updatePacket);
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
