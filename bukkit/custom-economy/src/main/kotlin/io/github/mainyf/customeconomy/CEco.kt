package io.github.mainyf.customeconomy

import io.github.mainyf.customeconomy.storage.StorageManager
import java.util.*

object CEco {

    fun getEconomys(): Set<String> {
        return StorageManager.getEconomys()
    }

    fun giveMoney(uuid: UUID, coinName: String, value: Double) {
        StorageManager.giveMoney(uuid, coinName, value)
    }

    fun takeMoney(uuid: UUID, coinName: String, value: Double) {
        StorageManager.takeMoney(uuid, coinName, value)
    }

    fun setMoney(uuid: UUID, coinName: String, value: Double) {
        StorageManager.setMoney(uuid, coinName, value)
    }

    fun getMoney(uuid: UUID, coinName: String): Double {
        return StorageManager.getMoney(uuid, coinName)
    }

}