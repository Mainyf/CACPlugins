package io.github.mainyf.itemenchantplus.config

import io.github.mainyf.newmclib.config.DefaultSlotConfig
import io.github.mainyf.newmclib.config.MenuSettingsConfig

class DashboardMenuConfig(
    val settings: MenuSettingsConfig,
    val giveEnchantSlot: DefaultSlotConfig,
    val intensifySlot: DefaultSlotConfig,
    val upgradeSlot: DefaultSlotConfig
)

class EnchantListMenuConfig(
    val settings: MenuSettingsConfig,
    val prevSlot: DefaultSlotConfig,
    val nextSlot: DefaultSlotConfig,
    val enchantSlot: DefaultSlotConfig,
    val backSlot: DefaultSlotConfig
)

class GiveEnchantMenuConfig(
    val settings: MenuSettingsConfig,
    val materialsSlot: DefaultSlotConfig,
    val infoSlot: DefaultSlotConfig,
    val equipSlot: DefaultSlotConfig,
    val backSlot: DefaultSlotConfig,
    val finishSlot: DefaultSlotConfig
)

class EnchantIntensifyMenuConfig(
    val settings: MenuSettingsConfig,
    val equipSlot: DefaultSlotConfig,
    val materialsSlot: DefaultSlotConfig,
    val backSlot: DefaultSlotConfig,
    val finishSlot: DefaultSlotConfig
)

class EnchantUpgradeMenuConfig(
    val settings: MenuSettingsConfig,
    val backgroundEquipNoEmpty: String,
    val materialsSlot: DefaultSlotConfig,
    val infoSlot: DefaultSlotConfig,
    val equipSlot: DefaultSlotConfig,
    val upgradeResultSlot: DefaultSlotConfig,
    val backSlot: DefaultSlotConfig,
    val finishSlot: DefaultSlotConfig
)

class EnchantSkinMenuConfig(
    val settings: MenuSettingsConfig,
    val largeSkinSlot: DefaultSlotConfig,
    val prevSlot: DefaultSlotConfig,
    val nextSlot: DefaultSlotConfig,
    val enchantSkinX1Slot: DefaultSlotConfig,
    val enchantSkinX2Slot: DefaultSlotConfig,
    val enchantSkinX3Slot: DefaultSlotConfig,
    val enchantSkinX4Slot: DefaultSlotConfig,
    val enchantSkinX5Slot: DefaultSlotConfig,
    val finishSlot: DefaultSlotConfig,
    val backSlot: DefaultSlotConfig
)