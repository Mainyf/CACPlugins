package io.github.mainyf.bungeesettingsbukkit.network

import io.github.mainyf.bungeesettingsbukkit.BungeeSettingsBukkit
import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.newmclib.exts.toByteArray
import io.github.mainyf.newmclib.logger
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.net.SocketException
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

object ClientSocketManager {

    private var threadPool: ExecutorService? = null
    private var clientSocket: Socket? = null
    private var dis: DataInputStream? = null
    private var dos: DataOutputStream? = null

    private var clientKeepLoop: Thread? = null
    private var clientSenderLoop: Future<*>? = null
    private var clientReceiveLoop: Future<*>? = null

    private val pendingData = LinkedBlockingQueue<ByteArray>()

    private var bcPort = 24440

    private var hasClose = false

    private fun loadClientSocket(): Boolean {
        kotlin.runCatching {
            clientSocket = Socket("127.0.0.1", this.bcPort)
            clientSocket!!.tcpNoDelay = true
            dis = DataInputStream(clientSocket!!.getInputStream())

            dos = DataOutputStream(BufferedOutputStream(clientSocket!!.getOutputStream(), 5120))
            CrossServerManager.initServerID()
        }.onFailure {
            BungeeSettingsBukkit.LOG.info("未连接到根节点，等待重试")
            return false
        }
        BungeeSettingsBukkit.LOG.info("连接成功")
        return true
    }

    fun tryConnect(block: () -> Unit) {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                if (loadClientSocket()) {
                    hasClose = true
                    block()
                    cancel()
                }
            }
        }, 1000L, 1000L)
    }

    fun initClient(bcPort: Int) {
        this.bcPort = bcPort
        kotlin.runCatching {
            clientSocket?.close()

            clientKeepLoop?.interrupt()
            clientSenderLoop?.cancel(true)
            clientReceiveLoop?.cancel(true)
        }.onFailure {
            it.printStackTrace()
        }
        threadPool = Executors.newFixedThreadPool(3)
        tryConnect {
            clientKeepLoop = thread {
                while (true) {
                    if (Thread.currentThread().isInterrupted) {
                        return@thread
                    }
                    Thread.sleep(200L)

                    runConnect {
                        sendData(999)
                    }
                }
            }
            clientSenderLoop = threadPool!!.submit {
                while (true) {
                    if (pendingData.isEmpty()) continue

                    runConnect {
                        val bytes = pendingData.poll()
                        dos!!.writeInt(bytes.size)
                        dos!!.write(bytes)

                        dos!!.flush()
                    }
                }
            }
            clientReceiveLoop = threadPool!!.submit {
                while (true) {
                    runConnect {
                        val len = dis!!.readInt()
                        if (len > 0) {
                            val bytes = ByteArray(len)
                            dis!!.readFully(bytes, 0, bytes.size)
                            if (bytes.isEmpty()) return@runConnect
                            handleData(Unpooled.wrappedBuffer(bytes))
                        }
                    }
                }
            }
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

    fun sendData(id: Int, bufBlock: ByteBuf.() -> Unit = {}) {
        pendingData.add(Unpooled.buffer().apply {
            writeInt(id)
            this.bufBlock()
        }.toByteArray())
    }

    fun handleData(buf: ByteBuf) {
        CrossServerManager.handleServerPacket(buf.readInt(), buf)
    }

}