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
    val backgroundFriend: String,
    val prevSlot: DefaultSlotConfig,
    val nextSlot: DefaultSlotConfig,
    val backSlot: DefaultSlotConfig,
    val friendsSlot: DefaultSlotConfig,
    val headSlot: DefaultSlotConfig,
    val cardX1Slot: DefaultSlotConfig,
    val cardX2Slot: DefaultSlotConfig,
    val cardX3Slot: DefaultSlotConfig,
    val cardX4Slot: DefaultSlotConfig,
    val onlineSlot: DefaultSlotConfig,
    val deleteSlot: DefaultSlotConfig,
    val allowRepairSlot: DefaultSlotConfig,
    val tpSlot: DefaultSlotConfig,
    val tpIsland: DefaultSlotConfig,
    val nickname: DefaultSlotConfig
)

class SocialIslandTPMenuConfig(
    val settings: MenuSettingsConfig,
    val plot1Slot: DefaultSlotConfig,
    val plot2Slot: DefaultSlotConfig,
    val infoSlot: DefaultSlotConfig,
    val backSlot: DefaultSlotConfig
)