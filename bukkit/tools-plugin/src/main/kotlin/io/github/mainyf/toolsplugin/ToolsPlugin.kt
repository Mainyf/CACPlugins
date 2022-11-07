package io.github.mainyf.toolsplugin

import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.toolsplugin.config.ConfigTP
import org.apache.logging.log4j.LogManager
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryOpenEvent
import java.util.Calendar

class ToolsPlugin : BasePlugin(), Listener {

    companion object {

        val LOGGER = LogManager.getLogger("ToolsPlugin")

        lateinit var INSTANCE: ToolsPlugin

    }

    override fun enable() {
        INSTANCE = this
        ConfigTP.load()
        pluginManager().registerEvents(this, this)
        apiCommand("toolsPlugin") {
            withAliases("tools", "toolsp")
            "reload" {
                executeOP {
                    ConfigTP.load()
                    sender.successMsg("[ToolsPlugin] 重载成功")
                }
            }
        }.register()
        submitTask(period = 20L) {
            if (!ConfigTP.saturdayFly) return@submitTask
            val calendar = Calendar.getInstance()
            val week = calendar[Calendar.DAY_OF_WEEK]
            if (week != 7) {
                onlinePlayers().forEach { player ->
                    if (player.allowFlight && !player.hasPermission("toolplugin.fly")) {
                        player.allowFlight = false
                    }
                }
            }
        }
    }

    @EventHandler
    fun onInventoryOpenEvent(event: InventoryOpenEvent) {
        if (!ConfigTP.recycleEnderDragonEgg) return
        val holder = event.inventory.holder
        val player = event.player as? Player ?: return
        if (holder is Chest || holder is DoubleChest || holder is ShulkerBox) {
            var flag = false
            var amount = 0
            event.inventory.forEachIndexed { index, itemStack ->
                if (itemStack?.type == Material.DRAGON_EGG) {
                    flag = true
                    amount += itemStack.amount
                    event.inventory.setItem(index, AIR_ITEM)
                }
            }
            if (flag) {
                val loc = when (holder) {
                    is Chest -> holder.location
                    is DoubleChest -> holder.location
                    is ShulkerBox -> holder.location
                    else -> null
                }!!
                //                val loc = (holder as? Chest)?.location ?: (holder as? DoubleChest)?.location ?: (holder as ShulkerBox).location
                LOGGER.info("玩家: ${player.name}，龙蛋已删除(x${amount})，位置: ${loc.world?.name} ${loc.x} ${loc.y} ${loc.z}")
                player.updateInventory()
            }
        }
    }

}