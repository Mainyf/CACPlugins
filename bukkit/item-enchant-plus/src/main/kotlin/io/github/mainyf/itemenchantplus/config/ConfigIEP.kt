package io.github.mainyf.itemenchantplus.config

import com.udojava.evalex.Expression
import io.github.mainyf.itemenchantplus.ItemEnchantPlus
import io.github.mainyf.newmclib.config.BaseLang
import io.github.mainyf.newmclib.config.asDefaultSlotConfig
import io.github.mainyf.newmclib.config.asMenuSettingsConfig
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.utils.ItemTypeWrapper
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack

fun CommandSender.sendLang(key: String, vararg data: Any) {
    ConfigIEP.lang.apply {
        sendLang(key, *data)
    }
}

object ConfigIEP {

    private lateinit var langConfig: FileConfiguration
    private lateinit var mainConfig: FileConfiguration
    private lateinit var skinConfig: FileConfiguration
    private lateinit var menuConfig: FileConfiguration
    private lateinit var expandConfig: FileConfiguration
    private lateinit var luckConfig: FileConfiguration

    private lateinit var levelExpression: String
    val enchantIntensifyMaterials = mutableMapOf<ItemTypeWrapper, Double>()

    val itemSkins = mutableMapOf<String, EnchantSkinConfig>()
    val enchants = mutableMapOf<ItemEnchantType, Any>()

    lateinit var expandEnchantConfig: ExpandEnchantConfig
    lateinit var luckEnchantConfig: LuckEnchantConfig

    //    val expandEnchant get() = enchants["expand"]!!

    lateinit var dashboardMenuConfig: DashboardMenuConfig
    lateinit var enchantListMenuConfig: EnchantListMenuConfig
    lateinit var giveEnchantMenuConfig: GiveEnchantMenuConfig
    lateinit var enchantIntensifyMenuConfig: EnchantIntensifyMenuConfig
    lateinit var enchantUpgradeMenuConfig: EnchantUpgradeMenuConfig
    lateinit var enchantSkinMenuConfig: EnchantSkinMenuConfig
    lateinit var lang: BaseLang

    fun init() {
        kotlin.runCatching {

            langConfig = ItemEnchantPlus.INSTANCE.createFileConfiguration("lang.yml")
            mainConfig = ItemEnchantPlus.INSTANCE.createFileConfiguration("config.yml")
            skinConfig = ItemEnchantPlus.INSTANCE.createFileConfiguration("skins.yml")
            menuConfig = ItemEnchantPlus.INSTANCE.createFileConfiguration("menu.yml")
            expandConfig = ItemEnchantPlus.INSTANCE.createFileConfiguration("enchants/expand.yml")
            luckConfig = ItemEnchantPlus.INSTANCE.createFileConfiguration("enchants/luck.yml")

            lang = BaseLang()
            lang.load(langConfig)
            loadMainConfig()
            loadSkinConfig()
            loadMenuConfig()
            loadEnchantConfig()
        }.onFailure {
            ItemEnchantPlus.INSTANCE.slF4JLogger.error("加载配置时出现错误", it)
        }
    }

    private fun loadMainConfig() {
        levelExpression = mainConfig.getString("level")!!
        enchantIntensifyMaterials.clear()
        mainConfig.getStringList("enchantIntensifyMaterials").forEach {
            val pair2 = it.split("|")
            enchantIntensifyMaterials[ItemTypeWrapper(pair2[0])] = pair2[1].toDouble()
        }
    }

    private fun loadSkinConfig() {
        itemSkins.clear()
        skinConfig.getKeys(false).forEach { skinName ->
            val skinSect = skinConfig.getSection(skinName)
            val enable = skinSect.getBoolean("enable")
            val enchantType = skinSect.getStringList("enchantType").map {
                EnumUtils.getEnum(ItemEnchantType::class.java, it)
            }
            //            val enchantType = skinSect.getEnum<ItemEnchantType>("enchantType")!!
            val skinEffectSectList = skinSect.getListAsConfigSection("skinEffect")
            val skinEffect = mutableListOf<SkinEffect>()
            skinEffectSectList.forEach { skinEffectSect ->
                val customModelData = skinEffectSect.getInt("customModelData")
                val menuItemName = skinEffectSect.getString("menuItemName")!!.colored()
                val menuItemLore = skinEffectSect.getStringList("menuItemLore").map { it.colored() }
                val effectSectList = skinEffectSect.getListAsConfigSection("effect")
                val effects = mutableListOf<SkinEffectItem>()
                effectSectList.forEach { effectSect ->
                    val triggerType = effectSect.getEnum<EffectTriggerType>("type")!!
                    val plays = effectSect.getPlay("value")
                    effects.add(SkinEffectItem(triggerType, plays))
                }
                skinEffect.add(SkinEffect(customModelData, menuItemName, menuItemLore, effects))
            }
            itemSkins[skinName] = EnchantSkinConfig(skinName, enable, enchantType, skinEffect)
        }
    }

