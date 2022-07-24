package io.github.mainyf.bungeesettingsbukkit.events;

import io.github.mainyf.bungeesettingsbukkit.ServerPacket;
import io.netty.buffer.ByteBuf;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerPacketReceiveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final String packetName;

    @Nullable
    private final ServerPacket packet;

    private final ByteBuf buf;

    public ServerPacketReceiveEvent(String packetName, @Nullable ServerPacket packet, ByteBuf buf) {
        this.packetName = packetName;
        this.packet = packet;
        this.buf = buf;
    }

    public String getPacketName() {
        return packetName;
    }

    @Nullable
    public ServerPacket getPacket() {
        return packet;
    }

    public ByteBuf getBuf() {
        return buf;
    }

    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}