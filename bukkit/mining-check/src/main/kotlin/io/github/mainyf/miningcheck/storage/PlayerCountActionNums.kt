package io.github.mainyf.miningcheck.storage

import io.github.mainyf.newmclib.env
import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.bukkit.Location
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.jodatime.date
import java.util.*

object PlayerCountActionNums : BaseTable("t_PlayerCountActionNums_${env()}") {

    val playerUUID = uuid("player_uuid")

    val worldName = varchar("world_name", 255)

    val startTime = date("start_time")

    val countActionNum = integer("count_action_num")

}

class PlayerCountActionNum(uuid: EntityID<UUID>) : BaseEntity(PlayerCountActionNums, uuid) {

    companion object : UUIDEntityClass<PlayerCountActionNum>(PlayerCountActionNums)

    var playerUUID by PlayerCountActionNums.playerUUID

    var worldName by PlayerCountActionNums.worldName

    var startTime by PlayerCountActionNums.startTime

    var countActionNum by PlayerCountActionNums.countActionNum

}
