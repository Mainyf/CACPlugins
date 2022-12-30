package io.github.mainyf.itemenchantplus.storage

import io.github.mainyf.newmclib.env
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object EnchantItemIDDatas : IdTable<Long>("t_EnchantItemIDData_${env()}") {

    override val id = long("id").entityId()

    override val primaryKey = PrimaryKey(id)

    val nextID = long("next_id")

}

class EnchantItemIDData(id: EntityID<Long>) : LongEntity(id) {

    companion object : LongEntityClass<EnchantItemIDData>(EnchantItemIDDatas)

    var nextID by EnchantItemIDDatas.nextID

}