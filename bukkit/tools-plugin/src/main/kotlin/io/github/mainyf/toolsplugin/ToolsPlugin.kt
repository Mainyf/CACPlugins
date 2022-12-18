package io.github.mainyf.toolsplugin

import dev.lone.itemsadder.api.CustomStack
import dev.lone.itemsadder.api.ItemsAdder
import io.github.mainyf.customeconomy.CEco
import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.toolsplugin.config.ConfigTP
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.apache.logging.log4j.LogManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
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

    @EventHandler
    fun onCraft(event: CraftItemEvent) {
        val player = event.viewers.firstOrNull() as? Player ?: return
        val key = (event.recipe as? ShapedRecipe)?.key?.toString() ?: return
        if (player.isOp) {
            player.sendMessage("&6当前合成配方的ID为: ".deserialize()
                .append(
                    "&b${key}".deserialize()
                        .hoverEvent(HoverEvent.showText("&6点击复制".deserialize()))
                        .clickEvent(ClickEvent.copyToClipboard(key))
                ))
        }
        val itemName = event.currentItem?.itemMeta?.displayName()?.text() ?: "空"
        if (!ConfigTP.iaRecipeCost.containsKey(key)) {
            return
        }
        val cost = ConfigTP.iaRecipeCost[key]!!
        val money = CEco.getMoney(player.uuid, ConfigTP.iaRecipeCostCoinName)
        if (money < cost) {
            ConfigTP.iaRecipeCostLack?.execute(
                player,
                "{itemName}", itemName,
                "{cost}", cost,
                "{money}", money
            )
            event.isCancelled = true
            return
        }
        CEco.takeMoney(player.uuid, ConfigTP.iaRecipeCostCoinName, cost)
        ConfigTP.iaRecipeSuccess?.execute(
            player,
            "{itemName}", itemName,
            "{cost}", cost,
            "{money}", money - cost
        )
    }

}