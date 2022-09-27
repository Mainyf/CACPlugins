package io.github.mainyf.questextension.storage

import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.insertByID
import io.github.mainyf.newmclib.storage.newByID
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.joda.time.DateTime

object StorageManager : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                PlayerDailyQuests
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun findDailyQuest(player: Player): PlayerDailyQuest? {
        return transaction {
            val iter = PlayerDailyQuest.find { (PlayerDailyQuests.pUUID eq player.uuid) }
                .orderBy(PlayerDailyQuests.questStartTime to SortOrder.DESC)
            iter.firstOrNull()
        }
    }

    fun addDailyQuestData(player: Player, list: List<String>): PlayerDailyQuest {
        return transaction {
            PlayerDailyQuest.newByID {
                this.pUUID = player.uuid
                this.questStartTime = DateTime.now().withTimeAtStartOfDay()
                this.questListText = list.joinToString(",")
            }
//            PlayerDailyQuests.insertByID {
//                it[pUUID] = player.uuid
//                it[questStartTime] = DateTime.now().withTimeAtStartOfDay()
//                it[questList] = list.joinToString(",")
//            }
        }
    }

}