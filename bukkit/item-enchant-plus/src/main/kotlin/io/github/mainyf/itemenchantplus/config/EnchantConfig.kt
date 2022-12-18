package io.github.mainyf.itemenchantplus.config

import io.github.mainyf.newmclib.utils.ItemTypeWrapper
import org.bukkit.Material
import org.bukkit.potion.PotionEffect

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
    val upgradeMaterials: List<List<EnchantMaterial>>,
    val comboAttenuation: Long,
    val combo1_2: LanRenCombo1_2Config,
    val combo3: LanRenCombo3Config,
    val combo4: LanRenCombo4Config,
)

class LanRenCombo1_2Config(
    val distance: List<Double>,
    val size: ModelSizeConfig,
    val baseDamage: List<Double>,
    val throughDamage: List<Double>,
    val pveDamage: List<Double>,
    val hitTargetShooterBuff: List<PotionEffect?>
)

class LanRenCombo3Config(
    val count: Int,
    val distance: Double,
    val size: ModelSizeConfig,
    val throughDamage: List<Double>,
    val hitTargetBuff: PotionEffect?
)

class LanRenCombo4Config(
    val distance: Double,
    val size: ModelSizeConfig,
    val throughDamage: Double
)

class ModelSizeConfig(
    val width: Double,
    val height: Double
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