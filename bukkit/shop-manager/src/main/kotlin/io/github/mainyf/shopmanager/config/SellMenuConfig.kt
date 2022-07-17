package io.github.mainyf.shopmanager.config

import io.github.mainyf.newmclib.config.DefaultSlotConfig
import io.github.mainyf.newmclib.config.MenuSettingsConfig

class SellMenuConfig(
    val settings: MenuSettingsConfig,
    val placeholderSlot: DefaultSlotConfig,
    val sellSlot: DefaultSlotConfig
)