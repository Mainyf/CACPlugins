package io.github.mainyf.bungeesettingsbukkit

import com.Zrips.CMI.events.CMIPlayerTeleportEvent
import io.github.mainyf.bungeesettingsbukkit.events.ServerPacketReceiveEvent
import io.github.mainyf.bungeesettingsbukkit.network.ClientSocketManager
import io.github.mainyf.newmclib.dsl.event
import io.github.mainyf.newmclib.dsl.events
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.serverId
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerTeleportEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

object CrossServerManager {

    private val pendingTPPosRequest = mutableMapOf<UUID, Location>()
    private val pendingTPPlayerRequest = mutableMapOf<UUID, Location>()
    private val tpMap = mutableMapOf<UUID, Long>()
    private val joinPlayers = mutableSetOf<UUID>()
    private val CHANNEL_NAME = "cacserver:dispatcher"
    val serverIds = CopyOnWriteArraySet<String>()
    val playerMap = ConcurrentHashMap<UUID, String>()

    private fun handleTP(player: Player, event: Cancellable? = null) {
        if (pendingTPPosRequest.containsKey(player.uuid)) {
            event?.isCancelled = true
            val loc = pendingTPPosRequest[player.uniqueId]!!
            player.teleport(loc)
            pendingTPPosRequest.remove(player.uniqueId)
            tpMap[player.uuid] = currentTime()
        }
        if (pendingTPPlayerRequest.containsKey(player.uuid)) {
            event?.isCancelled = true
            val loc = pendingTPPlayerRequest[player.uuid]!!
            player.teleport(loc)
            pendingTPPlayerRequest.remove(player.uuid)
            tpMap[player.uuid] = currentTime()
        }
    }

    fun init() {
        server().messenger.registerOutgoingPluginChannel(BungeeSettingsBukkit.INSTANCE, CHANNEL_NAME)
        server().messenger.registerIncomingPluginChannel(
            BungeeSettingsBukkit.INSTANCE, CHANNEL_NAME
        ) { _, _, data ->
            val buf = Unpooled.wrappedBuffer(data)
            when (buf.readString()) {
                ServerPacket.TP_POS.name -> {
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
                        pendingTPPosRequest[playerUUID] = loc
                    } else {
                        player.teleport(loc)
                    }
                }
                ServerPacket.TP_PLAYER.name -> {
                    val playerUUID = buf.readUUID()
                    val targetUUID = buf.readUUID()
                    val player = playerUUID.asPlayer()
                    val loc = targetUUID.asPlayer()?.location
                    if (player == null) {
                        if (loc != null) {
                            pendingTPPlayerRequest[playerUUID] = loc
                        }
                    } else {
                        if (loc != null) {
                            player.teleport(loc)
                        }
                    }
                }
            }
        }
        BungeeSettingsBukkit.INSTANCE.events {
            event<PlayerJoinEvent> {
                joinPlayers.add(player.uniqueId)
                BungeeSettingsBukkit.INSTANCE.submitTask(
                    delay = 20
                ) {
                    handleTP(player)
                }
            }
            event<CMIPlayerTeleportEvent> {
                if (joinPlayers.contains(player.uuid)) {
                    joinPlayers.remove(player.uuid)
                    handleTP(player, this)
                }
            }
            event<PlayerTeleportEvent> {
                if (joinPlayers.contains(player.uuid)) {
                    joinPlayers.remove(player.uuid)
                    handleTP(player, this)
                }
                if (tpMap.containsKey(player.uuid)) {
                    val pTime = tpMap[player.uuid]!!
                    if (currentTime() - pTime <= 3000L) {
                        isCancelled = true
                    }
                }
            }
        }
//        BungeeSettingsBukkit.INSTANCE.submitTask(period = 3 * 20L) {
//            updatePlayers()
//        }
    }

    fun handleServerPacket(packetName: String, packet: ServerPacket?, buf: ByteBuf) {
        kotlin.runCatching {
            when (packet) {
                ServerPacket.CMD -> {
                    val cmd = buf.readString()
                    BungeeSettingsBukkit.INSTANCE.submitTask {
                        execmd(cmd)
                    }
                }
                ServerPacket.UPDATE_SERVERIDS -> {
                    serverIds.clear()
                    serverIds.addAll(buf.readStringList())
                }
                ServerPacket.UPDATE_PLAYERS -> {
                    playerMap.clear()
                    repeat(buf.readInt()) {
                        val uuid = buf.readUUID()
                        val pName = buf.readString()
                        playerMap[uuid] = pName
                    }
                }
                ServerPacket.PLAYER_CHAT -> {
                    val player = buf.readUUID().asPlayer()
                    if (player != null) {
                        val json = buf.readString()
                        val comp = GsonComponentSerializer.gson().deserialize(json)
                        player.sendMessage(comp)
                    }
                }
                else -> {
                    BungeeSettingsBukkit.INSTANCE.submitTask {
                        val event = ServerPacketReceiveEvent(packetName, packet, buf)
                        pluginManager().callEvent(event)
                    }
                }
            }
            Unit
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun updateServerID() {
        ClientSocketManager.sendData(ServerPacket.UPDATE_SERVERIDS) {
            writeString(serverId())
        }
    }

    fun loadSocket(port: Int) {
        kotlin.runCatching {
            ClientSocketManager.initClient(port)
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun stp(player: Player, serverId: String, worldName: String, loc: Location) {
        if (serverId == serverId()) {
            player.teleport(loc.clone().apply {
                this.world = worldName.asWorld()
            })
        } else {
            player.sendPluginMessage(BungeeSettingsBukkit.INSTANCE, CHANNEL_NAME, Unpooled.buffer().apply {
                writeString(ServerPacket.TP_POS.name)
                writeUUID(player.uniqueId)
                writeString(serverId)
                writeString(worldName)
                writeDouble(loc.x)
                writeDouble(loc.y)
                writeDouble(loc.z)
                writeFloat(loc.yaw)
                writeFloat(loc.pitch)
            }.toByteArray())
        }
    }

    fun stp(player: Player, target: UUID) {
        val targetPlayer = target.asPlayer()
        if (targetPlayer != null) {
            player.teleport(targetPlayer.location)
        } else {
            player.sendPluginMessage(BungeeSettingsBukkit.INSTANCE, CHANNEL_NAME, Unpooled.buffer().apply {
                writeString(ServerPacket.TP_PLAYER.name)
                writeUUID(player.uuid)
                writeUUID(target)
            }.toByteArray())
        }
    }

    fun executeCommandToServer(serverId: String, cmd: String) {
        if (serverId == serverId()) {
            execmd(cmd)
        } else {
            ClientSocketManager.sendData(ServerPacket.CMD) {
                writeString(serverId)
                writeString(cmd)
            }
        }
    }

    fun executeCommandToAll(cmd: String) {
        execmd(cmd)
        ClientSocketManager.sendData(ServerPacket.CMD_ALL) {
            writeString(cmd)
        }
    }

    fun sendChat(player: UUID, comp: Component) {
        val p = player.asPlayer()
        if (p != null) {
            p.sendMessage(comp)
        } else {
            ClientSocketManager.sendData(ServerPacket.PLAYER_CHAT) {
                writeUUID(player)
                writeString(GsonComponentSerializer.gson().serialize(comp))
            }
        }
    }

    fun sendData(packet: ServerPacket, block: ByteBuf.() -> Unit) {
        ClientSocketManager.sendData(packet, block)
    }

    fun isOnline(uuid: UUID): Boolean {
        return playerMap.containsKey(uuid)
    }

}