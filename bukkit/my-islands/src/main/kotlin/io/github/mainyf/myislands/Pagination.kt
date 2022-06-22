package io.github.mainyf.myislands

import io.github.mainyf.newmclib.exts.pagination
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.SizedIterable
import kotlin.math.ceil

//class Pagination<T>(
//    var pageSize: Int, val dataSource: PaginationDataSource<T>
//) {
//
//    companion object {
//
//        fun <T> fromDataSource(pageSize: Int, dataSource: PaginationDataSource<T>): Pagination<T> {
//            return Pagination(pageSize, dataSource)
//        }
//
//        fun <T> fromList(pageSize: Int, list: List<T>): Pagination<T> {
//            return Pagination(pageSize, ListPaginationDataSource(list))
//        }
//
//        fun <ID : Comparable<ID>, E : Entity<ID>> fromStorage(
//            pageSize: Int,
//            storageManager: AbstractStorageManager,
//            daoEntity: EntityClass<ID, E>,
//            block: (EntityClass<ID, E>) -> SizedIterable<E>
//        ): Pagination<E> {
//            return Pagination(pageSize, StoragePaginationDataSource(storageManager, daoEntity, block))
//        }
//
//    }
//
//    var pageIndex = 1
//
//    val maxPageIndex: Int
//        get() {
//            val maxSize = dataSource.maxSize()
//            return ceil(maxSize.toDouble() / pageSize.toDouble()).toInt()
//        }
//
//    val currentList = mutableListOf<T>()
//
//    fun hasStart() = pageIndex <= 1
//
//    fun hasEnd() = pageIndex >= maxPageIndex
//
//    fun prev(): Boolean {
//        if (pageIndex > 1) {
//            pageIndex--
//            updateCurrentList()
//            return true
//        }
//        return false
//    }
//
//    fun next(): Boolean {
//        if (pageIndex < maxPageIndex) {
//            pageIndex++
//            updateCurrentList()
//            return true
//        }
//        return false
//    }
//
//    fun updateCurrentList() {
//        synchronized(this) {
//            currentList.clear()
//            currentList.addAll(dataSource.get(this))
//        }
//    }
//
//}
//
//interface PaginationDataSource<T> {
//
//    fun maxSize(): Long
//
//    fun get(pagination: Pagination<T>): List<T>
//
//}
//
//class ListPaginationDataSource<T>(val list: List<T>) : PaginationDataSource<T> {
//
//    override fun maxSize(): Long {
//        return list.size.toLong()
//    }
//
//    override fun get(pagination: Pagination<T>): List<T> {
//        return list.pagination(pagination.pageIndex, pagination.pageSize)
//    }
//
//}
//
//open class StoragePaginationDataSource<ID : Comparable<ID>, E : Entity<ID>>(
//    val storageManager: AbstractStorageManager,
//    val daoEntity: EntityClass<ID, E>,
//    val block: (EntityClass<ID, E>) -> SizedIterable<E>
//) : PaginationDataSource<E> {
//
//    override fun maxSize(): Long {
//        return storageManager.transaction {
//            daoEntity.count()
//        }
//    }
//
//    override fun get(pagination: Pagination<E>): List<E> {
//        return storageManager.transaction {
//            block(daoEntity).limit(
//                pagination.pageSize,
//                (pagination.pageIndex.toLong() - 1L) * pagination.pageSize.toLong()
//            ).toList()
//        }
//    }
//
//}