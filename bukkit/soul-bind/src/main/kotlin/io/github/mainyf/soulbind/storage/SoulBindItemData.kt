package io.github.mainyf.soulbind.storage

import io.github.mainyf.newmclib.env
import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import java.util.UUID

object SoulBindItemDatas : BaseTable("t_SoulBindItemDatas_${env()}") {

    val ownerUUID = uuid("owner_uuid")

    val itemRawData = text("item_rawdata")

    val recallCount = integer("recall_count")

    var hasAbandon = bool("has_abandon").default(false)

}


class SoulBindItemData(id: EntityID<UUID>) : BaseEntity(SoulBindItemDatas, id) {

    companion object : UUIDEntityClass<SoulBindItemData>(SoulBindItemDatas)

    var ownerUUID by SoulBindItemDatas.ownerUUID

    var itemRawData by SoulBindItemDatas.itemRawData

    var recallCount by SoulBindItemDatas.recallCount

    var hasAbandon by SoulBindItemDatas.hasAbandon

}
