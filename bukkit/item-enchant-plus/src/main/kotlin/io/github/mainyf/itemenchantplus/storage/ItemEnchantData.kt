package io.github.mainyf.itemenchantplus.storage

import io.github.mainyf.newmclib.env
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object ItemEnchantDatas : IdTable<Long>("t_ItemEnchantData_${env()}") {

    override val id = long("id").entityId()

    override val primaryKey = PrimaryKey(id)

    val stage = integer("stage")

    val level = integer("level")

    val exp = double("exp")

    val skinName = varchar("skin", 255)

}

class ItemEnchantData(id: EntityID<Long>) : LongEntity(id) {

    companion object : LongEntityClass<ItemEnchantData>(ItemEnchantDatas)

    var stage by ItemEnchantDatas.stage

    var level by ItemEnchantDatas.level

    var exp by ItemEnchantDatas.exp

    var skinName by ItemEnchantDatas.skinName

}