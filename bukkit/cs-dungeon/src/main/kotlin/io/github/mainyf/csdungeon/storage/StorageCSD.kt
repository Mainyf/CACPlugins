package io.github.mainyf.csdungeon.storage

import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.newByID
import org.bukkit.Location
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and

object StorageCSD : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                DungeonStructures,
                DungeonMobSpawnLocs
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun tryAddDungeonStructure(
        structureName: String,
        coreLoc: Location,
        minLoc: Location,
        maxLoc: Location,
        locs: List<Pair<Location, String>>
    ) {
        val worldName = minLoc.world.name
        transaction {
            val rs = DungeonStructure.find {
                (DungeonStructures.structureName eq structureName) and
                        (DungeonStructures.worldName eq worldName) and
                        (DungeonStructures.coreX eq coreLoc.blockX.toDouble()) and
                        (DungeonStructures.coreY eq coreLoc.blockY.toDouble()) and
                        (DungeonStructures.coreZ eq coreLoc.blockZ.toDouble())
            }.empty()
            if (!rs) {
                return@transaction
            }
            val dungeon = DungeonStructure.newByID {
                this.structureName = structureName
                this.worldName = worldName
                this.coreX = coreLoc.blockX.toDouble()
                this.coreY = coreLoc.blockY.toDouble()
                this.coreZ = coreLoc.blockZ.toDouble()
                this.minX = minLoc.x
                this.minY = minLoc.y
                this.minZ = minLoc.z
                this.maxX = maxLoc.x
                this.maxY = maxLoc.y
                this.maxZ = maxLoc.z
            }
            locs.forEach { (loc, mobName) ->
                DungeonMobSpawnLoc.newByID {
                    this.dungeon = dungeon.id
                    this.mobName = mobName
                    this.x = loc.blockX.toDouble()
                    this.y = loc.blockY.toDouble()
                    this.z = loc.blockZ.toDouble()
                }
            }
        }
    }

    fun findDungeonByLoc(location: Location): DungeonStructure? {
        val worldName = location.world.name
        return transaction {
            val rs = DungeonStructure.find {
                (DungeonStructures.worldName eq worldName) and
                        (DungeonStructures.minX lessEq location.x) and
                        (DungeonStructures.minY lessEq location.y) and
                        (DungeonStructures.minZ lessEq location.z) and
                        (DungeonStructures.maxX greaterEq location.x) and
                        (DungeonStructures.maxY greaterEq location.y) and
                        (DungeonStructures.maxZ greaterEq location.z)
            }
            rs.firstOrNull()
        }
    }

}