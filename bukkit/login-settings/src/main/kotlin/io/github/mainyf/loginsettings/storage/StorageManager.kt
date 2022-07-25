package io.github.mainyf.loginsettings.storage

import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.newByID
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SchemaUtils
import org.joda.time.DateTime

object StorageManager : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                PlayerPlayRuleAgreeLogs
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun hasAgreePlayRuleInWeek(player: Player): Boolean {
        return transaction {
            val data = PlayerPlayRuleAgreeLog.findById(player.uuid) ?: return@transaction false
            DateTime.now() < data.expiredTime
        }
    }

//    fun hasAgreePlayRuleInWeek(player: Player): Boolean {
//        return transaction {
//            val data = PlayerPlayRuleAgreeLog.findById(player.uuid) ?: return@transaction false
//            DateTime.now() >= data.expiredTime
//        }
//    }

    fun addPlayRuleAgreeLog(player: Player) {
        transaction {
            val data = PlayerPlayRuleAgreeLog.findById(player.uuid)
            val now = DateTime.now()
            val expiredTime = now.plusDays(7 - now.dayOfWeek + 1).withTimeAtStartOfDay()
            if (data == null) {
                PlayerPlayRuleAgreeLog.newByID(player.uuid) {
                    this.expiredTime = expiredTime
                }
            } else {
                data.expiredTime = expiredTime
            }
        }
    }

}