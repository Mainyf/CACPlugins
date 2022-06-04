package io.github.mainyf.playersettings.storage

import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.sql.jodatime.date

object PlayerRegisterLogs : BaseTable("t_PlayerRegisterLogs") {

    val date = date("date")

    val count = integer("count")

}

object CommandExecuteLogs : BaseTable("t_CommandExecuteLogs") {

    val cmd = varchar("cmd", 255)

    val count = integer("count")

}

//class CommandExecuteLog(uuid: EntityID<UUID>) : UUIDEntity(uuid) {
//
//    companion object : UUIDEntityClass<CommandExecuteLog>(CommandExecuteLogs)
//
//}