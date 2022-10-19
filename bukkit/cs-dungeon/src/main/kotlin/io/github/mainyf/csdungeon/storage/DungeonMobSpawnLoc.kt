package io.github.mainyf.csdungeon.storage

import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object DungeonMobSpawnLocs : BaseTable("t_DungeonMobSpawnLocs", true) {

    val dungeon = reference("dungeon", DungeonStructures)

    val mobName = varchar("mob_name", 255)

    val x = double("x")

    val y = double("y")

    val z = double("z")

}

class DungeonMobSpawnLoc(uuid: EntityID<UUID>) : BaseEntity(DungeonMobSpawnLocs, uuid) {

    companion object : UUIDEntityClass<DungeonMobSpawnLoc>(DungeonMobSpawnLocs)

    var dungeon by DungeonMobSpawnLocs.dungeon

    var mobName by DungeonMobSpawnLocs.mobName

    var x by DungeonMobSpawnLocs.x

    var y by DungeonMobSpawnLocs.y

    var z by DungeonMobSpawnLocs.z

}