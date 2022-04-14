package io.github.mainyf.worldsettings

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import io.github.mainyf.newmclib.exts.registerCommand
import io.github.mainyf.newmclib.exts.runTaskTimerBR
import io.github.mainyf.worldsettings.config.ConfigManager
import io.github.mainyf.worldsettings.listeners.BlockListener
import io.github.mainyf.worldsettings.listeners.PlayerListener
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class WorldSettings : JavaPlugin() {

    companion object {

        lateinit var INSTANCE: WorldSettings

    }

    private var packetListener: PacketAdapter? = null

    override fun onEnable() {
        INSTANCE = this
        saveDefaultConfig()
        reloadConfig()
        ConfigManager.load(config)
        PlayerDropItemStorage.init(this)
        registerCommand("wsett", CommandHandler)
        Bukkit.getServer().pluginManager.registerEvents(BlockListener, this)
        Bukkit.getServer().pluginManager.registerEvents(PlayerListener, this)
        runTaskTimerBR(20L, 20L) {
            Bukkit.getWorlds().forEach {
                val settings = ConfigManager.getSetting(it) ?: return@forEach
                if (it.difficulty != settings.difficulty) {
                    it.difficulty = settings.difficulty
                }
            }
            Bukkit.getOnlinePlayers().forEach { player ->
                if (player.hasPermission(ConfigManager.ignorePermission)) return@forEach
                val settings = ConfigManager.getSetting(player.world) ?: return@forEach
                if (settings.antiFly && player.isFlying) {
                    player.allowFlight = false
                    player.isFlying = false
                    settings.flyBlockAction?.execute(player)
                }
                if (player.gameMode != settings.gameMode) {
                    player.gameMode = settings.gameMode
                }
            }
        }
        Bukkit.getWorlds().forEach {
            val settings = ConfigManager.getSetting(it) ?: return@forEach
            settings.gameRules.forEach { (rule, value) ->
                val oldValue = it.getGameRuleValue(rule)
                if (oldValue != value) {
                    it.setGameRule(rule, value)
                }
            }
        }
        packetListener = object : PacketAdapter(this, PacketType.Play.Client.TAB_COMPLETE) {

            override fun onPacketReceiving(event: PacketEvent) {
                val player = event.player
                val settings = ConfigManager.getSetting(player.world) ?: return
                if (player.hasPermission(ConfigManager.ignorePermission)) return
                if (!settings.tabComplete) {
                    event.isCancelled = true
                }
            }

        }
        ProtocolLibrary.getProtocolManager().addPacketListener(packetListener)
    }

    override fun onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListener(packetListener)
        PlayerDropItemStorage.close()
    }

}