package io.github.mainyf.socialsystem.storage

import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object PlayerFriendRequests : BaseTable("t_PlayerFriendRequests", true) {

    val sender = reference("sender", PlayerSocials)

    val receiver = reference("receiver", PlayerSocials)

}

class PlayerFriendRequest(uuid: EntityID<UUID>) : BaseEntity(PlayerFriendRequests, uuid) {

    companion object : UUIDEntityClass<PlayerFriendRequest>(PlayerFriendRequests)

    var sender by PlayerFriendRequests.sender

    var receiver by PlayerFriendRequests.receiver

}