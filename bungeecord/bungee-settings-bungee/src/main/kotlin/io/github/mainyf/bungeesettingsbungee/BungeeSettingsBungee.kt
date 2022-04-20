package io.github.mainyf.bungeesettingsbungee

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import java.util.UUID

class BungeeSettingsBungee : Plugin(), Listener {

    private val CHANNEL_NAME = "cacserver:dispatcher"

    override fun onEnable() {
        proxy.registerChannel(CHANNEL_NAME)
        ProxyServer.getInstance().pluginManager.registerListener(this, this)
    }

    @EventHandler
    fun onReceive(event: PluginMessageEvent) {
        if (event.tag != CHANNEL_NAME) return
        val buf = Unpooled.wrappedBuffer(event.data)
        when (buf.readString()) {
            "tp" -> {
                val playerUUID = buf.readUUID()
                val proxyPlayer = ProxyServer.getInstance().getPlayer(playerUUID) ?: return
                val serverName = buf.readString()
                val proxyServer = ProxyServer.getInstance().getServerInfo(serverName) ?: return
                val world = buf.readString()
                val x = buf.readDouble()
                val y = buf.readDouble()
                val z = buf.readDouble()
                val yaw = buf.readFloat()
                val pitch = buf.readFloat()
                if (proxyPlayer.server.info != proxyServer) {
                    proxyPlayer.connect(proxyServer)
                }
                proxyServer.sendData(CHANNEL_NAME, Unpooled.buffer().apply {
                    writeString("tp")
                    writeUUID(playerUUID)
                    writeString(world)
                    writeDouble(x)
                    writeDouble(y)
                    writeDouble(z)
                    writeFloat(yaw)
                    writeFloat(pitch)
                }.toByteArray())
            }
        }
    }

    fun ByteBuf.toByteArray(): ByteArray {
        val bytes: ByteArray
        val length = readableBytes()

        if (hasArray()) {
            bytes = array()
        } else {
            bytes = ByteArray(length)
            getBytes(readerIndex(), bytes)
        }
        return bytes
    }

    fun ByteBuf.writeUUID(uuid: UUID) {
        writeLong(uuid.mostSignificantBits)
        writeLong(uuid.leastSignificantBits)
    }

    fun ByteBuf.readUUID(): UUID {
        val most = readLong()
        val least = readLong()
        return UUID(most, least)
    }

    fun ByteBuf.writeString(t: String) {
        writeInt(t.length)
        t.forEach {
            writeChar(it.code)
        }
    }

    fun ByteBuf.readString(): String {
        val l = readInt()
        val sb = StringBuilder()
        repeat(l) {
            sb.append(readChar())
        }
        return sb.toString()
    }

}