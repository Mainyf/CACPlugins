package io.github.mainyf.soulbind.config

import io.github.mainyf.newmclib.config.DefaultSlotConfig
import io.github.mainyf.newmclib.config.MenuSettingsConfig

class RecallItemMenuConfig(
    val settings: MenuSettingsConfig,
    val recallItemSlot: DefaultSlotConfig,
    val prevSlot: DefaultSlotConfig,
    val nextSlot: DefaultSlotConfig,
    val infoSlot: DefaultSlotConfig,
    val backSlot: DefaultSlotConfig
)

class AbandonConfirmMenu(
    val settings: MenuSettingsConfig,
    val itemSlot: DefaultSlotConfig,
    val confirmSlot: DefaultSlotConfig,
    val backSlot: DefaultSlotConfig,
)