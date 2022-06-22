package io.github.mainyf.myislands.config

import io.github.mainyf.newmclib.config.DefaultSlotConfig
import io.github.mainyf.newmclib.config.ItemDisplayConfig
import io.github.mainyf.newmclib.config.action.MultiAction

class IslandMainMenuConfig(
    val cooldown: Long,
    val row: Int,
    val background: String,
    val prevSlot: DefaultSlotConfig,
    val nextSlot: DefaultSlotConfig,
    val islandViewSlot: DefaultSlotConfig,
    val switchViewIslandSlot: DefaultSlotConfig,
    val infoAndKudosSlot: InfoAndKudosSlotConfig,
    val upgradeAndBackIslandSlot: UpgradeAndBackIslandSlotSlotConfig,
    val islandSettingsSlot: DefaultSlotConfig
)

class IslandSettingsMenuConfig(
    val cooldown: Long,
    val row: Int,
    val background: String,
    val helpersSlot: DefaultSlotConfig,
    val moveCoreSlot: DefaultSlotConfig,
    val visibilitySlot: DefaultSlotConfig,
    val resetIslandSlot: DefaultSlotConfig
)

class IslandChooseMenuConfig(
    val cooldown: Long,
    val row: Int,
    val background: String,
    val islandListSlot: IslandPresetSlotConfig,
    val prevSlot: DefaultSlotConfig,
    val nextSlot: DefaultSlotConfig,
    val backSlot: DefaultSlotConfig
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