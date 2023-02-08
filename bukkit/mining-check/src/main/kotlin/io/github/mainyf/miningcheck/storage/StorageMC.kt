package io.github.mainyf.miningcheck.storage

import io.github.mainyf.miningcheck.config.ConfigMC
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.newByID
import org.bukkit.Location
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.joda.time.DateTime
import java.util.*

object StorageMC : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                PlayerCountActionNums
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun reset() {
        transaction {
            PlayerCountActionNums.deleteAll()
        }
    }

    fun getPlayerActionNum(config: ConfigMC.WorldConfig, uuid: UUID): Int {
        return transaction {
            val playerCANum = PlayerCountActionNum.find {
                (PlayerCountActionNums.worldName eq config.worldName) and (PlayerCountActionNums.playerUUID eq
                        uuid)
            }.firstOrNull() ?: return@transaction 0

            if (hasExpired(config, playerCANum)) {
                playerCANum.startTime = DateTime.now().withTimeAtStartOfDay()
                playerCANum.countActionNum = 0
                0
            } else playerCANum.countActionNum
        }
    }

    fun setPlayerActionNum(config: ConfigMC.WorldConfig, uuid: UUID, num: Int) {
        transaction {
            val playerCANum = PlayerCountActionNum.find {
                (PlayerCountActionNums.worldName eq config.worldName) and (PlayerCountActionNums.playerUUID eq
                        uuid)
            }.firstOrNull()
            if (playerCANum == null) {
                PlayerCountActionNum.newByID {
                    this.playerUUID = uuid
                    this.worldName = config.worldName
                    this.countActionNum = num
                    this.startTime = DateTime.now().withTimeAtStartOfDay()
                }
            } else {
                if (hasExpired(config, playerCANum)) {
                    playerCANum.startTime = DateTime.now().withTimeAtStartOfDay()
                    playerCANum.countActionNum = 0
                } else {
                    playerCANum.countActionNum = num
                }
            }
        }
    }

    private fun hasExpired(config: ConfigMC.WorldConfig, playerCANum: PlayerCountActionNum): Boolean {
        val curTime = DateTime.now().withTimeAtStartOfDay()
        val curDay = curTime.millis / 1000 / 60 / 60 / 24
        val startDay = playerCANum.startTime.millis / 1000 / 60 / 60 / 24
        return curDay - startDay >= config.actionResetTime
    }

}