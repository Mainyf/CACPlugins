package io.github.mainyf.myislands.config

import io.github.mainyf.newmclib.config.DefaultSlotConfig
import io.github.mainyf.newmclib.config.ItemSlotConfig
import io.github.mainyf.newmclib.config.MenuSettingsConfig

class IslandMainMenuConfig(
    val settings: MenuSettingsConfig,
    val prevSlot: DefaultSlotConfig,
    val nextSlot: DefaultSlotConfig,
    val islandViewSlot: DefaultSlotConfig,
    val switchViewIslandSlot: DefaultSlotConfig,
    val infoAndKudosSlot: InfoAndKudosSlotConfig,
    val upgradeAndBackIslandSlot: UpgradeAndBackIslandSlotSlotConfig,
    val islandSettingsSlot: DefaultSlotConfig
)

class IslandSettingsMenuConfig(
    val settings: MenuSettingsConfig,
    val helpersSlot: IslandHelperSlotConfig,
    val moveCoreSlot: DefaultSlotConfig,
    val visibilitySlot: DefaultSlotConfig,
    val resetIslandSlot: DefaultSlotConfig
)

class IslandChooseMenuConfig(
    val settings: MenuSettingsConfig,
    val islandListSlot: IslandPresetSlotConfig,
    val prevSlot: DefaultSlotConfig,
    val nextSlot: DefaultSlotConfig,
    val backSlot: IslandChooseBackSlotConfig
)

class IslandHelperSelectMenuConfig(
    val settings: MenuSettingsConfig,
    val playerListSlot: DefaultSlotConfig,
    val prevSlot: DefaultSlotConfig,
    val nextSlot: DefaultSlotConfig,
    val backSlot: DefaultSlotConfig
)

class IslandChooseBackSlotConfig(
    val slot: List<Int>,
    val backCity: ItemSlotConfig,
    val backPrev: ItemSlotConfig
)

class IslandHelperSlotConfig(
    val slot: List<Int>,
    val itemSlot: ItemSlotConfig?,
    val emptyItemSlot: ItemSlotConfig?
)

class IslandPresetSlotConfig(
    val slot: List<List<Int>>,
    val itemSlot: ItemSlotConfig?
)

class InfoAndKudosSlotConfig(
    val slot: List<Int>,
    val info: ItemSlotConfig,
    val kudos: ItemSlotConfig
)

class UpgradeAndBackIslandSlotSlotConfig(
    val slot: List<Int>,
    val upgrade: ItemSlotConfig,
    val back: ItemSlotConfig
)