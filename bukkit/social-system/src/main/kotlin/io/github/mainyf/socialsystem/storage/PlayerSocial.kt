package io.github.mainyf.socialsystem.storage

import io.github.mainyf.newmclib.env
import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object PlayerSocials : BaseTable("t_PlayerSocials_${env()}") {

    val allowRepair = bool("allow_repair").default(false)

}


class PlayerSocial(uuid: EntityID<UUID>) : BaseEntity(PlayerSocials, uuid) {

    companion object : UUIDEntityClass<PlayerSocial>(PlayerSocials)

    var allowRepair by PlayerSocials.allowRepair

    val friends by PlayerFriend referrersOn PlayerFriends.social

    val sendRequests by PlayerFriendRequest referrersOn PlayerFriendRequests.sender

    val receiveRequests by PlayerFriendRequest referrersOn PlayerFriendRequests.receiver

}