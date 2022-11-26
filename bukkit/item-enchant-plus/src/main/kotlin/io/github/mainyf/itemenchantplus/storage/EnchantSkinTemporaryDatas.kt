package io.github.mainyf.itemenchantplus.storage

import io.github.mainyf.newmclib.env
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.jodatime.datetime

object EnchantSkinTemporaryDatas : IdTable<Long>("t_EnchantSkinTemporaryData_${env()}") {

    override val id = long("id").entityId()

    override val primaryKey = PrimaryKey(id)

    val playerUID = uuid("player_uid")

    val skinName = varchar("skin_name", 255)

    val expiredTime = datetime("expired_time")

    val stage = integer("stage")

}

class EnchantSkinTemporaryData(id: EntityID<Long>) : LongEntity(id) {

    companion object : LongEntityClass<EnchantSkinTemporaryData>(EnchantSkinTemporaryDatas)

    var playerUID by EnchantSkinTemporaryDatas.playerUID

    var skinName by EnchantSkinTemporaryDatas.skinName

    var expiredTime by EnchantSkinTemporaryDatas.expiredTime

    var stage by EnchantSkinTemporaryDatas.stage

    fun isExpired(): Boolean {
        return expiredTime.isAfterNow
    }

}