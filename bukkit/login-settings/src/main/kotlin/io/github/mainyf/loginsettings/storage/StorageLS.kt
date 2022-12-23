package io.github.mainyf.loginsettings.storage

import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.insertByID
import io.github.mainyf.newmclib.storage.newByID
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.joda.time.DateTime
import java.util.*

internal object StorageLS : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                PlayerPlayRuleAgreeLogs,
                PlayerLinkQQs
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun hasLinkQQ(uuid: UUID): Boolean {
        return transaction {
            !PlayerLinkQQs.select { PlayerLinkQQs.id eq uuid }.empty()
        }
    }

    fun hasLinkAccount(qqNum: Long): Boolean {
        return transaction {
            !PlayerLinkQQs.select { PlayerLinkQQs.qqNum eq qqNum }.empty()
        }
    }

    fun getLinkQQNum(uuid: UUID): Long? {
        return transaction {
            PlayerLinkQQs
                .select { PlayerLinkQQs.id eq uuid }
                .firstOrNull()
                ?.let {
                    it[PlayerLinkQQs.qqNum]
                }
        }
    }

    fun removeLinkQQ(uuid: UUID) {
        transaction {
            PlayerLinkQQs.deleteWhere { PlayerLinkQQs.id eq uuid }
        }
    }

    fun addLinkQQ(uuid: UUID, qqNum: Long) {
        transaction {
            PlayerLinkQQs.insertByID(uuid) {
                it[PlayerLinkQQs.qqNum] = qqNum
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