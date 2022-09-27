package io.github.mainyf.bungeesettingsbukkit.network

import io.github.mainyf.bungeesettingsbukkit.BungeeSettingsBukkit
import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.bungeesettingsbukkit.ServerPacket
import io.github.mainyf.newmclib.exts.readString
import io.github.mainyf.newmclib.exts.toByteArray
import io.github.mainyf.newmclib.exts.writeString
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.concurrent.timerTask

object ClientSocketManager {

    private var clientSocket: Socket? = null
    private var dis: DataInputStream? = null
    private var dos: DataOutputStream? = null

    private var clientKeepLoop: Timer? = null
    private var clientSenderLoop: Timer? = null
    private var clientReceiveLoop: Timer? = null

    private val pendingData = LinkedBlockingQueue<ByteArray>()

    private var bcPort = 24440

    private var hasClose = false

    private var retryInfo = true

    private fun loadClientSocket(): Boolean {
        kotlin.runCatching {
            clientSocket = Socket("127.0.0.1", this.bcPort)
            clientSocket!!.tcpNoDelay = true
            dis = DataInputStream(clientSocket!!.getInputStream())

            dos = DataOutputStream(BufferedOutputStream(clientSocket!!.getOutputStream(), 5120))
            CrossServerManager.updateServerID()
        }.onFailure {
            if(retryInfo) {
                retryInfo = false
                BungeeSettingsBukkit.LOG.info("未连接到根节点，等待重试")
            }
            return false
        }
        retryInfo = true
        BungeeSettingsBukkit.LOG.info("连接成功")
        return true
    }

    fun tryConnect(block: () -> Unit) {
        Timer().schedule(timerTask {
            if (loadClientSocket()) {
                hasClose = true
                block()
                cancel()
            }
        }, 1000L, 1000L)
    }

    fun initClient(bcPort: Int) {
        this.bcPort = bcPort
        kotlin.runCatching {
            clientSocket?.close()

            clientKeepLoop?.cancel()
            clientSenderLoop?.cancel()
            clientReceiveLoop?.cancel()

        }.onFailure {
            it.printStackTrace()
        }
        tryConnect {
            clientKeepLoop = Timer("clientKeepLoop")
            clientKeepLoop!!.schedule(timerTask {
                runConnect {
                    sendData(ServerPacket.KEEP_ALIVE)
                }
            }, 300L, 300L)
            clientSenderLoop = Timer("clientSenderLoop")
            clientSenderLoop!!.schedule(timerTask {
                if (pendingData.isEmpty()) return@timerTask

                runConnect {
                    val bytes = pendingData.poll()
                    dos!!.writeInt(bytes.size)
                    dos!!.write(bytes)

                    dos!!.flush()
                }
            }, 100L, 100L)
            clientReceiveLoop = Timer("clientReceiveLoop")
            clientReceiveLoop!!.schedule(timerTask {
                runConnect {
                    val len = dis!!.readInt()
                    if (len > 0) {
                        val bytes = ByteArray(len)
                        dis!!.readFully(bytes, 0, bytes.size)
                        if (bytes.isEmpty()) return@runConnect
                        handleData(Unpooled.wrappedBuffer(bytes))
                    }
                }
            }, 100L, 100L)
        }
    }

    fun runConnect(block: () -> Unit) {
        if (!hasClose) {
            return
        }
        kotlin.runCatching {
            block()
        }.onFailure {
            BungeeSettingsBukkit.LOG.info("连接错误，准备重试")
            tryConnect {
                hasClose = true
            }
//            if (it is SocketException && it.message == "Connection reset by peer") {
//                loadClientSocket()
//            } else {
//                loadClientSocket()
//            }
            hasClose = false
        }
    }

    fun sendData(buf: ByteBuf) {
        pendingData.add(buf.toByteArray())
    }

    fun sendData(id: ServerPacket, bufBlock: ByteBuf.() -> Unit = {}) {
        pendingData.add(Unpooled.buffer().apply {
            writeString(id.name)
            this.bufBlock()
        }.toByteArray())
    }

    fun handleData(buf: ByteBuf) {
        val packetName = buf.readString()
        CrossServerManager.handleServerPacket(packetName, ServerPacket.getPacket(packetName), buf)
    }

}