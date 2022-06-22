package io.github.mainyf.playersettings.storage

import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.date
import org.jetbrains.exposed.sql.jodatime.datetime
import java.util.*

object PlayerLivelys : BaseTable("t_PlayerLively") {

    val registerDate = datetime("register_date")

//    val onlineMinutes = long("online_minutes")

//    val a = reference("online_minutes", PlayerDayOnlineTable)

    val lastLoginDate = datetime("last_login_date")

}

class PlayerLively(uuid: EntityID<UUID>) : UUIDEntity(uuid) {

    companion object : UUIDEntityClass<PlayerLively>(PlayerLivelys)

    var createTime by PlayerLivelys.createTime

    var registerDate by PlayerLivelys.registerDate

//    var onlineMinutes by PlayerLivelyTable.onlineMinutes

//    var dayOnlines by PlayerDayOnline via PlayerLivelyToDayOnlines

    var lastLoginDate by PlayerLivelys.lastLoginDate

}

//object PlayerLivelyToDayOnlines : Table("t_PlayerLivelyToDayOnline") {
//
//    val lively = reference("lively", PlayerLivelys)
//
//    val dayOnline = reference("day_online", PlayerDayOnlines)
//
//    override val primaryKey = PrimaryKey(lively, dayOnline)
//
//}

object PlayerDayOnlines : BaseTable("t_PlayerDayOnline") {

    val pUUID = uuid("player_uuid")

    val date = date("date")

    val minutes = long("minutes")

}


class PlayerDayOnline(uuid: EntityID<UUID>) : UUIDEntity(uuid) {

    companion object : UUIDEntityClass<PlayerDayOnline>(PlayerDayOnlines)

    var createTime by PlayerDayOnlines.createTime

    var pUUID by PlayerDayOnlines.pUUID

    var date by PlayerDayOnlines.date

    var minutes by PlayerDayOnlines.minutes

}