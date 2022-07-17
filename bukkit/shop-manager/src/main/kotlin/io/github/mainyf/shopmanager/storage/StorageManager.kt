package io.github.mainyf.shopmanager.storage

import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.newByID
import org.bukkit.Material
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.joda.time.DateTime
import java.util.*

object StorageManager : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                PlayerPurchases
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun getCurrentHarvest(uuid: UUID, material: Material): Double {
        return transaction {
            getPlayerPurchase(uuid, material).money
        }
    }

    fun updateHarvest(uuid: UUID, money: Double, material: Material) {
        transaction {
            val data = getPlayerPurchase(uuid, material)
            data.money += money
        }
    }

    private fun getPlayerPurchase(uuid: UUID, material: Material): PlayerPurchase {
        return transaction {
            var data = PlayerPurchase.find { (PlayerPurchases.pUUID eq uuid) and (PlayerPurchases.item eq material) }.firstOrNull()
            if (data == null) {
                data = PlayerPurchase.newByID {
                    this.pUUID = uuid
                    this.item = material
                    this.date = DateTime.now()
                    this.money = 0.0
                }
            }
            val now = DateTime.now()
            if (data.date.withTimeAtStartOfDay() != now.withTimeAtStartOfDay()) {
                data.date = now
                data.money = 0.0
            }
            data
        }
    }

}