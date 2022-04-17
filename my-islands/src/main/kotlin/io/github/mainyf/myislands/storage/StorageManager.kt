package io.github.mainyf.myislands.storage

import io.github.mainyf.newmclib.serverId
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import org.bukkit.util.Vector
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.joda.time.DateTime
import java.util.*

object StorageManager : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            SchemaUtils.createMissingTablesAndColumns(PlayerIslandTable)
        }
    }

    fun createPlayerIsland(uuid: UUID, coreLoc: Vector) {
        transaction {
            PlayerIslandData.new(uuid) {
                createTime = DateTime.now()

                serverId = serverId()
                coreX = coreLoc.blockX
                coreY = coreLoc.blockY
                coreZ = coreLoc.blockZ
                visibility = false
                kudos = 0
            }
        }
    }

    fun getPlayerIsland(uuid: UUID): PlayerIslandData? {
        return transaction {
            PlayerIslandData.find { (PlayerIslandTable.id eq uuid) and (PlayerIslandTable.serverId eq serverId()) }
                .firstOrNull()
        }
    }

    fun setVisibility(uuid: UUID, visibility: Boolean) {
        transaction {
            val data = getPlayerIsland(uuid) ?: return@transaction
            data.visibility = visibility
        }
    }

    fun addKudos(uuid: UUID, count: Int = 1) {
        transaction {
            val data = getPlayerIsland(uuid) ?: return@transaction
            data.kudos += count
        }
    }

    fun updateCoreLoc(uuid: UUID, coreLoc: Vector) {
        transaction {
            val data = getPlayerIsland(uuid) ?: return@transaction
            data.coreX = coreLoc.blockX
            data.coreY = coreLoc.blockY
            data.coreZ = coreLoc.blockZ
        }
    }

    fun getIsLandsOrderByKudos(pageIndex: Int, pageSize: Int): List<PlayerIslandData> {
        return transaction {
            PlayerIslandData.all()
                .orderBy(PlayerIslandTable.kudos to SortOrder.DESC)
                .limit(pageSize, (pageIndex.toLong() - 1L) * pageSize.toLong())
                .toList()
        }
    }

}