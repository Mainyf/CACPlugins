package io.github.mainyf.playeraccount.storage

import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object PlayerAccountDatas : BaseTable("t_PlayerAccountDatas") {

    val phoneNumber = varchar("phone_number", 255)

}

class PlayerAccountData(uuid: EntityID<UUID>) : BaseEntity(PlayerAccountDatas, uuid) {

    companion object : UUIDEntityClass<PlayerAccountData>(PlayerAccountDatas)

    var phoneNumber by PlayerAccountDatas.phoneNumber

}