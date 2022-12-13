package io.github.mainyf.itemenchantplus.config

import io.github.mainyf.newmclib.utils.ItemTypeWrapper
import org.bukkit.Material

class ExpandEnchantConfig(
    val enable: Boolean,
    val name: String,
    val description: List<String>,
    val allowGiveItem: List<Material>,
    val menuItemInListMenu: List<String>,
    val menuItemInGiveMenu: List<String>,
    val menuItemInUpgradeMenu: List<String>,
    val allowBlocks: List<EnchantBlock>,
    val defaultSkin: EnchantSkinConfig,
    val upgradeMaterials: List<List<EnchantMaterial>>
)

class LuckEnchantConfig(
    val enable: Boolean,
    val name: String,
    val description: List<String>,
    val allowGiveItem: List<Material>,
    val menuItemInListMenu: List<String>,
    val menuItemInGiveMenu: List<String>,
    val menuItemInUpgradeMenu: List<String>,
    val max: Int,
    val luckPercentage: LuckPercentage,
    val allowBlocks: List<EnchantBlock>,
    val defaultSkin: EnchantSkinConfig,
    val upgradeMaterials: List<List<EnchantMaterial>>
)

class LanRenEnchantConfig(
    val enable: Boolean,
    val name: String,
    val description: List<String>,
    val allowGiveItem: List<Material>,
    val menuItemInListMenu: List<String>,
    val menuItemInGiveMenu: List<String>,
    val menuItemInUpgradeMenu: List<String>,
    val defaultSkin: EnchantSkinConfig,
    val upgradeMaterials: List<List<EnchantMaterial>>
)

class LuckPercentage(
    val stage1_2x: Double,
    val stage2_2x: Double,
    val stage3_2x: Double,
    val stage3_3x: Double
)

class EnchantMaterial(
    val item: ItemTypeWrapper,
    val amount: Int
)