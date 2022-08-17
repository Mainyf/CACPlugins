package io.github.mainyf.customeconomy.storage

import io.github.mainyf.newmclib.storage.BaseTable

object EconomysLists : BaseTable("t_EconomysList") {

    val coinName = varchar("coin_name", 255)

}

class PlayerEconomys(coinName: String) : BaseTable("t_PlayerEconomy_${coinName}") {

    val value = double("value")

}