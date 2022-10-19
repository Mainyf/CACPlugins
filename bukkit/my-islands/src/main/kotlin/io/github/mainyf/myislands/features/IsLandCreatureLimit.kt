package io.github.mainyf.myislands.features

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.plotsquared.bukkit.util.BukkitUtil
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.config.ConfigMI
import io.github.mainyf.myislands.config.CreatureType
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.myislands.storage.StorageMI
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.onlinePlayers
import io.github.mainyf.newmclib.exts.submitTask
import io.github.mainyf.newmclib.exts.uuid
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.player.PlayerInteractEvent

object IsLandCreatureLimit : Listener {

    private val buckets = arrayOf(
        Material.PUFFERFISH_BUCKET,
        Material.SALMON_BUCKET,
        Material.COD_BUCKET,
        Material.TROPICAL_FISH_BUCKET,
        Material.AXOLOTL_BUCKET,
        Material.TADPOLE_BUCKET
    )

    fun init() {
        MyIslands.INSTANCE.submitTask(period = ConfigMI.creatureLimitConfig.period) {
            onlinePlayers().forEach { player ->
                CreatureType.values().forEach {
                    val count = StorageMI.getCreatureCount(player.uuid, it)
                    val config = ConfigMI.creatureLimitConfig.get(it)
                    if (count > config.count) {
                        config.msg?.execute(player, "{number}", count)
                    }
                }
            }
        }
        MyIslands.INSTANCE.submitTask(period = 20L) {
            val world = Bukkit.getWorld("plotworld") ?: return@submitTask
            val entities = world.entities
            CreatureType.values().forEach { cType ->
                onlinePlayers().forEach { player ->
                    var count = 0
//                    var locs = mutableListOf<Location>()
                    entities.forEach eLoop@{
                        val type = CreatureType.from(it)
                        if (cType != type) return@eLoop
                        val eLoc = BukkitUtil.adaptComplete(it.location)
                        val plot = eLoc.plotAbs ?: return@eLoop
                        if (plot.owner == player.uuid) {
                            count++
//                            locs.add(it.location)
                        }
                    }
                    StorageMI.setCreatureCount(player.uuid, cType, count)
//                    println("玩家: ${player.name} 的 ${cType} 被更新为 ${count}")
//                    locs.forEach {
//                        println("玩家: ${player.name} 的 ${cType}: ${it.x} ${it.y} ${it.z}")
//                    }
                }
            }
        }
    }

    private fun hasLimit(player: Player): Boolean {
        return CreatureType.values().any {
            val count = StorageMI.getCreatureCount(player.uuid, it)
            val config = ConfigMI.creatureLimitConfig.get(it)
            count > config.count
        }
    }

    @EventHandler
    fun onEntityRemove(event: EntityRemoveFromWorldEvent) {
        val entity = event.entity
        val plotLoc = BukkitUtil.adaptComplete(entity.location)
        val plot = plotLoc.plotAbs ?: return
        if (plot.owner == null) return
        val type = CreatureType.from(entity)
        if (type != null) {
            StorageMI.removeCreatureCount(plot.owner!!, type, 1)
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return
        val type = item.type
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        if (type != Material.DRAGON_EGG && (type.name.contains("_EGG") || buckets.contains(type))) {
            var cType: CreatureType? = null
            var curCount = 0
            var maxCount = 0
            for (it in CreatureType.values()) {
                val count = StorageMI.getCreatureCount(player.uuid, it)
                val config = ConfigMI.creatureLimitConfig.get(it)
                if (count > config.count) {
                    cType = it
                    curCount = count
                    maxCount = config.count
                    break
                }
            }
            if (cType != null) {
                player.sendLang(
                    "useEggcreatureLimit",
                    "{cType}", cType.text,
                    "{curCount}", curCount,
                    "{maxCount}", maxCount
                )
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onSpawn(event: CreatureSpawnEvent) {
        val entity = event.entity
        val plotLoc = BukkitUtil.adaptComplete(entity.location)
        val plot = plotLoc.plotAbs ?: return
        if (plot.owner == null) return
        if (CreatureType.values().any {
                val count = StorageMI.getCreatureCount(plot.owner!!, it)
                val config = ConfigMI.creatureLimitConfig.get(it)
                count > config.count
            }) {
            if (event.spawnReason != CreatureSpawnEvent.SpawnReason.SLIME_SPLIT) {
                event.isCancelled = true
                return
            }
        }
        val type = CreatureType.from(entity) ?: return
//        val config = ConfigMI.creatureLimitConfig.get(type)
//        val count = StorageMI.getCreatureCount(plot.owner!!, type)
//        if (count > config.count) {
//            event.isCancelled = true
//            return
//        }
        StorageMI.addCreatureCount(plot.owner!!, type, 1)
    }

}