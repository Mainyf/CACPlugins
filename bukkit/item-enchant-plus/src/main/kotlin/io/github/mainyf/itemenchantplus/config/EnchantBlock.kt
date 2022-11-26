package io.github.mainyf.itemenchantplus.config

import dev.lone.itemsadder.api.CustomBlock
import org.bukkit.Material
import org.bukkit.block.Block

class EnchantBlock(val id: String) {

    fun equalsBlock(block: Block): Boolean {
        return id.uppercase() == block.type.name || id == CustomBlock.byAlreadyPlaced(block)?.namespacedID
    }

    fun equalsBlock(material: Material, customBlock: CustomBlock?): Boolean {
        return id.uppercase() == material.name || id == customBlock?.namespacedID
    }

}