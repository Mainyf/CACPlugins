package io.github.mainyf.itemskillsplus.config

import dev.lone.itemsadder.api.CustomStack
import net.kyori.adventure.text.Component
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

fun ConfigurationSection.asItemDisplay(): ItemDisplayConfig {
    val typeWrapper = asItemTypeWrapper()
    val name = getString("name")
    val lore = getStringList("lore")
    return ItemDisplayConfig(
        typeWrapper,
        name,
        lore
    )
}

fun ConfigurationSection.asItemTypeWrapper(): ItemTypeWrapper {
    val type = getString("type")
    return ItemTypeWrapper(type ?: "AIR")
}

fun String.asItemTypeWrapper(): ItemTypeWrapper {
    return ItemTypeWrapper(this)
}

class ItemDisplayConfig(
    val type: ItemTypeWrapper,
    val name: String?,
    val lore: List<String>?
) {

    companion object {

        val AIR = ItemDisplayConfig(ItemTypeWrapper("AIR"), null, null)

    }

    fun toItemStack(block: ItemStack.() -> Unit = {}): ItemStack {
        val item = type.toItemStack()
        val meta = item.itemMeta
        if (meta == null) {
            block.invoke(item)
            return item
        }
        var flag = false
        if (name != null) {
            flag = true
            meta.displayName(Component.text(name))
        }
        if (lore != null && lore.isNotEmpty()) {
            flag = true
            meta.lore(lore.map { Component.text(it) })
        }
        if (flag) {
            item.itemMeta = meta
        }
        block.invoke(item)
        return item
    }

}

class ItemTypeWrapper(id: String) {

    private val customStack: CustomStack? = CustomStack.getInstance(id)

    private val material: Material = EnumUtils.getEnum(Material::class.java, id) ?: Material.AIR

    fun toItemStack(): ItemStack {
        return customStack?.itemStack ?: ItemStack(material)
    }

}