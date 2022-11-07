package io.github.mainyf.worldsettings

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import dev.jorel.commandapi.CommandAPI
import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.exts.submitTask
import io.github.mainyf.newmclib.protocolManager
import io.github.mainyf.worldsettings.config.ConfigWS
import io.github.mainyf.worldsettings.config.WorldSettingConfig
import io.github.mainyf.worldsettings.listeners.BlockListener
import io.github.mainyf.worldsettings.listeners.EntityListener
import io.github.mainyf.worldsettings.listeners.PlayerListener
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

class WorldSettings : BasePlugin() {

    companion object {

        lateinit var INSTANCE: WorldSettings

    }

    private var packetListener: PacketAdapter? = null

    override fun enable() {
        INSTANCE = this
        ConfigWS.load()
        PlayerDropItemStorage.init(this)
        //        server.pluginManager.getPlugin("AuthMe")
        CommandHandler.init()
        CommandHandler.register()
        Bukkit.getServer().pluginManager.registerEvents(EntityListener, this)
        Bukkit.getServer().pluginManager.registerEvents(BlockListener, this)
        Bukkit.getServer().pluginManager.registerEvents(PlayerListener, this)
        submitTask(delay = 20L, period = 5L) {
            Bukkit.getWorlds().filter { it.environment == World.Environment.THE_END }.forEach { world ->
                val settings = ConfigWS.getSetting(world) ?: return@submitTask
                if (!settings.antiSpawnEnderDragonEgg) return@submitTask
                repeat(9) {
                    val eggLoc = world.getBlockAt(0, 58 + it, 0)
                    if (eggLoc.type == Material.DRAGON_EGG) {
                        eggLoc.type = Material.AIR
                    }
                }
            }
        }
        submitTask(
            delay = 20L,
            period = 20L
        ) {
            Bukkit.getWorlds().forEach {
                val settings = ConfigWS.getSetting(it) ?: return@forEach
                if (it.difficulty != settings.difficulty) {
                    it.difficulty = settings.difficulty
                }
            }
            Bukkit.getOnlinePlayers().forEach { player ->
                if (player.hasPermission(ConfigWS.ignorePermission)) return@forEach
                val settings = ConfigWS.getSetting(player.location) ?: return@forEach
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
                val settings = ConfigWS.getSetting(it) ?: return@forEach
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
                val settings = ConfigWS.getSetting(player.location) ?: return
                if (player.hasPermission(ConfigWS.ignorePermission)) return
                if (!settings.tabComplete) {
                    event.isCancelled = true
                }
            }

        }
        protocolManager().addPacketListener(packetListener)
        protocolManager().addPacketListener(object : PacketAdapter(this, PacketType.Play.Server.ENTITY_SOUND) {

            override fun onPacketSending(event: PacketEvent) {
                val player = event.player
                val settings = ConfigWS.getSetting(player.location) ?: return
                val soundName = event.packet.soundEffects.read(0).name
                if (settings.antiGoatHornSound && soundName.startsWith("ITEM_GOAT_HORN_SOUND_")) {
                    event.isCancelled = true
                }
            }

        })
    }

    override fun onDisable() {
        CommandAPI.unregister(CommandHandler.name)
        ProtocolLibrary.getProtocolManager().removePacketListener(packetListener)
        PlayerDropItemStorage.close()
    }

}

fun ignorePermAndGetWorldSettings(entity: Entity?, world: World? = entity?.world, block: (WorldSettingConfig) -> Unit) {
    if (entity?.hasPermission(ConfigWS.ignorePermission) == true) return
    val settings =
        (if (entity == null) ConfigWS.getSetting(world) else ConfigWS.getSetting(entity.location)) ?: return
    //    val settings = ConfigManager.getSetting(world) ?: return
    block.invoke(settings)
}

fun getWorldSettings(player: Player, world: World = player.world, block: (WorldSettingConfig) -> Unit) {
    val settings = ConfigWS.getSetting(player.location) ?: return
    //    val settings = ConfigManager.getSetting(world) ?: return
    block.invoke(settings)
}