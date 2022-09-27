package io.github.mainyf.questextension

import com.guillaumevdn.gcore.lib.economy.Currency
import io.github.mainyf.customeconomy.CEco
import org.bukkit.OfflinePlayer

class CurrencyToken1 : Currency("TOKEN_1", "CustomEconomy") {

    override fun initialize(): Boolean {
        return true
    }

    override fun doGet(p0: OfflinePlayer): Double {
        return CEco.getMoney(p0.uniqueId, "token_1")
    }

    override fun doGive(p0: OfflinePlayer, p1: Double): Boolean {
        CEco.giveMoney(p0.uniqueId, "token_1", p1)
        return true
    }

    override fun doTake(p0: OfflinePlayer, p1: Double): Boolean {
        CEco.takeMoney(p0.uniqueId, "token_1", p1)
        return true
    }

}