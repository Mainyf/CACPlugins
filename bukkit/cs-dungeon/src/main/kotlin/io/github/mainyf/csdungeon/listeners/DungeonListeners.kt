package io.github.mainyf.csdungeon.listeners

import com.ryandw11.structure.api.StructureSpawnEvent
import dev.lone.itemsadder.api.CustomFurniture
import dev.lone.itemsadder.api.CustomStack
import io.github.mainyf.csdungeon.config.ConfigCSD
import io.github.mainyf.csdungeon.config.sendLang
import io.github.mainyf.csdungeon.menu.DungeonMenu
import io.github.mainyf.csdungeon.storage.StorageCSD
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.text
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent

object DungeonListeners : Listener {

    @EventHandler
    fun onSSpawn(event: StructureSpawnEvent) {
        val structure = event.structure
        //        val minLoc = event.minimumPoint
        //        val maxLoc = event.maximumPoint
        val signs = event.containersAndSignsLocations
            .filter {
                it.block.type == Material.OAK_SIGN || it.block.type == Material.OAK_WALL_SIGN
            }
            .map { it to (it.block.state as? Sign) }
        var coreLoc: Location? = null
        var minLoc: Location? = null
        var maxLoc: Location? = null
        val mobLocs = mutableListOf<Pair<Location, String>>()
        signs.forEach { (loc, sign) ->
            val line = sign?.lines()?.getOrNull(0)?.text() ?: return@forEach
            when {
                line == "[core]" -> {
                    coreLoc = loc
                }

                line.contains("mob") -> {
                    kotlin.runCatching {
                        mobLocs.add(loc to line.replace("[", "").replace("]", "").replace("mob_", ""))
                    }.onFailure {
                        it.printStackTrace()
                    }
                }

                line == "[min_loc]" -> {
                    minLoc = loc
                }

                line == "[max_loc]" -> {
                    maxLoc = loc
                }
            }
        }
        val dungeonConfig = ConfigCSD.dungeonConfigMap.values.find {
            it.structureName == structure.name
        } ?: return
        if (coreLoc != null && minLoc != null && maxLoc != null) {
            mobLocs.forEach {
                val block = it.first.block
                block.type = Material.AIR
            }
            coreLoc!!.add(0.5, 0.0, 0.5)
            coreLoc!!.block.type = Material.AIR
            CustomFurniture.spawnPreciseNonSolid(ConfigCSD.dungeonCoreId, coreLoc)!!.apply {
                armorstand?.isInvulnerable = true
            }
            minLoc!!.block.type = Material.AIR
            maxLoc!!.block.type = Material.AIR
            val locPair = getMinAndMax(minLoc!!, maxLoc!!)
            StorageCSD.tryAddDungeonStructure(
                dungeonConfig.dungeonName,
                structure.name,
                coreLoc!!,
                locPair.first,
                locPair.second,
                mobLocs
            )
        }
    }

    fun getMinAndMax(posA: Location, posB: Location): Pair<Location, Location> {
        val hX: Double
        val hY: Double
        val hZ: Double
        val lX: Double
        val lY: Double
        val lZ: Double
        if (posA.x > posB.x) {
            hX = posA.x
            lX = posB.x
        } else {
            hX = posB.x
            lX = posA.x
        }
        if (posA.y > posB.y) {
            hY = posA.y
            lY = posB.y
        } else {
            hY = posB.y
            lY = posA.y
        }
        if (posA.z > posB.z) {
            hZ = posA.z
            lZ = posB.z
        } else {
            hZ = posB.z
            lZ = posA.z
        }
        return Location(posA.world, lX, lY, lZ) to Location(posA.world, hX, hY, hZ)
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val loc = event.block.location
        val dungeon = StorageCSD.findDungeonByLoc(loc)
        if (event.player.isOp) return
        if (dungeon != null) {
            val dungeonConfig = ConfigCSD.dungeonConfigMap.values.find { it.structureName == dungeon.structureName }
            if (dungeonConfig?.protectBuild == true) {
                event.player.sendLang("noBreakDungeon")
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onInteractAE(event: PlayerInteractAtEntityEvent) {
        val entity = event.rightClicked
        if (entity is ArmorStand && isCore(entity)) {
            val player = event.player
            val dungeon = StorageCSD.findDungeonByLoc(player.location) ?: return
            val furniture = CustomFurniture.byAlreadySpawned(entity)
            if (furniture != null) {
                DungeonMenu().open(event.player)
            }
        }
    }

    @EventHandler
    fun onSpawn(event: CreatureSpawnEvent) {
        val dungeon = StorageCSD.findDungeonByLoc(event.location)
        if (dungeon != null && event.spawnReason != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            event.isCancelled = true
        }
    }

    fun isCore(armorStand: ArmorStand): Boolean {
        if (CustomFurniture.byAlreadySpawned(armorStand) == null) {
            return false
        }
        return CustomStack.byItemStack(armorStand.equipment.helmet)?.namespacedID == ConfigCSD.dungeonCoreId
    }

}