    private fun loadMenuConfig() {
        val dashboardMenuSect = menuConfig.getSection("dashboardMenu")
        dashboardMenuConfig = DashboardMenuConfig(
            dashboardMenuSect.asMenuSettingsConfig(),
            dashboardMenuSect.asDefaultSlotConfig("giveEnchantSlot"),
            dashboardMenuSect.asDefaultSlotConfig("intensifySlot"),
            dashboardMenuSect.asDefaultSlotConfig("upgradeSlot")
        )
        val enchantListMenuSect = menuConfig.getSection("enchantListMenu")
        enchantListMenuConfig = EnchantListMenuConfig(
            enchantListMenuSect.asMenuSettingsConfig(),
            enchantListMenuSect.asDefaultSlotConfig("prevSlot"),
            enchantListMenuSect.asDefaultSlotConfig("nextSlot"),
            enchantListMenuSect.asDefaultSlotConfig("enchantSlot"),
            enchantListMenuSect.asDefaultSlotConfig("backSlot")
        )

        val giveEnchantMenuSect = menuConfig.getSection("giveEnchantMenu")
        giveEnchantMenuConfig = GiveEnchantMenuConfig(
            giveEnchantMenuSect.asMenuSettingsConfig(),
            giveEnchantMenuSect.asDefaultSlotConfig("materialsSlot"),
            giveEnchantMenuSect.asDefaultSlotConfig("infoSlot"),
            giveEnchantMenuSect.asDefaultSlotConfig("equipSlot"),
            giveEnchantMenuSect.asDefaultSlotConfig("backSlot"),
            giveEnchantMenuSect.asDefaultSlotConfig("finishSlot")
        )

        val enchantIntensifyMenuSect = menuConfig.getSection("enchantIntensifyMenu")
        enchantIntensifyMenuConfig = EnchantIntensifyMenuConfig(
            enchantIntensifyMenuSect.asMenuSettingsConfig(),
            enchantIntensifyMenuSect.asDefaultSlotConfig("equipSlot"),
            enchantIntensifyMenuSect.asDefaultSlotConfig("materialsSlot"),
            enchantIntensifyMenuSect.asDefaultSlotConfig("backSlot"),
            enchantIntensifyMenuSect.asDefaultSlotConfig("finishSlot"),
        )

        val enchantUpgradeMenuSect = menuConfig.getSection("enchantUpgradeMenu")
        enchantUpgradeMenuConfig = EnchantUpgradeMenuConfig(
            enchantUpgradeMenuSect.asMenuSettingsConfig(),
            enchantUpgradeMenuSect.getString("backgroundEquipNoEmpty")!!,
            enchantUpgradeMenuSect.asDefaultSlotConfig("materialsSlot"),
            enchantUpgradeMenuSect.asDefaultSlotConfig("infoSlot"),
            enchantUpgradeMenuSect.asDefaultSlotConfig("equipSlot"),
            enchantUpgradeMenuSect.asDefaultSlotConfig("upgradeResultSlot"),
            enchantUpgradeMenuSect.asDefaultSlotConfig("backSlot"),
            enchantUpgradeMenuSect.asDefaultSlotConfig("finishSlot")
        )

        val enchantSkinMenuSect = menuConfig.getSection("enchantSkinMenu")
        enchantSkinMenuConfig = EnchantSkinMenuConfig(
            enchantSkinMenuSect.asMenuSettingsConfig(),
            enchantSkinMenuSect.asDefaultSlotConfig("largeSkinSlot"),
            enchantSkinMenuSect.asDefaultSlotConfig("prevSlot"),
            enchantSkinMenuSect.asDefaultSlotConfig("nextSlot"),
            enchantSkinMenuSect.asDefaultSlotConfig("enchantSkinSlot"),
            enchantSkinMenuSect.asDefaultSlotConfig("backSlot")
        )
    }

