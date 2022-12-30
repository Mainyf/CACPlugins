package io.github.mainyf.soulbind.storage

import io.github.mainyf.newmclib.env
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object SoulBindItemIDDatas : IdTable<Long>("t_SoulBindItemIDData_${env()}") {

    override val id = long("id").entityId()

    override val primaryKey = PrimaryKey(id)

    val nextID = long("next_id")

}

class SoulBindItemIDData(id: EntityID<Long>) : LongEntity(id) {

    companion object : LongEntityClass<SoulBindItemIDData>(SoulBindItemIDDatas)

    var nextID by SoulBindItemIDDatas.nextID

}