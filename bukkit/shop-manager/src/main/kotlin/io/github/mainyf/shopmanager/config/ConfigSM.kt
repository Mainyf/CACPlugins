@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.shopmanager.config

import io.github.mainyf.newmclib.config.BaseLang
import io.github.mainyf.newmclib.config.asDefaultSlotConfig
import io.github.mainyf.newmclib.config.asMenuSettingsConfig
import io.github.mainyf.newmclib.exts.getSection
import io.github.mainyf.newmclib.utils.ItemTypeWrapper
import io.github.mainyf.shopmanager.ShopManager
import net.kyori.adventure.text.Component
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun CommandSender.sendLang(key: String, vararg data: Any) {
    ConfigSM.lang.apply {
        sendLang(key, *data)
    }
}

object ConfigSM {

    var debug = false

    private lateinit var mainConfigFile: FileConfiguration
    private lateinit var menuConfigFile: FileConfiguration
    private lateinit var langConfigFile: FileConfiguration

    lateinit var sellMenuConfig: SellMenuConfig

    var addPermission = "shopmanager.add"
    private val sellShopLimitMap = mutableMapOf<Material, SellShopLimit>()
    private val shopPriceLimits = mutableListOf<ShopPriceLimit>()
    private val shopTaxs = mutableListOf<ShopTax>()
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

        shopPriceLimits.clear()
        val shopPriceLimitsSect = mainConfigFile.getSection("shopPrice")
        shopPriceLimitsSect.getKeys(false).forEach { key ->
            val pair = shopPriceLimitsSect.getString(key)!!.split(",")
            shopPriceLimits.add(
                ShopPriceLimit(
                    ItemTypeWrapper(key),
                    pair[0],
                    pair[1]
                )
            )
        }
        shopTaxs.clear()
        val shopTaxSect = mainConfigFile.getSection("shopTax")
        shopTaxSect.getKeys(false).forEach {
            shopTaxs.add(ShopTax(ItemTypeWrapper(it), shopTaxSect.getDouble(it)))
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

    fun getShopPriceLimitByItem(itemStack: ItemStack): ShopPriceLimit? {
        return shopPriceLimits.find {
            it.itemType.equalsItem(itemStack)
        }
    }

    fun getShopTaxByItem(itemStack: ItemStack): ShopTax? {
        return shopTaxs.find {
            it.itemType.equalsItem(itemStack)
        }
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

    class ShopPriceLimit(
        val itemType: ItemTypeWrapper,
        val min: String,
        val max: String
    ) {

        fun contains(value: Double): Boolean {
            if (min == "~" && max == "~") return true
            if (min == "~" && max != "~") {
                return value <= max.toDouble()
            }
            if (min != "~" && max == "~") {
                return value >= min.toDouble()
            }
            return value in min.toDouble() .. max.toDouble()
        }

    }

    class ShopTax(
        val itemType: ItemTypeWrapper,
        val taxValue: Double
    )

}