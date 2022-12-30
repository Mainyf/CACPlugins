package io.github.mainyf.soulbind.storage

import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.soulbind.toBase64
import io.github.mainyf.soulbind.toItemStack
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import java.util.UUID

object StorageSB : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                SoulBindItemIDDatas,
                SoulBindItemDatas
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
            if (SoulBindItemIDData.count() == 0L) {
                SoulBindItemIDData.new(1) {
                    this.nextID = 1
                }
            }
        }
    }

    fun nextItemLongID(): Long {
        return transaction {
            val data = SoulBindItemIDData.findById(1L)
            val rs: Long
            if (data == null) {
                SoulBindItemIDData.new(1) {
                    nextID = 1
                }
                rs = 1
            } else {
                rs = data.nextID
                data.nextID++
            }
            rs
        }
    }

    fun updateRecallCount(owner: UUID, itemId: Long, itemStack: ItemStack) {
        transaction {
            val data = SoulBindItemData.findById(itemId)
            if (data == null) {
                SoulBindItemData.new(itemId) {
                    this.ownerUUID = owner
                    this.recallCount = 0
                    this.itemRawData = itemStack.toBase64()
                }
            } else {
                data.itemRawData = itemStack.toBase64()
            }
        }
    }

    fun getPlayerRecallItems(owner: UUID): Map<Long, ItemStack> {
        return transaction {
            SoulBindItemData
                .find { (SoulBindItemDatas.ownerUUID eq owner) and (SoulBindItemDatas.hasAbandon eq false) }
                .associate {
                    it.id.value to it.itemRawData.toItemStack()
                }
        }
    }

    fun addRecallCount(itemId: Long, count: Int = 1): Int {
        return transaction {
            val data = SoulBindItemData.findById(itemId)
            if (data != null) {
                data.recallCount += count
                data.recallCount
            } else -1
        }
    }

    fun getRecallCount(itemId: Long): Int {
        return transaction {
            val data = SoulBindItemData.findById(itemId)
            data?.recallCount ?: -1
        }
    }

    fun hasAbandonItem(itemId: Long): Boolean {
        return transaction {
            val data = SoulBindItemData.findById(itemId)
            data?.hasAbandon ?: false
        }
    }

    fun markAbandonItem(itemId: Long) {
        transaction {
            val data = SoulBindItemData.findById(itemId) ?: return@transaction
            data.hasAbandon = true
        }
    }

}
