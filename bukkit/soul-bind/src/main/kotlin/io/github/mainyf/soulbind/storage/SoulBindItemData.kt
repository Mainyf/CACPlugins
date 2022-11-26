package io.github.mainyf.soulbind.storage

import io.github.mainyf.newmclib.env
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object SoulBindItemDatas : IdTable<Long>("t_SoulBindItemDatas_${env()}") {

    override val id = long("id").entityId()

    override val primaryKey = PrimaryKey(id)

    val recallCount = integer("recall_count")

}


class SoulBindItemData(id: EntityID<Long>) : LongEntity(id) {

    companion object : LongEntityClass<SoulBindItemData>(SoulBindItemDatas)

    var recallCount by SoulBindItemDatas.recallCount

}
