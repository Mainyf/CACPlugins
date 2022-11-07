package io.github.mainyf.itemenchantplus.config

import io.github.mainyf.itemenchantplus.ItemEnchantPlus
import io.github.mainyf.newmclib.config.asDefaultSlotConfig
import io.github.mainyf.newmclib.config.asMenuSettingsConfig
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.utils.ItemTypeWrapper
import org.bukkit.configuration.file.FileConfiguration

object ConfigIEP {

    private lateinit var skinConfig: FileConfiguration
    private lateinit var enchantConfig: FileConfiguration
    private lateinit var menuConfig: FileConfiguration

    val itemSkins = mutableMapOf<String, EnchantSkinConfig>()
    val enchants = mutableMapOf<String, ExpandEnchantConfig>()

    val expandEnchant get() = enchants["expand"]!!

    lateinit var dashboardMenuConfig: DashboardMenuConfig
    lateinit var enchantListMenuConfig: EnchantListMenuConfig

    fun init() {
        kotlin.runCatching {

            skinConfig = ItemEnchantPlus.INSTANCE.createFileConfiguration("skins.yml")
            enchantConfig = ItemEnchantPlus.INSTANCE.createFileConfiguration("enchant.yml")
            menuConfig = ItemEnchantPlus.INSTANCE.createFileConfiguration("menu.yml")

            loadSkinConfig()
            loadEnchantConfig()
            loadMenuConfig()
        }.onFailure {
            ItemEnchantPlus.INSTANCE.slF4JLogger.error("加载配置时出现错误", it)
        }
    }

    private fun loadSkinConfig() {
        itemSkins.clear()
        skinConfig.getKeys(false).forEach { skinName ->
            val skinSect = skinConfig.getSection(skinName)
            val enable = skinSect.getBoolean("enable")
            val enchantType = skinSect.getEnum<ItemEnchant>("enchantType")!!
            val skinEffectSectList = skinSect.getListAsConfigSection("skinEffect")
            val skinEffect = mutableListOf<SkinEffect>()
            skinEffectSectList.forEach { skinEffectSect ->
                val customModelData = skinEffectSect.getInt("customModelData")
                val effectSectList = skinEffectSect.getListAsConfigSection("effect")
                val effects = mutableListOf<SkinEffectItem>()
                effectSectList.forEach { effectSect ->
                    val triggerType = effectSect.getEnum<EffectTriggerType>("type")!!
                    val plays = effectSect.getPlay("value")
                    effects.add(SkinEffectItem(triggerType, plays))
                }
                skinEffect.add(SkinEffect(customModelData, effects))
            }
            itemSkins[skinName] = EnchantSkinConfig(skinName, enable, enchantType, skinEffect)
        }
    }

    private fun loadEnchantConfig() {
        enchants.clear()
        enchantConfig.getKeys(false).forEach { enchantID ->
            val enchantSect = enchantConfig.getSection(enchantID)
            val enable = enchantSect.getBoolean("enable")
            val name = enchantSect.getString("name")!!
            val description = enchantSect.getStringList("description")
            val allowBlocks = enchantSect.getStringList("allowBlocks").map { EnchantBlock(it) }
            val defaultSkin = itemSkins[enchantSect.getString("defaultSkin")]!!
            val upgradeMaterials = enchantSect.getStringList("upgradeMaterials").map {
                val pair = it.split("|")
                EnchantMaterial(
                    ItemTypeWrapper(pair[0]),
                    pair[1].toInt()
                )
            }
            enchants[enchantID] = ExpandEnchantConfig(
                enchantID,
                enable,
                name,
                description,
                allowBlocks,
                defaultSkin,
                upgradeMaterials
            )
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
    }
}