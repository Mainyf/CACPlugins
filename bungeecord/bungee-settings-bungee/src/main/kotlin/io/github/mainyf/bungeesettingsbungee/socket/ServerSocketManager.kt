package io.github.mainyf.bungeesettingsbungee.socket

import io.github.mainyf.bungeesettingsbungee.*
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.md_5.bungee.api.ProxyServer
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.logging.Logger

object ServerSocketManager {

    private var threadPool: ExecutorService? = null

    private var serverSocket: ServerSocket? = null

    private var serverLoop: Future<*>? = null

    private val clientSockets = CopyOnWriteArrayList<Pair<ClientMessageHandler, Future<*>>>()

    private lateinit var logger: Logger

    fun close() {
        clientSockets.forEach {
            it.first.close()
            it.second.cancel(true)
        }
        serverSocket!!.close()
    }

    fun getClient(serverId: String): ClientMessageHandler? {
        return clientSockets.find {
            it.first.serverID == serverId
        }?.first
    }

    fun sendDataToClient(serverId: String, id: ServerPacket, block: ByteBuf.() -> Unit) {
        val targetClient = getClient(serverId)
        if (targetClient != null) {
            targetClient.sendData(id, block)
        } else {
            logger.info("$serverId 未连接到根节点")
        }
    }

    fun removeClient(client: ClientMessageHandler) {
        kotlin.runCatching {
            clientSockets.forEach { (c, future) ->
                if (c.clientID == client.clientID) {
                    client.close()
                    future.cancel(true)
                }
            }
            clientSockets.removeAll { it.first.clientID == client.clientID }
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun initServer(port: Int, logger: Logger) {
        this.logger = logger
        kotlin.runCatching {
            serverSocket?.close()
            threadPool?.shutdownNow()
            serverLoop?.cancel(true)
        }.onFailure {
            it.printStackTrace()
        }
        threadPool = Executors.newFixedThreadPool(2)
        serverSocket = ServerSocket(port)

        serverLoop = threadPool!!.submit {
            logger.info("等待子节点数据传递")
            while (true) {
                try {
                    val handler = ClientMessageHandler(serverSocket!!.accept(), this.logger)
                    clientSockets.add(handler to threadPool!!.submit(handler))
                } catch (e: Exception) {
                    if (serverSocket == null || serverSocket!!.isClosed) {
                        break
                    }
                    e.printStackTrace()
                }
            }
        }
        Timer().schedule(object : TimerTask() {
            override fun run() {
                if (clientSockets.isEmpty()) return
                val buf = Unpooled.buffer()
                val players = ProxyServer.getInstance().players
                buf.writeString(ServerPacket.UPDATE_PLAYERS.name)
                buf.writeInt(players.size)
                players.forEach {
                    buf.writeUUID(it.uniqueId)
                    buf.writeString(it.name)
                }
                val bytes = buf.toByteArray()
                clientSockets.forEach {
                    it.first.sendData(bytes)
                }
            }
        }, 3000L, 3000L)
    }

    fun loadServerIdList() {
        val ids = clientSockets.map { it.first.serverID }.toMutableList()
        clientSockets.forEach {
            it.first.sendData(ServerPacket.UPDATE_SERVERIDS) {
                writeStringList(ids)
            }
        }
    }

    fun handleClientMessage(client: ClientMessageHandler, bytes: ByteArray) {
        val buf = Unpooled.wrappedBuffer(bytes)
        val packetName = buf.readString()
        when (ServerPacket.getPacket(packetName)) {
            ServerPacket.UPDATE_SERVERIDS -> {
                client.serverID = buf.readString()
                logger.info("${client.serverID} 子节点初始化成功")
                loadServerIdList()
            }
            ServerPacket.CMD -> {
                val serverId = buf.readString()
                val cmd = buf.readString()
                sendDataToClient(serverId, ServerPacket.CMD) {
                    writeString(cmd)
                }
            }
            ServerPacket.CMD_ALL -> {
                val cmd = buf.readString()
                clientSockets.map { it.first }.forEach {
                    if (it.serverID != client.serverID) {
                        it.sendData(ServerPacket.CMD) {
                            writeString(cmd)
                        }
                    }
                }
            }
            ServerPacket.PLAYER_CHAT -> {
                val pUUID = buf.readUUID()
                ProxyServer.getInstance().servers.forEach { (key, info) ->
                    if (info.players.any { it.uniqueId == pUUID }) {
                        getClient(key)?.sendData(ServerPacket.PLAYER_CHAT) {
                            writeUUID(pUUID)
                            writeString(buf.readString())
                        }
                    }
                }
            }
            ServerPacket.KEEP_ALIVE -> {}
            else -> {
                if (packetName.startsWith("broadcast_")) {
                    clientSockets.map { it.first }.forEach {
                        if (it.serverID != client.serverID) {
                            it.sendData(buf.toByteArray())
                        }
                    }
                }
            }
        }
    }
}