package io.github.mainyf.socialsystem.storage

import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object PlayerSocials : BaseTable("t_PlayerSocials") {

    val intimacy = long("intimacy").default(0L)

    val allowRepair = bool("allow_repair").default(false)

}


class PlayerSocial(uuid: EntityID<UUID>) : BaseEntity(PlayerSocials, uuid) {

    companion object : UUIDEntityClass<PlayerSocial>(PlayerSocials)

    var intimacy by PlayerSocials.intimacy

    var allowRepair by PlayerSocials.allowRepair

    val friends by PlayerFriend referrersOn PlayerFriends.friend

    val sendRequests by PlayerFriendRequest referrersOn PlayerFriendRequests.sender

    val receiveRequests by PlayerFriendRequest referrersOn PlayerFriendRequests.receiver

}