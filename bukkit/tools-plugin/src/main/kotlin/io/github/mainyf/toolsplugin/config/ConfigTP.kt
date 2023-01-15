@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.toolsplugin.config

import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.config.asItemTypeWrapper
import io.github.mainyf.newmclib.exts.getAction
import io.github.mainyf.newmclib.exts.getSection
import io.github.mainyf.newmclib.utils.ItemTypeWrapper
import io.github.mainyf.toolsplugin.ToolsPlugin

object ConfigTP {

    var recycleEnderDragonEgg = false
    var saturdayFly = false
    var iaRecipeDebug = true
    var iaRecipeCostCoinName: String = "token_1"
    val iaRecipeCost = mutableMapOf<String, Double>()
    var iaRecipeCostLack: MultiAction? = null
    var iaRecipeSuccess: MultiAction? = null

    var chunkLoggerDebug = false

    var checkPlayerInventoryLog = "玩家: {player} 持有物品({status}) {itemText}，超出限制"
    var checkPlayerInventoryInfo = "&c玩家: {player} 持有物品({status}) {itemText}，超出限制"
    val checkPlayerInventoryItems = mutableListOf<CheckPlayerInvItem>()

    var iaItemAutoUpdateInfo: MultiAction? = null
    val iaItemAutoUpdateItems = mutableSetOf<String>()

    var saturdayNightVisionEnable = true
    var saturdayNightVisionMvpPerm = "nightvision.use"
    var saturdayNightVisionToggleAction: MultiAction? = null

    fun load() {
        ToolsPlugin.INSTANCE.saveDefaultConfig()
        ToolsPlugin.INSTANCE.reloadConfig()
        val config = ToolsPlugin.INSTANCE.config
        recycleEnderDragonEgg = config.getBoolean("recycleEnderDragonEgg", recycleEnderDragonEgg)
        saturdayFly = config.getBoolean("saturdayFly", saturdayFly)

        iaRecipeDebug = config.getBoolean("iaRecipe.debug", iaRecipeDebug)
        iaRecipeCost.clear()
        iaRecipeCostCoinName = config.getString("iaRecipe.coinName", iaRecipeCostCoinName)!!
        val iaRecipeCostSect = config.getSection("iaRecipe.recipeCost")
        iaRecipeCostSect.getKeys(false).forEach { key ->
            iaRecipeCost[key] = iaRecipeCostSect.getDouble(key)
        }
        iaRecipeCostLack = config.getAction("iaRecipe.iaRecipeCostLack")
        iaRecipeSuccess = config.getAction("iaRecipe.iaRecipeSuccess")

        chunkLoggerDebug = config.getBoolean("chunkLogger.debug", chunkLoggerDebug)

        checkPlayerInventoryLog = config.getString("checkPlayerInventory.log", checkPlayerInventoryLog)!!
        checkPlayerInventoryInfo = config.getString("checkPlayerInventory.info", checkPlayerInventoryInfo)!!
        checkPlayerInventoryItems.clear()
        checkPlayerInventoryItems.addAll(config.getStringList("checkPlayerInventory.item").map {
            val pair = it.split("|")
            CheckPlayerInvItem(
                pair[0].asItemTypeWrapper(),
                pair[1].toInt()
            )
        })
        iaItemAutoUpdateInfo = config.getAction("iaItemAutoUpdate.info")
        iaItemAutoUpdateItems.clear()
        iaItemAutoUpdateItems.addAll(config.getStringList("iaItemAutoUpdate.item"))
        saturdayNightVisionEnable = config.getBoolean("saturdayNightVision.enable", saturdayNightVisionEnable)
        saturdayNightVisionMvpPerm = config.getString("saturdayNightVision.mvp", saturdayNightVisionMvpPerm)!!
        saturdayNightVisionToggleAction = config.getAction("saturdayNightVision.toggleAction")
    }

    class CheckPlayerInvItem(
        val item: ItemTypeWrapper,
        val amount: Int
    )

}