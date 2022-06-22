package io.github.mainyf.mcrmbmigration.storage

import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object PlayerClaimOldPoints : BaseTable("t_PlayerClaimOldPoints") {

    val value = double("value")

}

class PlayerClaimOldPoint(uuid: EntityID<UUID>) : BaseEntity(PlayerClaimOldPoints, uuid) {

    companion object : UUIDEntityClass<PlayerClaimOldPoint>(PlayerClaimOldPoints)

    var value by PlayerClaimOldPoints.value

}