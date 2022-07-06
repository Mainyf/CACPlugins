package io.github.mainyf.socialsystem.config

import io.github.mainyf.newmclib.config.DefaultSlotConfig
import io.github.mainyf.newmclib.config.ItemSlotConfig
import io.github.mainyf.newmclib.config.MenuSettingsConfig

class SocialCardMenu(
    val settings: MenuSettingsConfig,
    val requestSlot: DefaultSlotConfig,
    val repairSlot: DefaultSlotConfig,
    val headSlot: DefaultSlotConfig,
    val cardX1Slot: DefaultSlotConfig,
    val cardX2Slot: DefaultSlotConfig,
    val cardX3Slot: DefaultSlotConfig,
    val cardX4Slot: DefaultSlotConfig,
    val onlineSlot: DefaultSlotConfig
)

class SocialOnlineSlot(
    val slot: List<Int>,
    val onlineItem: ItemSlotConfig,
    val offlineItem: ItemSlotConfig
)