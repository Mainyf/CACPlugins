package io.github.mainyf.myislands.config

import io.github.mainyf.newmclib.config.ItemDisplayConfig

class IslandMainMenuConfig(
    val prevSlot: SlotConfig,
    val nextSlot: SlotConfig,
    val islandViewSlot: SlotConfig,
    val switchViewIslandSlot: SlotConfig,
    val infoAndKudosSlot: InfoAndKudosSlotConfig,
    val upgradeAndBackIslandSlot: UpgradeAndBackIslandSlotSlotConfig,
    val islandSettingsSlot: SlotConfig
)

class IslandSettingsMenuConfig(
    val helpersSlot: SlotConfig,
    val moveCoreSlot: SlotConfig,
    val visibilitySlot: SlotConfig,
    val resetIslandSlot: SlotConfig
)

class SlotConfig(
    val slot: List<Int>,
    val itemDisplay: ItemDisplayConfig?
)

class InfoAndKudosSlotConfig(
    val slot: List<Int>,
    val info: ItemDisplayConfig,
    val kudos: ItemDisplayConfig
)

class UpgradeAndBackIslandSlotSlotConfig(
    val slot: List<Int>,
    val upgrade: ItemDisplayConfig,
    val back: ItemDisplayConfig
)