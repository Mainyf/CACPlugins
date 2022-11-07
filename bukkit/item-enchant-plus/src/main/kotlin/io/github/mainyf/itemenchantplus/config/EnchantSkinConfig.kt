package io.github.mainyf.itemenchantplus.config

import io.github.mainyf.newmclib.config.play.MultiPlay

data class EnchantSkinConfig(
    val name: String,
    val enable: Boolean,
    val enchantType: ItemEnchant,
    val skinEffect: List<SkinEffect>
)

data class SkinEffect(
    val customModelData: Int,
    val effects: List<SkinEffectItem>
)

data class SkinEffectItem(
    val type: EffectTriggerType,
    val value: MultiPlay?
)

enum class EffectTriggerType {
    BREAK_BLOCK
}

enum class ItemEnchant {

    EXPAND,
    LUCK

}