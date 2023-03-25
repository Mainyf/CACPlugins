@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.toolsplugin.config

import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.config.asItemTypeWrapper
import io.github.mainyf.newmclib.exts.getAction
import io.github.mainyf.newmclib.exts.getEnum
import io.github.mainyf.newmclib.exts.getSection
import io.github.mainyf.newmclib.exts.saveResourceToFileAsConfiguration
import io.github.mainyf.newmclib.utils.ItemTypeWrapper
import io.github.mainyf.toolsplugin.ToolsPlugin
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Material
import org.bukkit.block.Biome
import org.bukkit.configuration.ConfigurationSection

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

    var saturdayNightVisionEnable = true
    var saturdayNightVisionMvpPerm = "nightvision.use"
    var saturdayNightVisionToggleAction: MultiAction? = null

    val placeholders = mutableMapOf<String, PlaceholderConfig>()

    val customBlockGenerators = mutableMapOf<String, CustomBlockGenerator>()

    var forwardMythicMobSummonDamage = false

    lateinit var mainConfig: ConfigurationSection
    lateinit var worldPopulatorConfig: ConfigurationSection

    fun load() {
        mainConfig = ToolsPlugin.INSTANCE.saveResourceToFileAsConfiguration("config.yml")
//        worldPopulatorConfig = ToolsPlugin.INSTANCE.saveResourceToFileAsConfiguration("world-populator.yml")
        loadMainConfig()
//        loadWorldPopulator()
    }

    private fun loadMainConfig() {
        recycleEnderDragonEgg = mainConfig.getBoolean("recycleEnderDragonEgg", recycleEnderDragonEgg)
        saturdayFly = mainConfig.getBoolean("saturdayFly", saturdayFly)

        iaRecipeDebug = mainConfig.getBoolean("iaRecipe.debug", iaRecipeDebug)
        iaRecipeCost.clear()
        iaRecipeCostCoinName = mainConfig.getString("iaRecipe.coinName", iaRecipeCostCoinName)!!
        val iaRecipeCostSect = mainConfig.getSection("iaRecipe.recipeCost")
        iaRecipeCostSect.getKeys(false).forEach { key ->
            iaRecipeCost[key] = iaRecipeCostSect.getDouble(key)
        }
        iaRecipeCostLack = mainConfig.getAction("iaRecipe.iaRecipeCostLack")
        iaRecipeSuccess = mainConfig.getAction("iaRecipe.iaRecipeSuccess")

        chunkLoggerDebug = mainConfig.getBoolean("chunkLogger.debug", chunkLoggerDebug)

        checkPlayerInventoryLog = mainConfig.getString("checkPlayerInventory.log", checkPlayerInventoryLog)!!
        checkPlayerInventoryInfo = mainConfig.getString("checkPlayerInventory.info", checkPlayerInventoryInfo)!!
        checkPlayerInventoryItems.clear()
        checkPlayerInventoryItems.addAll(mainConfig.getStringList("checkPlayerInventory.item").map {
            val pair = it.split("|")
            CheckPlayerInvItem(
                pair[0].asItemTypeWrapper(),
                pair[1].toInt()
            )
        })

        saturdayNightVisionEnable = mainConfig.getBoolean("saturdayNightVision.enable", saturdayNightVisionEnable)
        saturdayNightVisionMvpPerm = mainConfig.getString("saturdayNightVision.mvp", saturdayNightVisionMvpPerm)!!
        saturdayNightVisionToggleAction = mainConfig.getAction("saturdayNightVision.toggleAction")

        placeholders.clear()
        val placeholdersSect = mainConfig.getSection("placeholder")
        placeholdersSect.getKeys(false).forEach { placeholderName ->
            val placeholderSect = placeholdersSect.getSection(placeholderName)
            val target = placeholderSect.getString("target")!!
            val change = placeholderSect.getSection("change").let { changeSect ->
                changeSect.getKeys(false).associateWith { changeSect.getString(it)!! }
            }
            val other = placeholderSect.getString("other")!!
            placeholders[placeholderName] = PlaceholderConfig(
                placeholderName,
                target,
                change,
                other
            )
        }
        forwardMythicMobSummonDamage = mainConfig.getBoolean("forwardMythicMobSummonDamage", forwardMythicMobSummonDamage)
    }

    private fun loadWorldPopulator() {
        customBlockGenerators.clear()
        for (key in worldPopulatorConfig.getKeys(false)) {
            val sect = worldPopulatorConfig.getSection(key)
            val block = sect.getEnum<Material>("block")!!
            val worlds = sect.getStringList("worlds")
            val replaceableBlocks =
                sect.getStringList("replaceable_blocks").mapNotNull { EnumUtils.getEnum(Material::class.java, it) }
            val biomes = sect.getStringList("biomes").mapNotNull { EnumUtils.getEnum(Biome::class.java, it) }
            val minHeight = sect.getInt("min_height")
            val maxHeight = sect.getInt("max_height")
            val chunkChance = sect.getDouble("chunk_chance")
            val veinBlocks = sect.getInt("vein_blocks")
            val chunkVeins = sect.getInt("chunk_veins")
            customBlockGenerators[key] = CustomBlockGenerator(
                key,
                block,
                worlds,
                replaceableBlocks,
                biomes,
                minHeight,
                maxHeight,
                chunkChance,
                veinBlocks,
                chunkVeins
            )
        }
    }

    class CheckPlayerInvItem(
        val item: ItemTypeWrapper,
        val amount: Int
    )

    class PlaceholderConfig(
        val name: String,
        val target: String,
        val change: Map<String, String>,
        val other: String
    )

    class CustomBlockGenerator(
        val name: String,
        val block: Material,
        val worlds: List<String>,
        val replaceableBlocks: List<Material>,
        val biomes: List<Biome>,
        val minHeight: Int,
        val maxHeight: Int,
        val chunkChance: Double,
        val veinBlocks: Int,
        val chunkVeins: Int
    )

}