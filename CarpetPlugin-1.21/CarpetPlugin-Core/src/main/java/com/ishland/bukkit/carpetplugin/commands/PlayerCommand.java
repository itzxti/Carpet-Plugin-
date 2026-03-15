package com.ishland.bukkit.carpetplugin.commands;

import com.google.common.base.Preconditions;
import com.ishland.bukkit.carpetplugin.lib.fakeplayer.action.FakeEntityPlayerActionPack;
import com.ishland.bukkit.carpetplugin.lib.fakeplayer.base.FakeEntityPlayer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.ishland.bukkit.carpetplugin.utils.BrigadierUtils.*;

public class PlayerCommand {

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, Runtime.getRuntime().availableProcessors() * 16,
            60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    public void register() {
        CommandDispatcher<CommandSourceStack> dispatcher =
                ((CraftServer) Bukkit.getServer()).getHandle().getServer()
                        .getCommands().getDispatcher();

        dispatcher.register(literal("player")
                .requires(hasPermission("carpet.player"))
                .then(argument("player", StringArgumentType.word())
                        .suggests((ctx, b) -> suggestMatching(getPlayers(), b))
                        .then(literal("spawn")
                                .requires(hasPermission("carpet.player.spawn"))
                                .executes(PlayerCommand::spawnAsync)
                                .then(literal("sync")
                                        .requires(hasPermission("carpet.player.spawn.sync"))
                                        .executes(PlayerCommand::spawn)
                                )
                        )
                        .then(literal("kill")
                                .requires(hasPermission("carpet.player.kill"))
                                .executes(PlayerCommand::kill)
                        )
                        .then(literal("stop")
                                .requires(hasPermission("carpet.player.stop"))
                                .executes(ctx -> manipulate(ctx, FakeEntityPlayerActionPack::stopAll))
                        )
                        .then(literal("actions")
                                .requires(hasPermission("carpet.player.actions"))
                                .executes(PlayerCommand::actions)
                        )
                        .then(literal("sneak")
                                .requires(hasPermission("carpet.player.sneak"))
                                .executes(ctx -> manipulate(ctx, FakeEntityPlayerActionPack::doSneak))
                        )
                        .then(literal("unsneak")
                                .requires(hasPermission("carpet.player.sneak"))
                                .executes(ctx -> manipulate(ctx, FakeEntityPlayerActionPack::unSneak))
                        )
                        .then(literal("sprint")
                                .requires(hasPermission("carpet.player.sprint"))
                                .executes(ctx -> manipulate(ctx, FakeEntityPlayerActionPack::doSprint))
                        )
                        .then(literal("unsprint")
                                .requires(hasPermission("carpet.player.sprint"))
                                .executes(ctx -> manipulate(ctx, FakeEntityPlayerActionPack::unSprint))
                        )
                        .then(literal("look")
                                .requires(hasPermission("carpet.player.look"))
                                .then(literal("at")
                                        .then(literal("block").then(argument("blockpos", Vec3Argument.vec3())
                                                .executes(PlayerCommand::lookAtBlock)))
                                        .then(literal("direction").then(argument("direction", RotationArgument.rotation())
                                                .executes(PlayerCommand::lookAtDirection)))
                                )
                        )
                        .then(literal("use")
                                .requires(hasPermission("carpet.player.use"))
                                .then(literal("once").executes(ctx -> manipulate(ctx, FakeEntityPlayerActionPack::doUse)))
                                .then(literal("continuous")
                                        .then(argument("interval", IntegerArgumentType.integer(1))
                                                .then(argument("repeats", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> manipulate(ctx, ap -> ap.doUse(
                                                                IntegerArgumentType.getInteger(ctx, "interval"),
                                                                IntegerArgumentType.getInteger(ctx, "repeats"))))
                                                )
                                        )
                                        .executes(ctx -> manipulate(ctx, ap -> ap.doUse(1, 1)))
                                )
                        )
                        .then(literal("drop")
                                .requires(hasPermission("carpet.player.drop"))
                                .then(literal("once").executes(ctx -> manipulate(ctx, FakeEntityPlayerActionPack::dropOne)))
                                .then(literal("continuous")
                                        .then(argument("interval", IntegerArgumentType.integer(1))
                                                .then(argument("repeats", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> manipulate(ctx, ap -> ap.dropOne(
                                                                IntegerArgumentType.getInteger(ctx, "interval"),
                                                                IntegerArgumentType.getInteger(ctx, "repeats"))))
                                                )
                                        )
                                        .executes(ctx -> manipulate(ctx, ap -> ap.dropOne(1, 1)))
                                )
                        )
                        .then(literal("dropStack")
                                .requires(hasPermission("carpet.player.drop"))
                                .then(literal("once").executes(ctx -> manipulate(ctx, FakeEntityPlayerActionPack::dropStack)))
                                .then(literal("continuous")
                                        .then(argument("interval", IntegerArgumentType.integer(1))
                                                .then(argument("repeats", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> manipulate(ctx, ap -> ap.dropStack(
                                                                IntegerArgumentType.getInteger(ctx, "interval"),
                                                                IntegerArgumentType.getInteger(ctx, "repeats"))))
                                                )
                                        )
                                        .executes(ctx -> manipulate(ctx, ap -> ap.dropStack(1, 1)))
                                )
                        )
                        .then(literal("dropAll")
                                .requires(hasPermission("carpet.player.drop"))
                                .then(literal("once").executes(ctx -> manipulate(ctx, FakeEntityPlayerActionPack::dropAll)))
                                .then(literal("continuous")
                                        .then(argument("interval", IntegerArgumentType.integer(1))
                                                .then(argument("repeats", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> manipulate(ctx, ap -> ap.dropAll(
                                                                IntegerArgumentType.getInteger(ctx, "interval"),
                                                                IntegerArgumentType.getInteger(ctx, "repeats"))))
                                                )
                                        )
                                        .executes(ctx -> manipulate(ctx, ap -> ap.dropAll(1, 1)))
                                )
                        )
                )
        );
    }

