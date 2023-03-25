package io.github.mainyf.shopmanager

import dev.jorel.commandapi.arguments.ItemStackArgument
import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.offlinePlayerArguments
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.shopmanager.config.ConfigSM
import io.github.mainyf.shopmanager.listener.PlayerListeners
import io.github.mainyf.shopmanager.menu.SellMenu
import io.github.mainyf.shopmanager.storage.StorageManager
import net.kyori.adventure.text.Component
import org.apache.logging.log4j.LogManager
import org.bukkit.Material
import org.bukkit.entity.Player
import kotlin.math.floor

class ShopManager : BasePlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("ShopManager")

        lateinit var INSTANCE: ShopManager

    }

    override fun enable() {
        INSTANCE = this
        ConfigSM.load()
        StorageManager.init()
        pluginManager().registerEvents(PlayerListeners, this)
        apiCommand("shopManager") {
            withAliases("shopM", "shm", "sm")
            "reload" {
                executeOP {
                    ConfigSM.load()
                    sender.successMsg("[ShopManager] 重载成功")
                }
            }
            "view" {
                withArguments(
                    ItemStackArgument("物品类型"),
                    offlinePlayerArguments("玩家名")
                )
                executeOP {
                    val itemStack = itemStack()
                    val offlinePlayer = offlinePlayer()
                    val harvest = StorageManager.getCurrentHarvest(offlinePlayer.uuid, itemStack.type)
                    sender.sendMessage(
                        Component.text("物品: ")
                            .append(Component.translatable(itemStack))
                            .append(Component.text("已获得: "))
                            .append(Component.text(harvest))
                    )
                }
            }
            "sell-player" {
                withArguments(
                    playerArguments("玩家名")
                )
                executeOP {
                    SellMenu().open(player())
                }
            }
            "sell" {
                executePlayer {
                    SellMenu().open(sender)
                }
            }
        }.register()
    }

    fun getSellItemCount(
        player: Player,
        material: Material,
        sellShop: ConfigSM.SellShopLimit
    ): Int {
        val currentHarvest = StorageManager.getCurrentHarvest(player.uuid, material)
        val maxHarvest = ConfigSM.getMaxHarvest(player, sellShop)
        if (currentHarvest >= maxHarvest) {
            return 0
        }
        return floor((maxHarvest - currentHarvest) / sellShop.price).toInt()
    }

}