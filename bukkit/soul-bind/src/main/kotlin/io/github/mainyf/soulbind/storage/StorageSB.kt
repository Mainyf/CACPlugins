package io.github.mainyf.soulbind.storage

import io.github.mainyf.newmclib.storage.AbstractStorageManager
import org.jetbrains.exposed.sql.SchemaUtils

object StorageSB : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                SoulBindItemDatas
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun createRecallCount(itemId: Long) {
        transaction {
            val data = SoulBindItemData.findById(itemId)
            if (data == null) {
                SoulBindItemData.new(itemId) {
                    this.recallCount = 0
                }
            }
        }
    }

    fun addRecallCount(itemId: Long, count: Int = 1) {
        transaction {
            val data = SoulBindItemData.findById(itemId)
            if (data != null) {
                data.recallCount += count
            }
        }
    }

    fun getRecallCount(itemId: Long): Int {
        return transaction {
            val data = SoulBindItemData.findById(itemId)
            data?.recallCount ?: -1
        }
    }

}
