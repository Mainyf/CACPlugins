package io.github.mainyf.csdungeon.storage

import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.bukkit.Location
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object DungeonStructures : BaseTable("t_DungeonStructures", true) {

    val dungeonName = varchar("dungeon_name", 255)

    val structureName = varchar("structure_name", 255)

    val worldName = varchar("world_name", 255)

    val coreX = double("core_x")

    val coreY = double("core_y")

    val coreZ = double("core_z")

    val minX = double("min_x")

    val minY = double("min_y")

    val minZ = double("min_z")

    val maxX = double("max_x")

    val maxY = double("max_y")

    val maxZ = double("max_z")

}

class DungeonStructure(uuid: EntityID<UUID>) : BaseEntity(DungeonStructures, uuid) {

    companion object : UUIDEntityClass<DungeonStructure>(DungeonStructures)

    var dungeonName by DungeonStructures.dungeonName

    var structureName by DungeonStructures.structureName

    var worldName by DungeonStructures.worldName

    var coreX by DungeonStructures.coreX

    var coreY by DungeonStructures.coreY

    var coreZ by DungeonStructures.coreZ

    var minX by DungeonStructures.minX

    var minY by DungeonStructures.minY

    var minZ by DungeonStructures.minZ

    var maxX by DungeonStructures.maxX

    var maxY by DungeonStructures.maxY

    var maxZ by DungeonStructures.maxZ

    val mobSpawnLocs by DungeonMobSpawnLoc referrersOn DungeonMobSpawnLocs.dungeon

    fun containsDungeonArea(loc: Location): Boolean {
        return loc.world?.name == worldName && loc.x in minX..maxX && loc.y in minY..maxY && loc.z in minZ..maxZ
    }

}
