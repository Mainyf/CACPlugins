package io.github.mainyf.itemenchantplus.storage

import io.github.mainyf.newmclib.env
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable

object EnchantSkinDatas : LongIdTable("t_EnchantSkinData_${env()}") {

    val playerUID = uuid("player_uid")

    val skinName = varchar("skin_name", 255)

    val stage = integer("stage")

}

class EnchantSkinData(id: EntityID<Long>) : LongEntity(id) {

    companion object : LongEntityClass<EnchantSkinData>(EnchantSkinDatas)

    var playerUID by EnchantSkinDatas.playerUID

    var skinName by EnchantSkinDatas.skinName

    var stage by EnchantSkinDatas.stage

}