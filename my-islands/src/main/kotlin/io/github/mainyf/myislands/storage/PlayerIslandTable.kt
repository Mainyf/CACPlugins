package io.github.mainyf.myislands.storage

import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

object PlayerIslandTable : BaseTable("t_PlayerIslandData") {

    val serverId = varchar("server_id", 255)

    val coreX = integer("coreX")

    val coreY = integer("coreY")

    val coreZ = integer("coreZ")

    val visibility = bool("visibility")

    val kudos = integer("kudos")

}

class PlayerIslandData(uuid: EntityID<UUID>) : UUIDEntity(uuid) {

    companion object : UUIDEntityClass<PlayerIslandData>(PlayerIslandTable)

    var createTime by PlayerIslandTable.createTime

    var serverId by PlayerIslandTable.serverId
    var coreX by PlayerIslandTable.coreX
    var coreY by PlayerIslandTable.coreY
    var coreZ by PlayerIslandTable.coreZ
    var visibility by PlayerIslandTable.visibility
    var kudos by PlayerIslandTable.kudos

}
