package hu.montlikadani.v1_19_R1;

import com.mojang.authlib.GameProfile;
import hu.montlikadani.api.IPacketNM;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayInArmAnimation;
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
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.ScoreboardTeam;
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

    private final List<ScoreboardTeam> playerTeams = new ArrayList<>();

    private Field entriesField;
    private final IChatBaseComponent emptyComponent = IChatBaseComponent.ChatSerializer.a("");

    @Override
    public void sendPacket(Player player, Object packet) {
        getPlayerHandle(player).b.a((Packet<?>) packet);
    }

    private void sendPacket(EntityPlayer player, Packet<?> packet) {
        player.b.a(packet);
    }

    @Override
    public void addPlayerChannelListener(Player player, Class<?>... classesToListen) {
        EntityPlayer entityPlayer = getPlayerHandle(player);

        if (entityPlayer.b.b.m.pipeline().get(PACKET_INJECTOR_NAME) == null) {
            entityPlayer.b.b.m.pipeline().addBefore("packet_handler", PACKET_INJECTOR_NAME, new PacketReceivingListener(entityPlayer.fy().getId(), classesToListen));
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
        sendPacket(getPlayerHandle(source), new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.e,
                players.stream().map(this::getPlayerHandle).collect(Collectors.toList())));
    }

    @Override
    public void appendPlayerWithoutListed(Player source) {
        EntityPlayer from = getPlayerHandle(source);
        PacketPlayOutPlayerInfo updatePacket = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a, Collections.singletonList(from));

        setEntriesField(updatePacket, () -> new PacketPlayOutPlayerInfo.PlayerInfoData(from.fy(), from.e, from.d.b(), emptyComponent, from.fz() == null ? null : from.fz().b()));

        PacketPlayInArmAnimation animatePacket = new PacketPlayInArmAnimation(net.minecraft.world.EnumHand.a);

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
                setEntriesField(update, () -> new PacketPlayOutPlayerInfo.PlayerInfoData(playerInfo.a(), ping == -2 ? playerInfo.b() : ping, playerInfo.c(),
                        (IChatBaseComponent) component, playerInfo.e()));
                break;
            }
        }
    }

    private void setEntriesField(PacketPlayOutPlayerInfo playerInfoPacket, java.util.function.Supplier<PacketPlayOutPlayerInfo.PlayerInfoData> supplier) {
        try {

            // Entries list is immutable, so use reflection to bypass
            if (entriesField == null) {
                entriesField = playerInfoPacket.getClass().getDeclaredField("b");
                entriesField.setAccessible(true);
            }

            entriesField.set(playerInfoPacket, Collections.singletonList(supplier.get()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public PacketPlayOutScoreboardTeam createBoardTeam(Object teamNameComponent, String teamName, Player player, boolean followNameTagVisibility) {
        ScoreboardTeam playerTeam = new ScoreboardTeam(new net.minecraft.world.scores.Scoreboard(), teamName);

        playerTeam.g().add(player.getName());
        playerTeam.a((IChatBaseComponent) teamNameComponent);

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

        playerTeams.add(playerTeam);
        return PacketPlayOutScoreboardTeam.a(playerTeam, true);
    }

    @Override
    public PacketPlayOutScoreboardTeam unregisterBoardTeam(Object playerTeam) {
        ScoreboardTeam team = (ScoreboardTeam) playerTeam;
        playerTeams.remove(team);

        return PacketPlayOutScoreboardTeam.a(team);
    }

    @Override
    public ScoreboardTeam findBoardTeamByName(String teamName) {
        for (ScoreboardTeam team : playerTeams) {
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
        private final Class<?>[] classesToListen;

        public PacketReceivingListener(UUID listenerPlayerId, Class<?>... classesToListen) {
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

                PacketPlayOutPlayerInfo playerInfoPacket = (PacketPlayOutPlayerInfo) msg;

                if (playerInfoPacket.c() == PacketPlayOutPlayerInfo.EnumPlayerInfoAction.b) {
                    for (PacketPlayOutPlayerInfo.PlayerInfoData entry : playerInfoPacket.b()) {
                        if (entry.c() == EnumGamemode.d && !entry.a().getId().equals(listenerPlayerId)) {
                            setEntriesField(playerInfoPacket, () -> new PacketPlayOutPlayerInfo.PlayerInfoData(entry.a(), entry.b(), EnumGamemode.b, entry.d(), entry.e()));
                        }
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
