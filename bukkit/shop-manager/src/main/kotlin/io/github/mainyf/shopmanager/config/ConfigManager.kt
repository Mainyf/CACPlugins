@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.shopmanager.config

import io.github.mainyf.newmclib.config.BaseLang
import io.github.mainyf.newmclib.config.asDefaultSlotConfig
import io.github.mainyf.newmclib.config.asMenuSettingsConfig
import io.github.mainyf.shopmanager.ShopManager
import net.kyori.adventure.text.Component
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

fun CommandSender.sendLang(key: String, vararg data: Any) {
    ConfigManager.lang.apply {
        sendLang(key, *data)
    }
}

object ConfigManager {

    var debug = false

    private lateinit var mainConfigFile: FileConfiguration
    private lateinit var menuConfigFile: FileConfiguration
    private lateinit var langConfigFile: FileConfiguration

    lateinit var sellMenuConfig: SellMenuConfig

    var addPermission = "shopmanager.add"
    private val sellShopLimitMap = mutableMapOf<Material, SellShopLimit>()
    lateinit var lang: BaseLang

    private val addPermissionLevels = listOf(
        100,
        90,
        80,
        70,
        60,
        50,
        40,
        30,
        20,
        10
    )

    fun load() {
        ShopManager.INSTANCE.saveDefaultConfig()
        ShopManager.INSTANCE.reloadConfig()
        mainConfigFile = ShopManager.INSTANCE.config

        val menuFile = ShopManager.INSTANCE.dataFolder.resolve("menu.yml")
        if (!menuFile.exists()) {
            ShopManager.INSTANCE.saveResource("menu.yml", false)
        }
        val langFile = ShopManager.INSTANCE.dataFolder.resolve("lang.yml")
        if (!langFile.exists()) {
            ShopManager.INSTANCE.saveResource("lang.yml", false)
        }

        menuConfigFile = YamlConfiguration.loadConfiguration(menuFile)
        langConfigFile = YamlConfiguration.loadConfiguration(langFile)

        kotlin.runCatching {
            loadMenuConfig()
            loadMainConfig()
            lang = BaseLang()
            lang.load(langConfigFile)
        }.onFailure {
            ShopManager.LOGGER.info("加载配置时出现错误")
            it.printStackTrace()
        }
    }

    private fun loadMenuConfig() {
        val sellMenuSect = menuConfigFile.getConfigurationSection("sellMenu")!!
        sellMenuConfig = SellMenuConfig(
            sellMenuSect.asMenuSettingsConfig(),
            sellMenuSect.asDefaultSlotConfig("placeholderSlot"),
            sellMenuSect.asDefaultSlotConfig("sellSlot"),
        )
    }

    private fun loadMainConfig() {
        debug = mainConfigFile.getBoolean("debug", false)
        addPermission = mainConfigFile.getString("addPermission", addPermission)!!
        sellShopLimitMap.clear()
        val sellShopSect = mainConfigFile.getConfigurationSection("buyShop")!!
        sellShopSect.getKeys(false).forEach { key ->
            val material = EnumUtils.getEnum(Material::class.java, key.uppercase())
            if (material == null) {
                ShopManager.LOGGER.info("`${key}` 不是一个有效的物品类型，已忽略")
                return@forEach
            }
            kotlin.runCatching {
                val value = sellShopSect.getString(key)!!
                kotlin.runCatching {
                    val pair = value.split(",")
                    sellShopLimitMap[material] = SellShopLimit(pair[0].toDouble(), pair[1].toDouble())
                }.onFailure {
                    ShopManager.LOGGER.info("`${value}` 格式错误，ex: 1,5000")
                    it.printStackTrace()
                }
            }.onFailure {
                ShopManager.LOGGER.info("意外错误，请检查配置，`${key}` 项已忽略")
                it.printStackTrace()
            }
        }
    }

    fun hasSellable(material: Material): Boolean {
        return sellShopLimitMap.containsKey(material)
    }

    fun getSellShop(material: Material): SellShopLimit? {
        return sellShopLimitMap[material]
    }

    fun getMaxHarvest(player: Player, sellShop: SellShopLimit): Double {
        val value = addPermissionLevels.find { player.hasPermission("${addPermission}.${it}") }?.toDouble() ?: 0.0
        return sellShop.maxHarvest + (sellShop.maxHarvest * (value / 100.0))
    }

    class SellShopLimit(
        val price: Double,
        val maxHarvest: Double
    ) {

        fun getLangArr(player: Player, material: Material, count: Int): Array<Any> {
            return arrayOf(
                "{itemName}", Component.translatable(material),
                "{maxHarvest}", getMaxHarvest(player, this),
                "{eCount}", count.toString()
            )
        }

    }

}