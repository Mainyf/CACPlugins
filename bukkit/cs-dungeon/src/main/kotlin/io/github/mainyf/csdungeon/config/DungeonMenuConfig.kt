package io.github.mainyf.csdungeon.config

import io.github.mainyf.newmclib.config.DefaultSlotConfig
import io.github.mainyf.newmclib.config.MenuSettingsConfig

class DungeonMenuConfig(
    val settings: MenuSettingsConfig,
    val level1Slot: DefaultSlotConfig,
    val level2Slot: DefaultSlotConfig,
    val level3Slot: DefaultSlotConfig
)