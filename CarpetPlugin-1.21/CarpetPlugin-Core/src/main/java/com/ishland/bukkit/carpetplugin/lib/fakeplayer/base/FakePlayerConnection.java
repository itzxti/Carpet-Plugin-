package com.ishland.bukkit.carpetplugin.lib.fakeplayer.base;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;

public class FakePlayerConnection extends ServerGamePacketListenerImpl {

    public FakePlayerConnection(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        super(server, connection, player, cookie);
    }

    @Override
    public void send(Packet<?> packet) {
        // Discard all outgoing packets to fake player
    }

    @Override
    public void disconnect(net.minecraft.network.chat.Component reason) {
        if (player instanceof FakeEntityPlayer fakePlayer) {
            fakePlayer.kill();
        }
    }
}
