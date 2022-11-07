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