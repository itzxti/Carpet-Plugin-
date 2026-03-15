package com.ishland.bukkit.carpetplugin.lib.fakeplayer.base;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket;

import java.net.SocketAddress;

public class FakeNetworkManager extends Connection {

    public FakeNetworkManager() {
        super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        this.preparing = false;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return new SocketAddress() {
            @Override public String toString() { return "CarpetPlugin"; }
        };
    }

    @Override
    public void send(Packet<?> packet, PacketSendListener listener) {
        if (packet instanceof ClientboundKeepAlivePacket keepAlive) {
            // Respond automatically to keep-alive packets
            ServerboundKeepAlivePacket response = new ServerboundKeepAlivePacket(keepAlive.getId());
            PacketListener packetListener = this.getPacketListener();
            if (packetListener instanceof net.minecraft.server.network.ServerGamePacketListenerImpl handler) {
                handler.handleKeepAlive(response);
            }
        }
    }

    @Override
    public void send(Packet<?> packet) {
        send(packet, null);
    }

    @Override
    public void handleDisconnection() {}

    @Override
    public void setReadOnly() {}
}
