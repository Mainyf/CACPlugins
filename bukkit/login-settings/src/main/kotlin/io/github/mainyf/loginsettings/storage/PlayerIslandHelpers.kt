package io.github.mainyf.loginsettings.storage

import io.github.mainyf.newmclib.env
import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.jodatime.datetime
import java.util.*

object PlayerPlayRuleAgreeLogs : BaseTable("t_PlayerPlayRuleAgreeLog_${env()}") {

    val expiredTime = datetime("expired_time")

}

class PlayerPlayRuleAgreeLog(uuid: EntityID<UUID>) : BaseEntity(PlayerPlayRuleAgreeLogs, uuid) {

    companion object : UUIDEntityClass<PlayerPlayRuleAgreeLog>(PlayerPlayRuleAgreeLogs)

    var expiredTime by PlayerPlayRuleAgreeLogs.expiredTime

}