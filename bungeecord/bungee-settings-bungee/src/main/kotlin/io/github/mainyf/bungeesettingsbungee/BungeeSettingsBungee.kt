package io.github.mainyf.bungeesettingsbungee

import io.github.mainyf.bungeesettingsbungee.socket.ServerPacket
import io.github.mainyf.bungeesettingsbungee.socket.ServerSocketManager
import io.netty.buffer.Unpooled
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import net.md_5.bungee.event.EventHandler

class BungeeSettingsBungee : Plugin(), Listener {

    private val CHANNEL_NAME = "cacserver:dispatcher"
    var socketPort = 24440

    companion object {

        lateinit var INSTANCE: BungeeSettingsBungee

    }

    override fun onEnable() {
        INSTANCE = this
        proxy.registerChannel(CHANNEL_NAME)
        ProxyServer.getInstance().pluginManager.registerListener(this, this)
        ProxyServer.getInstance().pluginManager.registerCommand(this, object : Command("bsb") {

            override fun execute(sender: CommandSender, args: Array<String>) {
                if (!sender.hasPermission("bsb.command.use")) return
                if (args.getOrNull(0) == "reload") {
                    loadConfig()
                    sender.sendMessage(*ComponentBuilder("[BungeeSettingsBungee] 重载成功").color(ChatColor.GREEN).create())
                }
            }

        })
        loadConfig()
    }

    override fun onDisable() {
        ServerSocketManager.close()
    }

    private fun loadConfig() {
        val configProvider = ConfigurationProvider.getProvider(YamlConfiguration::class.java)
        val configFile = dataFolder.resolve("config.yml")
        if (!configFile.exists()) {
            dataFolder.mkdirs()
            getResourceAsStream("config.yml").transferTo(configFile.outputStream())
        }
        val config = configProvider.load(configFile)
        socketPort = config.getInt("socketPort")
        ServerSocketManager.initServer(socketPort, this.logger)
    }

    @EventHandler
    fun onReceive(event: PluginMessageEvent) {
        if (event.tag != CHANNEL_NAME) return
        val buf = Unpooled.wrappedBuffer(event.data)
        when (buf.readString()) {
            ServerPacket.TP_POS.name -> {
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
                    writeString(ServerPacket.TP_POS.name)
                    writeUUID(playerUUID)
                    writeString(world)
                    writeDouble(x)
                    writeDouble(y)
                    writeDouble(z)
                    writeFloat(yaw)
                    writeFloat(pitch)
                }.toByteArray())
            }
            ServerPacket.TP_PLAYER.name -> {
                val playerUUID = buf.readUUID()
                val targetUUID = buf.readUUID()
                val proxyPlayer = ProxyServer.getInstance().getPlayer(playerUUID) ?: return
                val proxyTarget = ProxyServer.getInstance().getPlayer(targetUUID) ?: return
                proxyPlayer.connect(proxyTarget.server.info)
                proxyTarget.server.info.sendData(CHANNEL_NAME, Unpooled.buffer().apply {
                    writeString(ServerPacket.TP_PLAYER.name)
                    writeUUID(playerUUID)
                    writeUUID(targetUUID)
                }.toByteArray())
            }
        }
    }

    @EventHandler
    fun onDisconnect(event: PlayerDisconnectEvent) {
        val player = event.player
        ServerSocketManager.clientSockets.map { it.first }.forEach {
            it.sendData(ServerPacket.PLAYER_DISCONNECT) {
                writeUUID(player.uniqueId)
                writeString(player.name)
            }
        }
    }

}