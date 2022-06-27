package io.github.mainyf.myislands.storage

import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.jodatime.date
import java.util.*

object PlayerKudoLogs : BaseTable("t_PlayerKudos", true) {

    val island = reference("island", PlayerIslands)

    val kudoUUID = uuid("kudo_uuid")

    val kudoDate = date("kudo_date")

}

class PlayerKudoLog(uuid: EntityID<UUID>) : BaseEntity(PlayerKudoLogs, uuid) {

    companion object : UUIDEntityClass<PlayerKudoLog>(PlayerKudoLogs)

    var island by PlayerKudoLogs.island

    var kudoUUID by PlayerKudoLogs.kudoUUID

    var kudoDate by PlayerKudoLogs.kudoDate

}