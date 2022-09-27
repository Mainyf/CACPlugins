package io.github.mainyf.questextension.config

import io.github.mainyf.newmclib.config.DefaultSlotConfig
import io.github.mainyf.newmclib.config.MenuSettingsConfig

data class QuestListMenuConfig(
    val settings: MenuSettingsConfig,
    val quest1Slot: DefaultSlotConfig,
    val quest2Slot: DefaultSlotConfig,
    val quest3Slot: DefaultSlotConfig,
    val quest4Slot: DefaultSlotConfig,
    val quest5Slot: DefaultSlotConfig,
    val rewardSlot: DefaultSlotConfig,
    val backSlot: DefaultSlotConfig
) {

    val questSlotList
        get() = listOf(
            quest1Slot,
            quest2Slot,
            quest3Slot,
            quest4Slot,
            quest5Slot
        )

}

data class QuestDetailMenuConfig(
    val settings: MenuSettingsConfig,
    val fsSlot: DefaultSlotConfig,
    val xypSlot: DefaultSlotConfig,
    val moneySlot: DefaultSlotConfig,
    val rewardSlot: DefaultSlotConfig,
    val backSlot: DefaultSlotConfig
)

data class QuestDetail2XMenuConfig(
    val step1Slot: DefaultSlotConfig,
    val step2Slot: DefaultSlotConfig
)

data class QuestDetail3XMenuConfig(
    val background: String,
    val step1Slot: DefaultSlotConfig,
    val step2Slot: DefaultSlotConfig,
    val step3Slot: DefaultSlotConfig
)