package com.ishland.bukkit.carpetplugin.lib.fakeplayer.base;

import com.google.common.base.Throwables;
import com.ishland.bukkit.carpetplugin.lib.fakeplayer.action.FakeEntityPlayerActionPack;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameType;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;

import javax.annotation.Nullable;

public class FakeEntityPlayer extends ServerPlayer {

    public final FakeEntityPlayerActionPack actionPack = new FakeEntityPlayerActionPack(this);
    public Runnable fixStartingPosition = null;

    public FakeEntityPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, CommonListenerCookie cookie) {
        super(server, level, profile, cookie);
    }

    @Nullable
    public static FakeEntityPlayer createFake(String username,
                                              MinecraftServer server,
                                              double x, double y, double z,
                                              float yaw, float pitch,
                                              ServerLevel world,
                                              GameType gameMode) {
        try {
            GameProfile profile = server.getProfileCache()
                    .map(c -> c.get(username).orElse(null))
                    .orElse(null);
            if (profile == null) return null;

            CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
            FakeEntityPlayer instance = new FakeEntityPlayer(server, world, profile, cookie);
            instance.fixStartingPosition = () -> instance.moveTo(x, y, z, yaw, pitch);

            FakeNetworkManager networkManager = new FakeNetworkManager();
            networkManager.channel = new FakeChannel();

            FakePlayerConnection connection = new FakePlayerConnection(server, networkManager, instance, cookie);
            networkManager.setListener(connection);

            server.getPlayerList().placeNewPlayer(networkManager, instance, cookie);

            instance.moveTo(x, y, z, yaw, pitch);
            instance.setHealth(20.0F);
            instance.setGameMode(gameMode);
            instance.removed = false;
            instance.maxUpStep = 0.6F;

            server.getPlayerList().broadcastAll(
                    new ClientboundRotateHeadPacket(instance, (byte) (instance.getYRot() * 256 / 360)),
                    world.dimension()
            );
            server.getPlayerList().broadcastAll(
                    new ClientboundTeleportEntityPacket(instance),
                    world.dimension()
            );
            world.getChunkSource().move(instance);
            return instance;
        } catch (Throwable t) {
            Throwables.throwIfUnchecked(t);
            throw new RuntimeException("Failed to spawn fake player: " + username, t);
        }
    }

    @Override
    public void kill() {
        server.execute(() -> {
            connection.connection.channel.close();
            connection.onDisconnect(net.minecraft.network.DisconnectionDetails.EMPTY);
        });
    }

    @Override
    public void tick() {
        if (server.getTickCount() % 10 == 0) {
            connection.resetPosition();
            level().getChunkSource().move(this);
            if (fixStartingPosition != null) {
                fixStartingPosition.run();
                fixStartingPosition = null;
            }
        }
        actionPack.tick();
        super.tick();
        doTick();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        server.execute(() -> {
            setHealth(20.0F);
            this.foodData = new FoodData();
            kill();
        });
    }
}
