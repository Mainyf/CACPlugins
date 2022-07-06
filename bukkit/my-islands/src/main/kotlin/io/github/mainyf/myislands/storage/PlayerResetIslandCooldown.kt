package io.github.mainyf.myislands.storage

import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.jodatime.datetime
import java.util.*

object PlayerResetIslandCooldowns : BaseTable("t_PlayerResetIslandCooldowns", true) {

    val island = reference("island", PlayerIslands)

    val prevTime = datetime("prev_time")

}


class PlayerResetIslandCooldown(uuid: EntityID<UUID>) : BaseEntity(PlayerResetIslandCooldowns, uuid) {

    companion object : UUIDEntityClass<PlayerResetIslandCooldown>(PlayerResetIslandCooldowns)

    var island by PlayerResetIslandCooldowns.island

    var prevTime by PlayerResetIslandCooldowns.prevTime

}