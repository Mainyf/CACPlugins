package io.github.mainyf.cdkey.storage

import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object ConsumeCDKeys : BaseTable("t_ConsumeCDKeys") {

    val codeName = varchar("code_name", 255)

    val cdkey = text("cdkey")

    val valid = bool("valid")

}

class ConsumeCDKey(uuid: EntityID<UUID>) : BaseEntity(ConsumeCDKeys, uuid) {

    companion object : UUIDEntityClass<ConsumeCDKey>(ConsumeCDKeys)

    var codeName by ConsumeCDKeys.codeName

    var cdkey by ConsumeCDKeys.cdkey

    var valid by ConsumeCDKeys.valid

}