package io.github.mainyf.bungeesettingsbungee.socket

import io.github.mainyf.bungeesettingsbukkit.toByteArray
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.net.SocketAddress
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Logger
import kotlin.concurrent.thread

class ClientMessageHandler(val socket: Socket, val logger: Logger) : Runnable {

    var serverID = ""
    var clientID: SocketAddress = socket.remoteSocketAddress
    private var bis: BufferedInputStream? = null
    private var dis: DataInputStream? = null
    private var bos: BufferedOutputStream? = null
    private var dos: DataOutputStream? = null

    private var senderLoop: Thread? = null
    private var receiveLoop: Thread? = null

    private val pendingData = LinkedBlockingQueue<ByteArray>()

    val ID get() = if (serverID.isBlank()) clientID else serverID

    override fun run() {
        kotlin.runCatching {
            println("子节点 $ID 成功连接到根节点")
            bis = BufferedInputStream(socket.getInputStream())
            dis = DataInputStream(bis!!)
            bos = BufferedOutputStream(socket.getOutputStream())
            dos = DataOutputStream(bos)
        }.onFailure {
            it.printStackTrace()
        }
        receiveLoop = thread {
            while (true) {
                if (!socket.isConnected || Thread.currentThread().isInterrupted) {
                    ServerSocketManager.removeClient(this)
                    break
                }
                var flag = false
                kotlin.runCatching {
                    val len = dis!!.readInt()
                    if (len > 0) {
                        val bytes = ByteArray(len)
                        dis!!.readFully(bytes, 0, bytes.size)
                        if (bytes.isEmpty()) return@runCatching
                        ServerSocketManager.handleClientMessage(this, bytes)
                    }
                }.onFailure {
                    flag = true
                    logger.info("$ID 断开连接")
                    ServerSocketManager.removeClient(this)
                }
                if (flag) {
                    break
                }
            }
        }
        senderLoop = thread {
            while (true) {
                if (!socket.isConnected || Thread.currentThread().isInterrupted) {
                    ServerSocketManager.removeClient(this)
                    break
                }
                var flag = false

                if (pendingData.isEmpty()) continue
                kotlin.runCatching {
                    val bytes = pendingData.poll()
                    dos!!.writeInt(bytes.size)
                    dos!!.write(bytes)

                    dos!!.flush()
                }.onFailure {
                    flag = true
                    logger.info("$ID 断开连接")
                    ServerSocketManager.removeClient(this)
                }
                if (flag) {
                    break
                }
            }
        }
    }

    private fun sendData(bytes: ByteArray) {
        pendingData.add(bytes)
    }

    fun sendData(id: Int, block: ByteBuf.() -> Unit) {
        return sendData(Unpooled.buffer().apply {
            writeInt(id)
            this.block()
        }.toByteArray())
    }

    fun close() {
        logger.info("子节点 $ID 已离线")
        kotlin.runCatching {
            senderLoop?.interrupt()
            receiveLoop?.interrupt()
            socket.close()
            dis!!.close()
            dos!!.close()
        }.onFailure { }
    }

}