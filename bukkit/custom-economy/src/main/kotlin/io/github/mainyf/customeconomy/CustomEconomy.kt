package io.github.mainyf.customeconomy

import dev.jorel.commandapi.CommandPermission
import dev.jorel.commandapi.arguments.DoubleArgument
import io.github.mainyf.customeconomy.storage.StorageManager
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.offlinePlayerArguments
import io.github.mainyf.newmclib.command.stringArguments
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.hooks.addPlaceholderExpansion
import org.apache.logging.log4j.LogManager
import org.bukkit.plugin.java.JavaPlugin

class CustomEconomy : JavaPlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("CustomEconomy")

        lateinit var INSTANCE: CustomEconomy

    }

    override fun onEnable() {
        INSTANCE = this
        StorageManager.init()
        apiCommand("customEconomy") {
            withAliases("customEco", "ceco")
            withPermission(CommandPermission.OP)
            "give" {
                withArguments(
                    offlinePlayerArguments("玩家名"),
                    stringArguments("货币类型") { _ ->
                        StorageManager.getEconomys().toTypedArray()
                    },
                    DoubleArgument("金钱")
                )
                executeOP {
                    val player = offlinePlayer()
                    val coinName = text()
                    val money = double()
                    StorageManager.giveMoney(player.uuid, coinName, money)
                }
            }
            "take" {
                withArguments(
                    offlinePlayerArguments("玩家名"),
                    stringArguments("货币类型") { _ ->
                        StorageManager.getEconomys().toTypedArray()
                    },
                    DoubleArgument("金钱")
                )
                executeOP {
                    val player = offlinePlayer()
                    val coinName = text()
                    val money = double()
                    StorageManager.takeMoney(player.uuid, coinName, money)
                }
            }
            "set" {
                withArguments(
                    offlinePlayerArguments("玩家名"),
                    stringArguments("货币类型") { _ ->
                        StorageManager.getEconomys().toTypedArray()
                    },
                    DoubleArgument("金钱")
                )
                executeOP {
                    val player = offlinePlayer()
                    val coinName = text()
                    val money = double()
                    StorageManager.setMoney(player.uuid, coinName, money)
                }
            }
            "look" {
                withArguments(
                    offlinePlayerArguments("玩家名"),
                    stringArguments("货币类型") { _ ->
                        StorageManager.getEconomys().toTypedArray()
                    }
                )
                executeOP {
                    val player = offlinePlayer()
                    val coinName = text()
                    val money = StorageManager.getMoney(player.uuid, coinName)
                    sender.msg("&b玩家: ${player.name}，当前金钱: $money")
                }
            }
            "list" {
                withArguments(
                    offlinePlayerArguments("玩家名"),
                )
                executeOP {
                    val player = offlinePlayer()
                    StorageManager.getEconomys().forEach { coinName ->
                        val money = StorageManager.getMoney(player.uuid, coinName)
                        sender.msg("&b玩家: ${player.name}，类型: ${coinName}, 当前金钱: $money")
                    }
                }
            }
        }.register()
        addPlaceholderExpansion("customeconomy") papi@{ offlinePlayer, params ->
            val uuid = offlinePlayer?.uniqueId ?: return@papi null
//            val player = offlinePlayer?.player ?: return@papi null
            StorageManager.getMoney(uuid, params!!).toInt().toString()
        }
        addPlaceholderExpansion("playermoney") papi@{ offlinePlayer, params ->
            val uuid = offlinePlayer?.uniqueId ?: return@papi null
            //            val player = offlinePlayer?.player ?: return@papi null

            StorageManager.getMoney(uuid, params!!).toInt().toString()
        }
    }

}