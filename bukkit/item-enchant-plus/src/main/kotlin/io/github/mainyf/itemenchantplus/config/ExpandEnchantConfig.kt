package io.github.mainyf.itemenchantplus.config

import dev.lone.itemsadder.api.CustomBlock
import io.github.mainyf.newmclib.utils.ItemTypeWrapper
import org.bukkit.block.Block

class ExpandEnchantConfig(
    val id: String,
    val enable: Boolean,
    val name: String,
    val description: List<String>,
    val allowBlocks: List<EnchantBlock>,
    val defaultSkin: EnchantSkinConfig,
    val upgradeMaterials: List<EnchantMaterial>
)

class LuckEnchantConfig(
    val id: String,
    val enable: Boolean,
    val name: String,
    val description: List<String>,
    val allowBlocks: List<EnchantBlock>,
    val defaultSkin: EnchantSkinConfig,
    val upgradeMaterials: List<EnchantMaterial>
)

class EnchantMaterial(
    val item: ItemTypeWrapper,
    val amount: Int
)

class EnchantBlock(val id: String) {

    fun equalsBlock(block: Block): Boolean {
        return id.uppercase() == block.type.name || id == CustomBlock.byAlreadyPlaced(block)?.namespacedID
    }

}