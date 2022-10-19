package io.github.mainyf.csdungeon

import com.ryandw11.structure.api.StructureSpawnEvent
import io.github.mainyf.csdungeon.config.ConfigCSD
import io.github.mainyf.csdungeon.storage.StorageCSD
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.text
import org.apache.logging.log4j.LogManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.plugin.java.JavaPlugin

class CsDungeon : JavaPlugin(), Listener {

    companion object {

        val LOGGER = LogManager.getLogger("CsDungeon")

        lateinit var INSTANCE: CsDungeon

    }

    override fun onEnable() {
        INSTANCE = this
        ConfigCSD.load()
        StorageCSD.init()

        pluginManager().registerEvents(this, this)
    }

    @EventHandler
    fun onSSpawn(event: StructureSpawnEvent) {
        val structure = event.structure
        val minLoc = event.minimumPoint
        val maxLoc = event.maximumPoint
        val signs = event.containersAndSignsLocations
            .filter {
                it.block.type == Material.OAK_SIGN
            }
            .map { it to (it.block.state as? Sign) }
        var coreLoc: Location? = null
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
            }
        }
        if (coreLoc != null) {
            StorageCSD.tryAddDungeonStructure(structure.name, coreLoc!!, minLoc, maxLoc, mobLocs)
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val loc = event.block.location
        val dungeon = StorageCSD.findDungeonByLoc(loc)
        if (event.player.isOp) return
        if (dungeon != null) {
            val dungeonConfig = ConfigCSD.dungeonConfigList.find { it.structureName == dungeon.structureName }
            if (dungeonConfig?.protectBuild == true) {
                event.player.msg("你无法破坏 ${dungeon.structureName} 遗迹")
                event.isCancelled = true
            }
        }
    }

}