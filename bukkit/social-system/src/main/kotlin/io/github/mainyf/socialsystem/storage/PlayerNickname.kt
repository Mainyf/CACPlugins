package io.github.mainyf.socialsystem.storage

import io.github.mainyf.newmclib.env
import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.jodatime.datetime
import java.util.*

object PlayerNicknames : BaseTable("t_PlayerNicknames_${env()}") {

    val social = reference("social", PlayerSocials)

    val nickname = varchar("nickname", 255)

    val prevModify = datetime("prev_modify").nullable().default(null)

    val visible = bool("visible").default(true)

}

class PlayerNickname(uuid: EntityID<UUID>) : BaseEntity(PlayerNicknames, uuid) {

    companion object : UUIDEntityClass<PlayerNickname>(PlayerNicknames)

    var social by PlayerNicknames.social

    var nickname by PlayerNicknames.nickname

    var prevModify by PlayerNicknames.prevModify

    var visible by PlayerNicknames.visible

}