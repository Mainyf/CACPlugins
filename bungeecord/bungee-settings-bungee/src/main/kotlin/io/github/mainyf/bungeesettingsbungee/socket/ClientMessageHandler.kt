package io.github.mainyf.bungeesettingsbungee.socket

import io.github.mainyf.bungeesettingsbungee.toByteArray
import io.github.mainyf.bungeesettingsbungee.writeString
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.net.SocketAddress
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.concurrent.timerTask

class ClientMessageHandler(val socket: Socket, val logger: Logger) : Runnable {

    var serverID = ""
    var clientID: SocketAddress = socket.remoteSocketAddress
    private var bis: BufferedInputStream? = null
    private var dis: DataInputStream? = null
    private var bos: BufferedOutputStream? = null
    private var dos: DataOutputStream? = null

    private var receiveLoop: Timer? = null
    private var senderLoop: Timer? = null

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

        receiveLoop = Timer("$ID - receiveLoop")
        receiveLoop!!.schedule(timerTask {
            var flag = false
            kotlin.runCatching {
                val len = dis!!.readInt()
                if (len > 0) {
                    val bytes = ByteArray(len)
                    dis!!.readFully(bytes, 0, bytes.size)
                    if (bytes.isEmpty()) return@runCatching
                    ServerSocketManager.handleClientMessage(this@ClientMessageHandler, bytes)
                }
            }.onFailure {
                flag = true
                logger.info("$ID 断开连接")
                this.cancel()
                ServerSocketManager.removeClient(this@ClientMessageHandler)
            }
            if (flag) {
                this.cancel()
            }
        }, 100L, 100L)
        senderLoop = Timer("$ID - senderLoop")
        senderLoop!!.schedule(timerTask {
            var flag = false

            if (pendingData.isEmpty()) return@timerTask
            kotlin.runCatching {
                val bytes = pendingData.poll()
                dos!!.writeInt(bytes.size)
                dos!!.write(bytes)

                dos!!.flush()
            }.onFailure {
                flag = true
                logger.info("$ID 断开连接")
                this.cancel()
                ServerSocketManager.removeClient(this@ClientMessageHandler)
            }
            if (flag) {
                this.cancel()
            }
        }, 100L, 100L)
    }

    fun sendData(bytes: ByteArray) {
        pendingData.add(bytes)
    }

    fun sendData(id: ServerPacket, block: ByteBuf.() -> Unit) {
        return sendData(Unpooled.buffer().apply {
            writeString(id.name)
            this.block()
        }.toByteArray())
    }

    fun close() {
        logger.info("子节点 $ID 已离线")
        kotlin.runCatching {
            senderLoop?.cancel()
            receiveLoop?.cancel()
            socket.close()
            dis!!.close()
            dos!!.close()
        }.onFailure { }
    }

}