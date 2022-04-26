package io.github.mainyf.itemskillsplus.config

class MenuSlotConfig(
    val initItemSkillTitle1: String,
    val initItemSkillTitle2: String,
    val initItemSkillTitle3: String,
    val upgradeItemSkillTitle1: String,
    val upgradeItemSkillTitle2: String,
    val skills: Map<String, ItemDisplayConfig>,
    val equipSlot: SlotConfig,
    val enchantSlot: EnchantSlot,
    val materialsOfAdequacySlot: MaterialsOfAdequacySlot,
    val materialsSlot: SlotConfig,
    val materialsCountSlot: MaterialsCountSlot,
    val completeSlot: SlotConfig
)

class SlotConfig(
    val initSlots: List<Int>,
    val upgradeSlots: List<Int>,
    val itemDisplay: ItemDisplayConfig
)

class EnchantSlot(
    val initSlots: List<Int>,
    val upgradeSlots: List<Int>,
    val initItemDisplay: ItemDisplayConfig,
    val upgradeItemDisplay: ItemDisplayConfig,
    val skills: Map<String, List<ItemTypeWrapper>>
)

class MaterialsOfAdequacySlot(
    val initSlots: List<Int>,
    val upgradeSlots: List<Int>,
    val default: ItemDisplayConfig,
    val satisfied: ItemDisplayConfig,
    val unSatisfied: ItemDisplayConfig
)

class MaterialsCountSlot(
    val initSlots: List<Int>,
    val upgradeSlots: List<Int>,
    val default: ItemDisplayConfig,
    val selectx32: ItemDisplayConfig,
    val selectx64: ItemDisplayConfig,
    val selectx128: ItemDisplayConfig,
    val selectx192: ItemDisplayConfig
)