package io.github.mainyf.myislands.storage

import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.menu.IslandViewListType
import io.github.mainyf.myislands.menu.IslandViewListType.*
import io.github.mainyf.newmclib.exts.onlinePlayers
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
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.util.*
import kotlin.math.max
import kotlin.system.measureTimeMillis

object StorageManager : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                PlayerIslands,
                PlayerIslandHelpers,
                PlayerKudoLogs,
                PlayerResetIslandCooldowns
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
            val islands = PlayerIsland.findById(uuid)
            if (islands == null) {
                PlayerIsland.newByID(uuid) {
                    coreX = coreLoc.blockX
                    coreY = coreLoc.blockY
                    coreZ = coreLoc.blockZ
                    visibility = IslandVisibility.ALL
                    kudos = 0
                    heats = 0
                }
            } else {
                islands.coreX = coreLoc.blockX
                islands.coreY = coreLoc.blockY
                islands.coreZ = coreLoc.blockZ
                islands.visibility = IslandVisibility.ALL
                islands.kudos = 0
                islands.heats = 0
            }
        }
    }

    fun removePlayerIsland(island: PlayerIsland, hasReset: Boolean) {
        transaction {
            island.helpers.forEach {
                it.delete()
            }
            PlayerKudoLogs.deleteWhere {
                PlayerKudoLogs.island eq island.id
            }
//            island.delete()
            if (hasReset) {
                val ri = PlayerResetIslandCooldown.findById(island.id)
                if (ri == null) {
                    PlayerResetIslandCooldown.newByID {
                        this.island = island.id
                        this.prevTime = DateTime.now()
                    }
                } else {
                    ri.prevTime = DateTime.now()
                }
            }
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

    fun updateHeat() {
        transaction {
            val nowTime = DateTime.now()
            val nowTimeMillis = nowTime.withTimeAtStartOfDay().millis
            PlayerIsland.all().forEach { island ->
                val prevAttenuation = island.heatAttenuationDateTime.withTimeAtStartOfDay().millis
                val day = (nowTimeMillis - prevAttenuation) / 1000 / 3600 / 24
                if (day >= 1) {
                    island.heats = max(0, island.heats - 1)
                    island.heatAttenuationDateTime = nowTime
                }
            }
        }
    }

    fun initHeat() {
        PlayerIsland.all().forEach { island ->
            island.heats = island.kudos
            island.heatAttenuationDateTime = DateTime.now()
        }
    }

    fun addKudos(source: UUID, island: PlayerIsland): Boolean {
        return transaction {
            val curTime = LocalDate.now().toDateTimeAtStartOfDay()
            val kudoData = PlayerKudoLog.find {
                (PlayerKudoLogs.island eq island.id) and (PlayerKudoLogs.kudoDate eq curTime) and (PlayerKudoLogs.kudoUUID eq source)
            }
            if (!kudoData.empty()) {
                return@transaction false
            }

            island.kudos += 1
            island.heats += 1

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

    fun removeHelpers(island: PlayerIsland, helper: UUID) {
        transaction {
            PlayerIslandHelpers.deleteWhere {
                (PlayerIslandHelpers.island eq island.id) and (PlayerIslandHelpers.helperUUID eq helper)
            }
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
                ONLINE -> PlayerIsland.find(PlayerIslands.visibility neq IslandVisibility.NONE).let { islands ->
                    val onlinePlayers = onlinePlayers()
                    islands.filter { island -> onlinePlayers.any { it.uuid == island.id.value } }.size
                }
                FRIEND -> PlayerIsland.count(PlayerIslands.visibility neq IslandVisibility.NONE).toInt()
                PERMISSION -> {
                    PlayerIsland.find { PlayerIslands.visibility neq IslandVisibility.NONE }
                        .filter { island ->
                            island.helpers.any {
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
            sortIsland(transaction {
                when (type) {
                    ALL -> PlayerIsland
                        .find { PlayerIslands.visibility eq IslandVisibility.ALL }
                        .orderBy(PlayerIslands.heats to SortOrder.DESC)
//                        .pagination(pageIndex, pageSize)
                        .toList()
                    ONLINE -> PlayerIsland
                        .find { PlayerIslands.visibility eq IslandVisibility.ALL }
                        .orderBy(PlayerIslands.heats to SortOrder.DESC)
                        .let { islands ->
                            val onlinePlayers = onlinePlayers()
                            islands.filter { island -> onlinePlayers.any { it.uuid == island.id.value } }
                        }
//                        .pagination(pageIndex, pageSize)
                        .toList()
                    FRIEND -> PlayerIsland
                        .find { PlayerIslands.visibility neq IslandVisibility.NONE }
                        .orderBy(PlayerIslands.heats to SortOrder.DESC)
//                        .pagination(pageIndex, pageSize)
                        .toList()
                    PERMISSION -> {
                        PlayerIsland.find { PlayerIslands.visibility neq IslandVisibility.NONE }
                            .filter { island ->
                                island.helpers.any {
                                    it.helperUUID == player.uuid
                                }
                            }
//                            .pagination(pageIndex, pageSize)
//                        val list = mutableListOf<PlayerIsland>()
//                        val islandData = getPlayerIsland(player.uuid)
//                        if (islandData != null) {
//                            val helpers = islandData.helpers.map { it.helperUUID }
//                            list.addAll(PlayerIsland.find { (PlayerIslands.visibility neq IslandVisibility.NONE) and (PlayerIslands.id inList helpers) })
//                        }
//                        list.pagination(pageIndex, pageSize)
                    }
                }
            }, pageIndex, pageSize)
        }
    }

    private fun sortIsland(list: List<PlayerIsland>, pageIndex: Int, pageSize: Int): List<PlayerIsland> {
        return list.sortedWith { a, b ->
            when {
                a.heats > b.heats -> -1
                a.heats < b.heats -> 1
                a.heats == b.heats -> {
                    (a.id.value.asOfflineData()?.name ?: "").compareTo(b.id.value.asOfflineData()?.name ?: "")
                }
                a.heats == b.heats -> 0
                else -> -1
            }
        }.pagination(pageIndex, pageSize)
    }

}