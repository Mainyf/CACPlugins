package io.github.mainyf.shopmanager.storage

import io.github.mainyf.newmclib.env
import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.bukkit.Material
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.jodatime.date
import java.util.*

object PlayerPurchases : BaseTable("t_PlayerPurchases_${env()}") {

    val pUUID = uuid("player_uuid")

    val date = date("date")

    val item = enumerationByName("item", 255, Material::class)

    val money = double("money")

}

class PlayerPurchase(uuid: EntityID<UUID>) : BaseEntity(PlayerPurchases, uuid) {

    companion object : UUIDEntityClass<PlayerPurchase>(PlayerPurchases)

    var pUUID by PlayerPurchases.pUUID

    var date by PlayerPurchases.date

    var item by PlayerPurchases.item

    var money by PlayerPurchases.money

}