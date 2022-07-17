package io.github.mainyf.socialsystem.config

import io.github.mainyf.newmclib.config.DefaultSlotConfig
import io.github.mainyf.newmclib.config.ItemSlotConfig
import io.github.mainyf.newmclib.config.MenuSettingsConfig

class SocialCardMenuConfig(
    val settings: MenuSettingsConfig,
    val requestSlot: DefaultSlotConfig,
    val repairSlot: DefaultSlotConfig,
    val headSlot: DefaultSlotConfig,
    val cardX1Slot: DefaultSlotConfig,
    val cardX2Slot: DefaultSlotConfig,
    val cardX3Slot: DefaultSlotConfig,
    val cardX4Slot: DefaultSlotConfig,
    val onlineSlot: DefaultSlotConfig,
    val helmetSlot: DefaultSlotConfig,
    val chestplateSlot: DefaultSlotConfig,
    val leggingsSlot: DefaultSlotConfig,
    val bootsSlot: DefaultSlotConfig
)

class SocialMainMenuConfig(
    val settings: MenuSettingsConfig,
    val prevSlot: DefaultSlotConfig,
    val nextSlot: DefaultSlotConfig,
    val friendsSlot: DefaultSlotConfig,
    val headSlot: DefaultSlotConfig,
    val cardX1Slot: DefaultSlotConfig,
    val cardX2Slot: DefaultSlotConfig,
    val cardX3Slot: DefaultSlotConfig,
    val cardX4Slot: DefaultSlotConfig,
    val onlineSlot: DefaultSlotConfig,
    val deleteFriendOrAllowRepairSlot: DefaultSlotConfig,
    val tpSlot: DefaultSlotConfig
)