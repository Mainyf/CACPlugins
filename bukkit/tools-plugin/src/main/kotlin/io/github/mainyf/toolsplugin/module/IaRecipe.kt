package io.github.mainyf.toolsplugin.module

import io.github.mainyf.customeconomy.CEco
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.toolsplugin.config.ConfigTP
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.inventory.ShapedRecipe

object IaRecipe : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCraft(event: CraftItemEvent) {
        val player = event.viewers.firstOrNull() as? Player ?: return
        val key = (event.recipe as? ShapedRecipe)?.key?.toString() ?: return
        if (player.isOp && ConfigTP.iaRecipeDebug) {
            player.sendMessage(
                "&6当前合成配方的ID为: ".deserialize()
                    .append(
                        "&b${key}".deserialize()
                            .hoverEvent(HoverEvent.showText("&6点击复制".deserialize()))
                            .clickEvent(ClickEvent.copyToClipboard(key))
                    )
            )
        }
        val amount = event.craftItemAmount()
        val itemName = event.currentItem?.itemMeta?.displayName()?.text() ?: "空"
        if (!ConfigTP.iaRecipeCost.containsKey(key)) {
            return
        }
        val cost = ConfigTP.iaRecipeCost[key]!! * amount
        val money = CEco.getMoney(player.uuid, ConfigTP.iaRecipeCostCoinName)
        if (money < cost) {
            ConfigTP.iaRecipeCostLack?.execute(
                player,
                "{itemName}", itemName,
                "{cost}", cost.toDisplayText(),
                "{money}", money.toDisplayText()
            )
            event.isCancelled = true
            return
        }
        CEco.takeMoney(player.uuid, ConfigTP.iaRecipeCostCoinName, cost)
        ConfigTP.iaRecipeSuccess?.execute(
            player,
            "{itemName}", itemName,
            "{cost}", cost.toDisplayText(),
            "{money}", (money - cost).toDisplayText()
        )
    }

    fun CraftItemEvent.craftItemAmount(): Int {
        var amount = 1
        if (isShiftClick) {
            var max = inventory.maxStackSize
            val matrix = inventory.matrix
            matrix.forEach {
                if (it.isEmpty()) return@forEach
                val tmp = it!!.amount
                if (tmp in 1 until max) {
                    max = tmp
                }
            }
            amount *= max
        }
        return amount
    }


}