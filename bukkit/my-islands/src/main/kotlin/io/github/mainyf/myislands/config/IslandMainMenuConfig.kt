package io.github.mainyf.myislands.config

import io.github.mainyf.newmclib.config.DefaultSlotConfig
import io.github.mainyf.newmclib.config.ItemDisplayConfig
import io.github.mainyf.newmclib.config.MenuSettingsConfig
import io.github.mainyf.newmclib.config.action.MultiAction

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
    val backCity: ItemDisplayConfig,
    val backPrev: ItemDisplayConfig,
    val backCityAction: MultiAction?,
    val backPrevAction: MultiAction?
)

class IslandHelperSlotConfig(
    val slot: List<Int>,
    val itemDisplay: ItemDisplayConfig?,
    val emptyItemDisplay: ItemDisplayConfig?,
    val action: MultiAction?,
    val emptyAction: MultiAction?,
)

class IslandPresetSlotConfig(
    val slot: List<List<Int>>,
    val itemDisplay: ItemDisplayConfig?,
    val action: MultiAction?
)

class InfoAndKudosSlotConfig(
    val slot: List<Int>,
    val info: ItemDisplayConfig,
    val kudos: ItemDisplayConfig,
    val infoAction: MultiAction?,
    val kudosAction: MultiAction?
)

class UpgradeAndBackIslandSlotSlotConfig(
    val slot: List<Int>,
    val upgrade: ItemDisplayConfig,
    val back: ItemDisplayConfig,
    val upgradeAction: MultiAction?,
    val backAction: MultiAction?
)