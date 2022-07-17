package io.github.mainyf.socialsystem.storage

import io.github.mainyf.newmclib.env
import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import io.github.mainyf.socialsystem.storage.PlayerSocials.default
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object PlayerFriends : BaseTable("t_PlayerFriends_${env()}") {

    val intimacy = long("intimacy").default(0L)

    val social = reference("social", PlayerSocials)

    val friend = uuid("friend")

}

class PlayerFriend(uuid: EntityID<UUID>) : BaseEntity(PlayerFriends, uuid) {

    companion object : UUIDEntityClass<PlayerFriend>(PlayerFriends)

    var intimacy by PlayerFriends.intimacy

    var social by PlayerFriends.social

    var friend by PlayerFriends.friend

}