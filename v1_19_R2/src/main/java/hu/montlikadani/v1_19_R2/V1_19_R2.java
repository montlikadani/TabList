package hu.montlikadani.v1_19_R2;

import com.mojang.authlib.GameProfile;
import hu.montlikadani.api.IPacketNM;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.PacketPlayInArmAnimation;
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
import org.bukkit.craftbukkit.v1_19_R2.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class V1_19_R2 implements IPacketNM {

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

        if (entityPlayer.b.b.m.pipeline().get("PacketInjector") == null) {
            entityPlayer.b.b.m.pipeline().addBefore("packet_handler", "PacketInjector", new PacketReceivingListener(entityPlayer.fD().getId(), classesToListen));
        }
    }

    @Override
    public void removePlayerChannelListener(Player player) {
        EntityPlayer entityPlayer = getPlayerHandle(player);

        if (entityPlayer.b.b.m.pipeline().get("PacketInjector") != null) {
            entityPlayer.b.b.m.pipeline().remove("PacketInjector");
        }
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
    public void addPlayersToTab(Player source, Player... targets) {
        List<EntityPlayer> players = new ArrayList<>(targets.length);

        for (Player player : targets) {
            players.add(getPlayerHandle(player));
        }

        sendPacket(source, ClientboundPlayerInfoUpdatePacket.a(players));
    }

    @Override
    public void removePlayersFromTab(Player source, Collection<? extends Player> players) {
        sendPacket(getPlayerHandle(source), new ClientboundPlayerInfoRemovePacket(players.stream().map(Player::getUniqueId).collect(Collectors.toList())));
    }

    @Override
    public void appendPlayerWithoutListed(Player source) {
        EntityPlayer from = getPlayerHandle(source);
        ClientboundPlayerInfoUpdatePacket updatePacket = ClientboundPlayerInfoUpdatePacket.a(Collections.singletonList(from));

        setEntriesField(updatePacket, () -> new ClientboundPlayerInfoUpdatePacket.b(from.fD().getId(), from.fD(), false, from.e, from.d.b(), emptyComponent,
                from.Y() == null ? null : from.Y().b()));

        PacketPlayInArmAnimation animatePacket = new PacketPlayInArmAnimation(net.minecraft.world.EnumHand.a);

        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            EntityPlayer entityPlayer = getPlayerHandle(player);

            sendPacket(entityPlayer, updatePacket);
            sendPacket(entityPlayer, animatePacket);
        }
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
                ClientboundPlayerInfoUpdatePacket.a.d, ClientboundPlayerInfoUpdatePacket.a.e), players);
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
                setEntriesField(update, () -> new ClientboundPlayerInfoUpdatePacket.b(playerInfo.a(), playerInfo.b(), playerInfo.c(),
                        ping == -2 ? playerInfo.d() : ping, playerInfo.e(), (IChatBaseComponent) component, playerInfo.g()));
                break;
            }
        }
    }

    private void setEntriesField(ClientboundPlayerInfoUpdatePacket playerInfoPacket, java.util.function.Supplier<ClientboundPlayerInfoUpdatePacket.b> supplier) {
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
    public PacketPlayOutScoreboardTeam createBoardTeam(Object teamName, Player player, boolean followNameTagVisibility) {
        IChatBaseComponent component = (IChatBaseComponent) teamName;
        ScoreboardTeam playerTeam = new ScoreboardTeam(new net.minecraft.world.scores.Scoreboard(), component.getString());

        playerTeam.g().add(player.getName());
        playerTeam.a(component);

        if (followNameTagVisibility) {
            net.minecraft.world.scores.ScoreboardTeam.EnumNameTagVisibility visibility = null;

            for (Team team : player.getScoreboard().getTeams()) {
                Team.OptionStatus optionStatus = team.getOption(Team.Option.NAME_TAG_VISIBILITY);

                switch (optionStatus) {
                    case FOR_OTHER_TEAMS:
                        visibility = net.minecraft.world.scores.ScoreboardTeam.EnumNameTagVisibility.c;
                        break;
                    case FOR_OWN_TEAM:
                        visibility = net.minecraft.world.scores.ScoreboardTeam.EnumNameTagVisibility.d;
                        break;
                    default:
                        if (optionStatus != Team.OptionStatus.ALWAYS) {
                            visibility = net.minecraft.world.scores.ScoreboardTeam.EnumNameTagVisibility.b;
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

                ClientboundPlayerInfoUpdatePacket playerInfoPacket = (ClientboundPlayerInfoUpdatePacket) msg;

                if (playerInfoPacket.b().contains(ClientboundPlayerInfoUpdatePacket.a.c)) {
                    for (ClientboundPlayerInfoUpdatePacket.b entry : playerInfoPacket.c()) {
                        if (entry.e() == EnumGamemode.d && !entry.a().equals(listenerPlayerId)) {
                            setEntriesField(playerInfoPacket, () -> new ClientboundPlayerInfoUpdatePacket.b(entry.a(), entry.b(), entry.c(), entry.d(),
                                    EnumGamemode.b, entry.f(), entry.g()));
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
