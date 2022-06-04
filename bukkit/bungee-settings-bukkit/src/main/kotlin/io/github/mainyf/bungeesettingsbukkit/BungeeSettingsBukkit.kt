package io.github.mainyf.bungeesettingsbukkit

import com.Zrips.CMI.events.CMIPlayerTeleportEvent
import io.github.mainyf.newmclib.command.cmdParser
import io.github.mainyf.newmclib.dsl.event
import io.github.mainyf.newmclib.dsl.events
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.storage.BaseModel
import io.github.mainyf.newmclib.storage.NoCacheStorage
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class BungeeSettingsBukkit : JavaPlugin() {

    private val LOG = io.github.mainyf.newmclib.getLogger("BungeeSettingsBukkit")

    private lateinit var storage: NoCacheStorage<GlobalCommandCache>
    private val CHANNEL_NAME = "cacserver:dispatcher"
    private val pendingTPRequest = mutableMapOf<UUID, Location>()
    private val joinPlayers = mutableSetOf<UUID>()

    override fun onEnable() {
        storage = NoCacheStorage.mysql(GlobalCommandCache::class)
        registerCommand("glDis") { sender, _, _, args ->
            cmdParser(sender, args) cmd@{
                if (!sender.isOp) return@cmd

                val type = arg<String>() ?: return@cmd
                when (type) {
                    "stp" -> {
                        val player = arg<Player>() ?: return@cmd
                        val serverName = arg<String>() ?: return@cmd
                        val world = arg<String>() ?: return@cmd
                        val x = arg<Double>() ?: return@cmd
                        val y = arg<Double>() ?: return@cmd
                        val z = arg<Double>() ?: return@cmd
                        val yaw = arg<Float>() ?: player.location.yaw
                        val pitch = arg<Float>() ?: player.location.pitch
                        player.sendPluginMessage(this@BungeeSettingsBukkit, CHANNEL_NAME, Unpooled.buffer().apply {
                            writeString("tp")
                            writeUUID(player.uniqueId)
                            writeString(serverName)
                            writeString(world)
                            writeDouble(x)
                            writeDouble(y)
                            writeDouble(z)
                            writeFloat(yaw)
                            writeFloat(pitch)
                        }.toByteArray())
                    }
                    "cmd" -> {
                        val serverName = arg<String>() ?: return@cmd
                        val cmd = arg(start = 2) ?: return@cmd
                        storage.add(GlobalCommandCache(serverName, cmd))
                    }
//                    "cmdAll" -> {
//                        val cmd = arg(start = 1) ?: return@cmd
//                        storage.add(GlobalCommandCache("all", cmd))
//                    }
                }
            }
            false
        }
        server.messenger.registerOutgoingPluginChannel(this, CHANNEL_NAME)
        server.messenger.registerIncomingPluginChannel(
            this, CHANNEL_NAME
        ) { _, _, data ->
            val buf = Unpooled.wrappedBuffer(data)
            when (buf.readString()) {
                "tp" -> {
                    val playerUUID = buf.readUUID()
                    val world = buf.readString().asWorld() ?: return@registerIncomingPluginChannel
                    val x = buf.readDouble()
                    val y = buf.readDouble()
                    val z = buf.readDouble()
                    val yaw = buf.readFloat()
                    val pitch = buf.readFloat()
                    val loc = Location(world, x, y, z, yaw, pitch)
                    val player = playerUUID.asPlayer()
                    if (player == null) {
                        pendingTPRequest[playerUUID] = loc
                    } else {
                        player.teleport(loc)
                    }
                }
            }
        }
        events {
            event<PlayerJoinEvent> {
                joinPlayers.add(player.uniqueId)
            }
            event<CMIPlayerTeleportEvent> {
                if (joinPlayers.contains(player.uniqueId)) {
                    joinPlayers.remove(player.uniqueId)
                    if (pendingTPRequest.containsKey(player.uniqueId)) {
                        isCancelled = true
                        val loc = pendingTPRequest[player.uniqueId]!!
                        player.teleport(loc)
                        pendingTPRequest.remove(player.uniqueId)
                    }
                }
            }
        }
//        runTaskTimerAsyncBR(30L, 30L) {
//            val cmds = storage.findAll().filter {
//                it.serverId == serverId()
//            }
//            if (cmds.isEmpty()) return@runTaskTimerAsyncBR
//            LOG.info("检测到全局命令执行: ${cmds.joinToString(", ") { it.cmd }}")
//            storage.transactionVoid { table ->
//                cmds.forEach { cache ->
//                    table.deleteWhere { table.id eq cache.id }
//                }
//            }
//            runTaskBR {
//                cmds.forEach {
//                    Bukkit.dispatchCommand(
//                        consoleSender(),
//                        it.cmd
//                    )
//                }
//            }
//        }
    }

    override fun onDisable() {
        storage.close()
        server.messenger.unregisterIncomingPluginChannel(this)
        server.messenger.unregisterOutgoingPluginChannel(this)
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

    class GlobalCommandCache(
        val serverId: String = "",
        val cmd: String = ""
    ) : BaseModel()
}
