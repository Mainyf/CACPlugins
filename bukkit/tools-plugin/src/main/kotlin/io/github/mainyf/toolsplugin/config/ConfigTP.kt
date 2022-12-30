@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.toolsplugin.config

import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.exts.getAction
import io.github.mainyf.newmclib.exts.getSection
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
    }

}