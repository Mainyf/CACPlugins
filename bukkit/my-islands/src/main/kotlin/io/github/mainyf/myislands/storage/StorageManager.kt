package io.github.mainyf.myislands.storage

import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.menu.IslandViewListType
import io.github.mainyf.myislands.menu.IslandViewListType.*
import io.github.mainyf.newmclib.exts.pagination
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.newByID
import io.github.mainyf.newmclib.utils.Heads
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.joda.time.LocalDate
import java.util.*

object StorageManager : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                PlayerIslands,
                PlayerIslandHelpers,
                PlayerKudoLogs
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
        transaction {
            val list = PlayerIsland.all()
            var count = 0
            list.forEach {
                val offlineData = it.id.value.asOfflineData() ?: return@forEach
                val pName = offlineData.name
                count++
                kotlin.runCatching {
                    Heads.initPlayerHead(pName)
                }.onFailure { e ->
                    MyIslands.LOGGER.error("初始化玩家 $pName 头颅数据时出现异常", e)
                }
            }
            MyIslands.LOGGER.info("成功初始化 $count 个岛屿玩家头颅数据")
        }
    }

    fun createPlayerIsland(uuid: UUID, coreLoc: Vector) {
        transaction {
            PlayerIsland.newByID(uuid) {

                coreX = coreLoc.blockX
                coreY = coreLoc.blockY
                coreZ = coreLoc.blockZ
                visibility = IslandVisibility.ALL
                kudos = 0

            }
        }
    }

    fun removePlayerIsland(island: PlayerIsland) {
        transaction {
            island.helpers.forEach {
                it.delete()
            }
            PlayerKudoLogs.deleteWhere {
                PlayerKudoLogs.island eq island.id
            }
//                PlayerIslandHelperData.find { PlayerIslandHelpers.island eq data.id }.forEach {
//                    it.delete()
//                }
            island.delete()
        }
    }

    fun getPlayerIsland(uuid: UUID): PlayerIsland? {
        return transaction {
            PlayerIsland.findById(uuid)
        }
    }

    fun setVisibility(island: PlayerIsland, visibility: IslandVisibility) {
        transaction {
            island.visibility = visibility
        }
    }

    fun addKudos(source: UUID, island: PlayerIsland): Boolean {
        return transaction {
            val curTime = LocalDate.now().toDateTimeAtStartOfDay()
            val kudoData = PlayerKudoLog.find {
                (PlayerKudoLogs.island eq island.id) and (PlayerKudoLogs.kudoDate eq curTime)
            }
            if (!kudoData.empty()) {
                return@transaction false
            }

            island.kudos += 1

            PlayerKudoLog.newByID {
                this.island = island.id
                this.kudoUUID = source
                this.kudoDate = LocalDate.now().toDateTimeAtStartOfDay()
            }
            true
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

    fun hasPermission(player: Player, owner: UUID): Boolean {
        return transaction {
            val ownerData = getPlayerIsland(owner) ?: return@transaction false

            ownerData.helpers.any { it.helperUUID == player.uuid }
        }
    }

    fun addHelpers(island: PlayerIsland, helper: UUID) {
        transaction {
            PlayerIslandHelper.newByID {
                this.island = island.id
                this.helperUUID = helper
            }
        }
    }

    fun getIsLandsCount(player: Player, type: IslandViewListType): Int {
        return transaction {
            when (type) {
                ALL -> PlayerIsland.count(PlayerIslands.visibility eq IslandVisibility.ALL).toInt()
                FRIEND -> PlayerIsland.count(PlayerIslands.visibility neq IslandVisibility.NONE).toInt()
                PERMISSION -> {
                    val islandDatas =
                        PlayerIsland.find { PlayerIslands.visibility neq IslandVisibility.NONE }

                    islandDatas.filter { iData ->
                        iData.helpers.any {
                            it.helperUUID == player.uuid
                        }
                    }.size
                }
            }
        }
    }

    fun getIsLandsOrderByKudos(
        pageIndex: Int,
        pageSize: Int,
        player: Player,
        type: IslandViewListType
    ): List<PlayerIsland> {
        return synchronized(this) {
            transaction {
                when (type) {
                    ALL -> PlayerIsland
                        .find { PlayerIslands.visibility eq IslandVisibility.ALL }
                        .orderBy(PlayerIslands.kudos to SortOrder.DESC)
                        .pagination(pageIndex, pageSize)
                        .toList()
                    FRIEND -> PlayerIsland
                        .find { PlayerIslands.visibility neq IslandVisibility.NONE }
                        .orderBy(PlayerIslands.kudos to SortOrder.DESC)
                        .pagination(pageIndex, pageSize)
                        .toList()
                    PERMISSION -> {
                        val list = mutableListOf<PlayerIsland>()
                        val islandData = getPlayerIsland(player.uuid)
                        if (islandData != null) {
                            val helpers = islandData.helpers.map { it.helperUUID }
                            list.addAll(PlayerIsland.find { (PlayerIslands.visibility neq IslandVisibility.NONE) and (PlayerIslands.id inList helpers) })
                        }
//                        val islandDatas = PlayerIsland.find { PlayerIslands.visibility neq IslandVisibility.NONE }
//
//                        islandDatas.filter { iData ->
//                            /*iData.id.value == player.uuid || */iData.helpers.any {
//                            it.helperUUID == player.uuid
//                        }
//                        }

                        list.pagination(pageIndex, pageSize)
                    }
                }
            }
        }
    }

}