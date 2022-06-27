package io.github.mainyf.myislands.storage

import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object PlayerIslandHelpers : BaseTable("t_PlayerIslandHelpers", true) {

    val island = reference("island", PlayerIslands)

    val helperUUID = uuid("helper_uuid")

}

class PlayerIslandHelper(uuid: EntityID<UUID>) : BaseEntity(PlayerIslandHelpers, uuid) {

    companion object : UUIDEntityClass<PlayerIslandHelper>(PlayerIslandHelpers)

    var island by PlayerIslandHelpers.island

    var helperUUID by PlayerIslandHelpers.helperUUID

}