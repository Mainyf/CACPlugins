package io.github.mainyf.itemenchantplus.config

import io.github.mainyf.newmclib.config.play.MultiPlay

data class EnchantSkinConfig(
    val name: String,
    val enable: Boolean,
    val enchantType: List<ItemEnchantType>,
    val skinEffect: List<SkinEffect>
)

data class SkinEffect(
    val customModelData: Int,
    val menuItemName: String,
    val menuItemLore: List<String>,
    val effects: List<SkinEffectItem>
)

data class SkinEffectItem(
    val type: EffectTriggerType,
    val value: MultiPlay?
)

enum class EffectTriggerType {
    BREAK_BLOCK
}