    private static int lookAtDirection(CommandContext<CommandSourceStack> ctx) {
        return manipulate(ctx, ap -> {
            Vec2 rotation = RotationArgument.getRotation(ctx, "direction").getRotation(ctx.getSource());
            ap.fakeEntityPlayer.setYRot(rotation.y);
            ap.fakeEntityPlayer.setXRot(rotation.x);
        });
    }

    private static int lookAtBlock(CommandContext<CommandSourceStack> ctx) {
        return manipulate(ctx, ap -> {
            try {
                Vec3 pos = Vec3Argument.getVec3(ctx, "blockpos");
                ap.fakeEntityPlayer.lookAt(
                        net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, pos);
            } catch (Exception ignored) {}
        });
    }

    private static int actions(CommandContext<CommandSourceStack> ctx) {
        return manipulate(ctx, ap -> {
            sendMessage(ctx, "Activated actions:");
            for (FakeEntityPlayerActionPack.Action action : ap.getActivatedActions())
                sendMessage(ctx, "  " + action.toString());
        });
    }

    private static int manipulate(CommandContext<CommandSourceStack> ctx, Consumer<FakeEntityPlayerActionPack> consumer) {
        if (!isFakePlayer(ctx)) {
            sendMessage(ctx, "Only fake players can be manipulated");
            return 0;
        }
        FakeEntityPlayer player = getFakeEntityPlayer(ctx);
        consumer.accept(player.actionPack);
        return 1;
    }

    private static FakeEntityPlayer getFakeEntityPlayer(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        CraftPlayer bukkit = (CraftPlayer) Bukkit.getPlayerExact(name);
        assert bukkit != null;
        return (FakeEntityPlayer) bukkit.getHandle();
    }

    public void shutdown() {
        executor.shutdownNow();
        for (String name : getPlayers()) {
            Player p = Bukkit.getPlayerExact(name);
            if (p != null) p.kickPlayer("Shutdown");
        }
    }

    private static int kill(CommandContext<CommandSourceStack> ctx) {
        if (!isFakePlayer(ctx)) { sendMessage(ctx, "Only fake players can be killed"); return 0; }
        getFakeEntityPlayer(ctx).kill();
        return 1;
    }

    private static int spawnAsync(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        Preconditions.checkNotNull(name);
        Preconditions.checkArgument(!name.isEmpty());
        if (name.length() > 40) { sendMessage(ctx, "Player name is longer than 40 characters"); return 0; }
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getHandle().getServer();
        executor.execute(() -> {
            boolean found = server.getProfileCache()
                    .map(c -> c.get(name).isPresent())
                    .orElse(false);
            if (found) server.execute(() -> spawn(ctx));
            else sendMessage(ctx, "Player not found");
        });
        sendMessage(ctx, "Command queued, please wait...");
        return 1;
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx) {
        if (!canSpawn(ctx)) return 0;
        String name = StringArgumentType.getString(ctx, "player");
        if (name.length() > 16) { sendMessage(ctx, "Player name is longer than 16 characters"); return 0; }
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getHandle().getServer();
        ServerLevel world = ((CraftWorld) Objects.requireNonNull(ctx.getSource().getBukkitLocation()).getWorld()).getHandle();
        Vec3 pos = ctx.getSource().getPosition();
        Vec2 rot = ctx.getSource().getRotation();
        FakeEntityPlayer player = FakeEntityPlayer.createFake(name, server, pos.x, pos.y, pos.z, rot.y, rot.x, world, GameType.SURVIVAL);
        if (player == null) { sendMessage(ctx, "Could not spawn player. Make sure they exist on this server (online mode)."); return 0; }
        return 1;
    }

    private static boolean canSpawn(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        Location loc = ctx.getSource().getBukkitLocation();
        if (loc == null) { sendMessage(ctx, "Player can only be spawned with a location"); return false; }
        if (Bukkit.getPlayerExact(name) != null) { sendMessage(ctx, "Player " + name + " is already online"); return false; }
        return true;
    }

    private static boolean isFakePlayer(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) return false;
        return ((CraftPlayer) p).getHandle() instanceof FakeEntityPlayer;
    }
}
