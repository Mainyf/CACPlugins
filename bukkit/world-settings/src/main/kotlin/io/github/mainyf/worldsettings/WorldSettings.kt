package io.github.mainyf.worldsettings

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.executors.ExecutorType
import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.offlinePlayerArguments
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.submitTask
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.protocolManager
import io.github.mainyf.worldsettings.config.ConfigWS
import io.github.mainyf.worldsettings.config.WorldSettingConfig
import io.github.mainyf.worldsettings.hooks.MythicHooks
import io.github.mainyf.worldsettings.listeners.BlockListener
import io.github.mainyf.worldsettings.listeners.EntityListener
import io.github.mainyf.worldsettings.listeners.PlayerListener
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.craftbukkit.v1_19_R1.CraftServer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.UUID

class WorldSettings : BasePlugin() {

    companion object {

        lateinit var INSTANCE: WorldSettings

    }

    private var packetListener: PacketAdapter? = null
    private val ignoreFlys = mutableSetOf<UUID>()

    override fun enable() {
        INSTANCE = this
        ConfigWS.load()
        PlayerDropItemStorage.init(this)
        //        //        server.pluginManager.getPlugin("AuthMe")
        CommandHandler.init()
        CommandHandler.register()
        Bukkit.getServer().pluginManager.registerEvents(EntityListener, this)
        Bukkit.getServer().pluginManager.registerEvents(BlockListener, this)
        Bukkit.getServer().pluginManager.registerEvents(PlayerListener, this)
        MythicHooks.init()
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
                if (!hasIgnoreFly(player.uuid)) {
                    if (settings.antiFly && player.isFlying) {
                        player.allowFlight = false
                        player.isFlying = false
                        settings.flyBlockAction?.execute(player)
                    }
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
//        val packetType = PacketType.findCurrent(PacketType.Protocol.PLAY, PacketType.Sender.SERVER, 0x0D)
//        protocolManager().addPacketListener(object : PacketAdapter(this, PacketType.Play.Server.TAB_COMPLETE) {
//
//            override fun onPacketSending(event: PacketEvent?) {
//                println("onPacketSending")
//            }
//
//            override fun onPacketReceiving(event: PacketEvent?) {
//                println("onPacketReceiving")
//            }
//
//        })
        //        apiCommand("demo") {
        //            "aaa" {
        //                withArguments(offlinePlayerArguments("玩家"))
        //                executePlayer {
        //                    sender.msg("信息")
        //                }
        //            }
        //        }
    }

    override fun onDisable() {
        CommandAPI.unregister(CommandHandler.name)
        ProtocolLibrary.getProtocolManager().removePacketListener(packetListener)
        PlayerDropItemStorage.close()
    }

    fun hasIgnoreFly(uuid: UUID): Boolean {
        return ignoreFlys.contains(uuid)
    }

    fun ignoreFly(uuid: UUID) {
        ignoreFlys.add(uuid)
    }

    fun unIgnoreFly(uuid: UUID) {
        ignoreFlys.remove(uuid)
    }

}

fun ignorePermAndGetWorldSettings(
    entity: Entity?,
    location: Location? = entity?.location,
    block: (WorldSettingConfig) -> Unit
) {
    if (entity?.hasPermission(ConfigWS.ignorePermission) == true) return
    //    val settings =
    //        (if (entity == null) ConfigWS.getSetting(world) else ConfigWS.getSetting(location!!)) ?: return
    val settings = getWorldSetting(location!!) ?: return
    //    val settings = ConfigManager.getSetting(world) ?: return
    block.invoke(settings)
}

fun getWorldSetting(location: Location): WorldSettingConfig? {
    return ConfigWS.getSetting(location)
}

fun getWorldSettings(player: Player, block: (WorldSettingConfig) -> Unit) {
    val settings = getWorldSetting(player.location) ?: return
    //    val settings = ConfigManager.getSetting(world) ?: return
    block.invoke(settings)
}