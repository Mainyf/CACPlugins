package io.github.mainyf.cdkey.storage

import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object PlayerClaimedCDKeys : BaseTable("t_PlayerClaimedCDKeys") {

    val pUUID = uuid("player_uuid")

    val cdkey = text("cdkey")

}

class PlayerClaimedCDKey(uuid: EntityID<UUID>) : BaseEntity(PlayerClaimedCDKeys, uuid) {

    companion object : UUIDEntityClass<PlayerClaimedCDKey>(PlayerClaimedCDKeys)

    var pUUID by PlayerClaimedCDKeys.pUUID

    var cdkey by PlayerClaimedCDKeys.cdkey

}