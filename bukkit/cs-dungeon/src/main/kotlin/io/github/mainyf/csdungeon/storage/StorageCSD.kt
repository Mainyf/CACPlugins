package io.github.mainyf.csdungeon.storage

import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.newByID
import org.bukkit.Location
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and

object StorageCSD : AbstractStorageManager() {

    private val dungeonStructures = mutableListOf<DungeonStructure>()

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                DungeonStructures,
                DungeonMobSpawnLocs
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
            dungeonStructures.addAll(DungeonStructure.all())
        }
    }

    fun getDungeonMobSpawnLoc(dungeon: DungeonStructure): List<DungeonMobSpawnLoc> {
        return transaction { dungeon.mobSpawnLocs.toList() }
    }

    fun tryAddDungeonStructure(
        dungeonName: String,
        structureName: String,
        coreLoc: Location,
        minLoc: Location,
        maxLoc: Location,
        locs: List<Pair<Location, String>>
    ) {
        val worldName = minLoc.world.name
        transaction {
            val rs = DungeonStructure.find {
                (DungeonStructures.dungeonName eq dungeonName) and
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
                this.dungeonName = dungeonName
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
            dungeonStructures.add(dungeon)
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

    fun findAllDungeon(pageIndex: Int = 1, pageSize: Long = 10): List<DungeonStructure> {
        return transaction {
            DungeonStructure.all().orderBy(DungeonStructures.createTime to SortOrder.DESC)
                .limit(pageSize.toInt(), (pageIndex - 1) * pageSize)
                .toList()
        }
    }

    //    fun findDungeonByLoc(location: Location): DungeonStructure? {
    //        val worldName = location.world.name
    //        return transaction {
    //            val rs = DungeonStructure.find {
    //                (DungeonStructures.worldName eq worldName) and
    //                        (DungeonStructures.minX lessEq location.x) and
    //                        (DungeonStructures.minY lessEq location.y) and
    //                        (DungeonStructures.minZ lessEq location.z) and
    //                        (DungeonStructures.maxX greaterEq location.x) and
    //                        (DungeonStructures.maxY greaterEq location.y) and
    //                        (DungeonStructures.maxZ greaterEq location.z)
    //            }
    //            rs.firstOrNull()
    //        }
    //    }

    fun findDungeonByLoc(location: Location): DungeonStructure? {
        val worldName = location.world.name
        return dungeonStructures.find {
            it.worldName == worldName &&
                    location.x in it.minX .. it.maxX &&
                    location.y in it.minY .. it.maxY &&
                    location.z in it.minZ .. it.maxZ
        }
//        return transaction {
//            val rs = DungeonStructure.find {
//                (DungeonStructures.worldName eq worldName) and
//                        (DungeonStructures.minX lessEq location.x) and
//                        (DungeonStructures.minY lessEq location.y) and
//                        (DungeonStructures.minZ lessEq location.z) and
//                        (DungeonStructures.maxX greaterEq location.x) and
//                        (DungeonStructures.maxY greaterEq location.y) and
//                        (DungeonStructures.maxZ greaterEq location.z)
//            }
//            rs.firstOrNull()
//        }
    }

}