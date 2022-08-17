package io.github.mainyf.customeconomy.storage

import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.insertByID
import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime
import java.util.UUID
import kotlin.math.max

object StorageManager : AbstractStorageManager() {

    private val economys = mutableMapOf<String, PlayerEconomys>()

    override fun init() {
        super.init()
        transaction {
            SchemaUtils.createMissingTablesAndColumns(EconomysLists)
            economys.clear()
            EconomysLists.selectAll().forEach {
                val coinName = it[EconomysLists.coinName]
                economys[coinName] = PlayerEconomys(coinName)
            }
            getEconomy("default")
        }
    }

//    fun createCoin(coinName: String) {
//        if (economys.containsKey(coinName)) return
//        EconomysLists.insertByID {
//            it[EconomysLists.createTime] = DateTime.now()
//            it[EconomysLists.coinName] = coinName
//        }
//        val table = PlayerEconomys(coinName)
//        economys[coinName] = table
//        SchemaUtils.createMissingTablesAndColumns(table)
//    }

    private fun getEconomy(coinName: String): PlayerEconomys {
        if (economys.containsKey(coinName)) {
            return economys[coinName]!!
        }
//        val rs = EconomysLists.select { EconomysLists.coinName eq coinName }.firstOrNull()
//        if(rs != null) {
//
//        }
        EconomysLists.insertByID {
            it[EconomysLists.createTime] = DateTime.now()
            it[EconomysLists.coinName] = coinName
        }
        val table = PlayerEconomys(coinName)
        economys[coinName] = table
        SchemaUtils.createMissingTablesAndColumns(table)
        return table
    }

    fun getEconomys(): Set<String> {
        return economys.keys
    }

    fun giveMoney(uuid: UUID, coinName: String, value: Double) {
        transaction {
            val economy = getEconomy(coinName)
            val peRs = economy.select { economy.id eq uuid }.firstOrNull()
            if (peRs != null) {
                val money = peRs[economy.value]
                economy.update(where = { economy.id eq uuid }, body = {
                    it[economy.value] = max(0.0, money + value)
                })
            } else {
                economy.insertByID(uuid) {
                    it[economy.createTime] = DateTime.now()
                    it[economy.value] = max(0.0, value)
                }
            }
        }
    }

    fun takeMoney(uuid: UUID, coinName: String, value: Double) {
        transaction {
            val economy = getEconomy(coinName)
            val peRs = economy.select { economy.id eq uuid }.firstOrNull()
            if (peRs != null) {
                val money = peRs[economy.value]
                economy.update(where = { economy.id eq uuid }, body = {
                    it[economy.value] = max(0.0, money - value)
                })
            } else {
                economy.insertByID(uuid) {
                    it[economy.createTime] = DateTime.now()
                    it[economy.value] = 0.0
                }
            }
        }
    }

    fun setMoney(uuid: UUID, coinName: String, value: Double) {
        transaction {
            val economy = getEconomy(coinName)
            val peRs = economy.select { economy.id eq uuid }.firstOrNull()
            if (peRs != null) {
                economy.update(where = { economy.id eq uuid }, body = {
                    it[economy.value] = max(0.0, value)
                })
            } else {
                economy.insertByID(uuid) {
                    it[economy.createTime] = DateTime.now()
                    it[economy.value] = max(0.0, value)
                }
            }
        }
    }

    fun getMoney(uuid: UUID, coinName: String): Double {
        return transaction {
            val economy = getEconomy(coinName)
            val peRs = economy.select { economy.id eq uuid }.firstOrNull()
            if (peRs != null) peRs[economy.value] else 0.0
        }
    }

}