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

object SoulBindItemDatas : IdTable<Long>("t_SoulBindItemDatas_${env()}") {

    override val id = long("id").entityId()

    override val primaryKey = PrimaryKey(id)

    val ownerUUID = uuid("owner_uuid")

    val itemRawData = text("item_rawdata")

    val recallCount = integer("recall_count")

    var hasAbandon = bool("has_abandon").default(false)

}


class SoulBindItemData(id: EntityID<Long>) : LongEntity(id) {

    companion object : LongEntityClass<SoulBindItemData>(SoulBindItemDatas)

    var ownerUUID by SoulBindItemDatas.ownerUUID

    var itemRawData by SoulBindItemDatas.itemRawData

    var recallCount by SoulBindItemDatas.recallCount

    var hasAbandon by SoulBindItemDatas.hasAbandon

}
