package io.github.mainyf.myislands.storage

import io.github.mainyf.newmclib.serverId
import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object PlayerIslands : BaseTable("t_PlayerIslandData_${serverId()}") {

    val coreX = integer("coreX")

    val coreY = integer("coreY")

    val coreZ = integer("coreZ")

    val visibility = enumerationByName("visibility", 255, IslandVisibility::class)

    val kudos = integer("kudos")

}

class PlayerIsland(uuid: EntityID<UUID>) : BaseEntity(PlayerIslands, uuid) {

    companion object : UUIDEntityClass<PlayerIsland>(PlayerIslands)

    var coreX by PlayerIslands.coreX
    var coreY by PlayerIslands.coreY
    var coreZ by PlayerIslands.coreZ
    var visibility by PlayerIslands.visibility
    var kudos by PlayerIslands.kudos

    val helpers by PlayerIslandHelper referrersOn PlayerIslandHelpers.island

//    val kudosLog by PlayerKudoLog referrersOn PlayerKudoLogs.island

}

enum class IslandVisibility(val text: String, val count: Int) {
    ALL("所有人", 0),
    PERMISSION("好友&授权者", 1),
    NONE("任何人不可见", 2)

}