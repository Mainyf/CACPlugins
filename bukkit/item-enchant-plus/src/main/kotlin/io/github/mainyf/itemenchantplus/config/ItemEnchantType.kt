package io.github.mainyf.itemenchantplus.config

import io.github.mainyf.itemenchantplus.ItemEnchantPlus
import io.github.mainyf.newmclib.exts.toReflect
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment

enum class ItemEnchantType(val namespacedKey: NamespacedKey) {

    EXPAND(NamespacedKey(ItemEnchantPlus.INSTANCE, "expand")),
    LUCK(NamespacedKey(ItemEnchantPlus.INSTANCE, "luck")),
    LAN_REN(NamespacedKey(ItemEnchantPlus.INSTANCE, "lan_ren"));

    companion object {

        fun of(name: String): ItemEnchantType? {
            return EnumUtils.getEnum(ItemEnchantType::class.java, name.uppercase())
        }

    }

    fun plusExtraDataName(): String {
        return "${namespacedKey.key}Plus"
    }

    fun enchantConfig(): Any {
        return ConfigIEP.enchants[this]!!
    }

    fun conflictEnchant(): List<Enchantment> {
        return enchantConfig().toReflect().get("conflictEnchant")
    }

    fun defaultSkin(): EnchantSkinConfig {
        return enchantConfig().toReflect().get("defaultSkin")
    }

    fun displayName(): String {
        return enchantConfig().toReflect().get("name")
    }

    fun plusDisplayName(): String {
        return enchantConfig().toReflect().get("plusName")
    }

    fun description(): List<String> {
        return enchantConfig().toReflect().get("description")
    }

    fun plusDescription(): List<String> {
        return enchantConfig().toReflect().get("plusDescription")
    }

    fun menuItemInListMenu(): List<String> {
        return enchantConfig().toReflect().get("menuItemInListMenu")
    }

    fun menuItemInGiveMenu(): List<String> {
        return enchantConfig().toReflect().get("menuItemInGiveMenu")
    }

    fun menuItemInUpgradeMenu(): List<String> {
        return enchantConfig().toReflect().get("menuItemInUpgradeMenu")
    }

    fun allowGiveItem(): List<Material> {
        return enchantConfig().toReflect().get("allowGiveItem")
    }

    fun upgradeMaterials(): List<List<EnchantMaterial>> {
        return enchantConfig().toReflect().get("upgradeMaterials")
    }

}