    private fun loadEnchantConfig() {
        enchants.clear()
        loadExpandEnchantConfig()
        loadLuckEnchantConfig()
        //        enchantConfig.getKeys(false).forEach { enchantID ->
        //            val enchantSect = enchantConfig.getSection(enchantID)
        //            val enable = enchantSect.getBoolean("enable")
        //            val name = enchantSect.getString("name")!!
        //            val description = enchantSect.getStringList("description")
        //            val allowBlocks = enchantSect.getStringList("allowBlocks").map { EnchantBlock(it) }
        //            val defaultSkin = itemSkins[enchantSect.getString("defaultSkin")]!!
        //            val upgradeMaterials = enchantSect.getStringList("upgradeMaterials").map {
        //                val pair = it.split("|")
        //                EnchantMaterial(
        //                    ItemTypeWrapper(pair[0]),
        //                    pair[1].toInt()
        //                )
        //            }
        //            enchants[enchantID] = ExpandEnchantConfig(
        //                enchantID,
        //                enable,
        //                name,
        //                description,
        //                allowBlocks,
        //                defaultSkin,
        //                upgradeMaterials
        //            )
        //        }
    }

    private fun loadExpandEnchantConfig() {
        expandEnchantConfig = ExpandEnchantConfig(
            expandConfig.getBoolean("enable"),
            expandConfig.getString("name")!!,
            expandConfig.getStringList("description"),
            expandConfig.getStringList("allowGiveItem").map { Material.valueOf(it.uppercase()) },
            expandConfig.getStringList("menuItemInListMenu"),
            expandConfig.getStringList("menuItemInGiveMenu"),
            expandConfig.getStringList("menuItemInUpgradeMenu"),
            expandConfig.getStringList("allowBlocks").map { EnchantBlock(it) },
            itemSkins[expandConfig.getString("defaultSkin")]!!,
            expandConfig.getStringList("upgradeMaterials").map {
                val pair = it.split(",")
                pair.map { it2 ->
                    val pair2 = it2.split("|")
                    EnchantMaterial(
                        ItemTypeWrapper(pair2[0]),
                        pair2[1].toInt()
                    )
                }
            }
        )
        enchants[ItemEnchantType.EXPAND] = expandEnchantConfig
    }

    private fun loadLuckEnchantConfig() {
        luckEnchantConfig = LuckEnchantConfig(
            luckConfig.getBoolean("enable"),
            luckConfig.getString("name")!!,
            luckConfig.getStringList("description"),
            luckConfig.getStringList("allowGiveItem").map { Material.valueOf(it.uppercase()) },
            luckConfig.getStringList("menuItemInListMenu"),
            luckConfig.getStringList("menuItemInGiveMenu"),
            luckConfig.getStringList("menuItemInUpgradeMenu"),
            luckConfig.getInt("max"),
            luckConfig.getSection("luckPercentage").run {
                LuckPercentage(
                    getDouble("stage1-2x"),
                    getDouble("stage2-2x"),
                    getDouble("stage3-2x"),
                    getDouble("stage3-3x")
                )
            },
            luckConfig.getStringList("allowBlocks").map { EnchantBlock(it) },
            itemSkins[luckConfig.getString("defaultSkin")]!!,
            luckConfig.getStringList("upgradeMaterials").map {
                val pair = it.split(",")
                pair.map { it2 ->
                    val pair2 = it2.split("|")
                    EnchantMaterial(
                        ItemTypeWrapper(pair2[0]),
                        pair2[1].toInt()
                    )
                }
            }
        )
        enchants[ItemEnchantType.LUCK] = luckEnchantConfig
    }

    fun getMaterialExp(itemStack: ItemStack): Double {
        return enchantIntensifyMaterials.find { it.key.equalsItem(itemStack) } ?: 0.0
    }

    fun getLevelMaxExp(level: Int): Double {
        return Expression(levelExpression).setVariable("level", level.toString()).eval().toDouble()
    }

    fun getSkinByName(skinName: String): EnchantSkinConfig? {
        return itemSkins[skinName]
    }

}