package io.github.mainyf.itemenchantplus.config

import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.config.play.MultiPlay
import org.bukkit.configuration.ConfigurationSection

data class EnchantSkinConfig(
    val name: String,
    val enable: Boolean,
    val enchantType: List<ItemEnchantType>,
    val priority: Int,
    val menuActions: MultiAction?,
    val menuBackground: IaIcon,
    val skinEffect: List<SkinEffect>,
    val data: EnchantSkinData?
) {

    fun getSkinEffect(stage: Int): SkinEffect {
        return skinEffect.getOrNull(stage - 1) ?: skinEffect.last()
    }

}

interface EnchantSkinData

@Suppress("UNCHECKED_CAST")
class LanRenEnchantSkinData(
    val comboModelDatas: List<LanRenModelData>
) : EnchantSkinData

class LanRenModelData(
    val modelName: String,
    val play: MultiPlay?
)

data class SkinEffect(
    val customModelData: Int,
    val menuLarge: SkinMenuItem,
    val menuItemName: String,
    val menuItemLore: List<String>,
    val selectItemName: String,
    val selectItemLore: List<String>,
    val effects: List<SkinEffectItem>
)

class SkinMenuItem(
    val customModelData: Int,
    val name: String,
    val lore: List<String>
)

data class SkinEffectItem(
    val type: EffectTriggerType,
    val value: MultiPlay?
)

enum class EffectTriggerType {
    BREAK_BLOCK
}

