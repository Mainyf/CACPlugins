package io.github.mainyf.worldsettings

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import dev.jorel.commandapi.CommandAPI
import io.github.mainyf.newmclib.exts.registerCommand
import io.github.mainyf.newmclib.exts.runTaskTimerBR
import io.github.mainyf.newmclib.exts.submitTask
import io.github.mainyf.worldsettings.config.ConfigManager
import io.github.mainyf.worldsettings.config.WorldSettingConfig
import io.github.mainyf.worldsettings.listeners.BlockListener
import io.github.mainyf.worldsettings.listeners.PlayerListener
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
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
//        server.pluginManager.getPlugin("AuthMe")
        CommandHandler.init()
        CommandHandler.register()
        Bukkit.getServer().pluginManager.registerEvents(BlockListener, this)
        Bukkit.getServer().pluginManager.registerEvents(PlayerListener, this)
        submitTask(
            delay = 20L,
            period = 20L
        ) {
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
        submitTask(
            delay = 5 * 20L,
            period = 5 * 20L
        ) {
            Bukkit.getWorlds().forEach {
                val settings = ConfigManager.getSetting(it) ?: return@forEach
                settings.gameRules.forEach { (rule, value) ->
                    val oldValue = it.getGameRuleValue(rule)
                    var log = "获取世界: ${it.name} 的规则: ${rule.name} 为: $oldValue"
                    if (oldValue != value) {
                        log += " 与配置不符，修改为: $value"
                        it.setGameRule(rule, value)
                    }
                    Log.debug(log)
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
        CommandAPI.unregister(CommandHandler.name)
        ProtocolLibrary.getProtocolManager().removePacketListener(packetListener)
        PlayerDropItemStorage.close()
    }

}

fun ignorePermAndGetWorldSettings(player: Player, world: World = player.world, block: (WorldSettingConfig) -> Unit) {
    if (player.hasPermission(ConfigManager.ignorePermission)) return
    val settings = ConfigManager.getSetting(world) ?: return
    block.invoke(settings)
}

fun getWorldSettings(player: Player, world: World = player.world, block: (WorldSettingConfig) -> Unit) {
    val settings = ConfigManager.getSetting(world) ?: return
    block.invoke(settings)
}