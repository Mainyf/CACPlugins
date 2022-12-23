package io.github.mainyf.toolsplugin.module

import io.github.mainyf.customeconomy.CEco
import io.github.mainyf.newmclib.exts.deserialize
import io.github.mainyf.newmclib.exts.text
import io.github.mainyf.newmclib.exts.toDisplayText
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.toolsplugin.config.ConfigTP
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.inventory.ShapedRecipe

object IaRecipe : Listener {

    @EventHandler
    fun onCraft(event: CraftItemEvent) {
        val player = event.viewers.firstOrNull() as? Player ?: return
        val key = (event.recipe as? ShapedRecipe)?.key?.toString() ?: return
        if (player.isOp && ConfigTP.iaRecipeDebug) {
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


}