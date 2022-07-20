package io.github.mainyf.bungeesettingsbungee.socket

import io.github.mainyf.bungeesettingsbukkit.readString
import io.github.mainyf.bungeesettingsbukkit.writeString
import io.github.mainyf.bungeesettingsbukkit.writeStringList
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.net.ServerSocket
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

    fun sendDataToClient(serverId: String, id: Int, block: ByteBuf.() -> Unit) {
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
    }

    fun loadServerIdList() {
        val ids = clientSockets.map { it.first.serverID }.toMutableList()
        clientSockets.forEach {
            it.first.sendData(101) {
                writeStringList(ids)
            }
        }
    }

    fun handleClientMessage(client: ClientMessageHandler, bytes: ByteArray) {
        val buf = Unpooled.wrappedBuffer(bytes)
        when (buf.readInt()) {
            0 -> {
                client.serverID = buf.readString()
                logger.info("${client.serverID} 子节点初始化成功")
                loadServerIdList()
            }
            1 -> {
                val serverId = buf.readString()
                val cmd = buf.readString()
                sendDataToClient(serverId, 1) {
                    writeString(cmd)
                }
            }
            2 -> {
                val cmd = buf.readString()
                clientSockets.map { it.first }.forEach {
                    if (it.serverID != client.serverID) {
                        it.sendData(1) {
                            writeString(cmd)
                        }
                    }
                }
            }
        }
    }
}