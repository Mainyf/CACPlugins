package io.github.mainyf.itemenchantplus

import io.github.mainyf.itemenchantplus.config.*
import io.github.mainyf.itemenchantplus.storage.StorageIEP
import io.github.mainyf.newmclib.exts.*
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

object EnchantManager {

    private val itemEnchantNameDataKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "itemEnchantName")

    // 物品uid
    private val itemUIDDataKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "itemUID")

    // 主人UID
    private val ownerUIDDataKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "ownerUID")

    // 主人 Name
    private val ownerNameDataKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "ownerName")

    private val stageKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "stageKey")

    private val levelKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "levelKey")

    private val expKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "expKey")

    private val skinKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "skinKey")

    private val stageText = arrayOf("", "I", "II", "III")

    private const val enchantLoreSpliter = "§i§t§c§h§a§n§t"

    fun initItemEnchant(
        player: Player,
        enchantType: ItemEnchantType,
        item: ItemStack
    ) {
        val meta = item.itemMeta!!

        val dataContainer = meta.persistentDataContainer

        val root = dataContainer.adapterContext.newPersistentDataContainer()

        val itemUID = StorageIEP.nextItemLongID()
        root.set(itemUIDDataKey, PersistentDataType.LONG, itemUID)
        root.set(ownerUIDDataKey, PersistentDataType.STRING, player.uniqueId.toString())
        root.set(ownerNameDataKey, PersistentDataType.STRING, player.name)

        root.set(stageKey, PersistentDataType.INTEGER, 0)
        root.set(levelKey, PersistentDataType.INTEGER, 1)
        root.set(expKey, PersistentDataType.DOUBLE, 0.0)
        root.set(skinKey, PersistentDataType.STRING, enchantType.defaultSkin().name)

        dataContainer.set(
            enchantType.namespacedKey,
            PersistentDataType.TAG_CONTAINER,
            root
        )
        dataContainer.set(
            itemEnchantNameDataKey,
            PersistentDataType.STRING,
            enchantType.name.lowercase()
        )

        item.itemMeta = meta
    }

    fun setItemEnchantData(item: ItemStack, data: EnchantData) {
        val meta = item.itemMeta ?: return
        val enchantName = meta.persistentDataContainer.get(itemEnchantNameDataKey, PersistentDataType.STRING) ?: return
        val enchantType = ItemEnchantType.of(enchantName) ?: return
        setItemEnchantData(enchantType, item, data)
    }

    fun setItemEnchantData(enchantType: ItemEnchantType, item: ItemStack, data: EnchantData) {
        trySetEnchantItemData(enchantType, item) { dataContainer ->
            dataContainer.set(itemUIDDataKey, PersistentDataType.LONG, data.itemUID)
            dataContainer.set(ownerUIDDataKey, PersistentDataType.STRING, data.ownerUID.toString())
            dataContainer.set(ownerNameDataKey, PersistentDataType.STRING, data.ownerName)

            dataContainer.set(stageKey, PersistentDataType.INTEGER, data.stage)
            dataContainer.set(levelKey, PersistentDataType.INTEGER, data.level)
            dataContainer.set(expKey, PersistentDataType.DOUBLE, data.exp)
            dataContainer.set(skinKey, PersistentDataType.STRING, data.enchantSkin.skinConfig.name)
        }
    }

    fun setExtraDataToItem(enchantType: ItemEnchantType, item: ItemStack, name: String) {
        trySetEnchantItemData(enchantType, item) {
            it.set(NamespacedKey(ItemEnchantPlus.INSTANCE, name), PersistentDataType.INTEGER, 1)
        }
    }

    fun removeExtraDataToItem(enchantType: ItemEnchantType, item: ItemStack, name: String) {
        trySetEnchantItemData(enchantType, item) {
            it.set(NamespacedKey(ItemEnchantPlus.INSTANCE, name), PersistentDataType.INTEGER, 0)
        }
    }

    fun hasExtraData(enchantType: ItemEnchantType, item: ItemStack, name: String): Boolean {
        val meta = item.itemMeta ?: return false
        val rootTag = meta.persistentDataContainer
        val dataContainer = rootTag.get(enchantType.namespacedKey, PersistentDataType.TAG_CONTAINER) ?: return false
        return dataContainer.get(NamespacedKey(ItemEnchantPlus.INSTANCE, name), PersistentDataType.INTEGER) == 1
    }

    fun hasEnchantItem(itemStack: ItemStack?): Boolean {
        if (itemStack.isEmpty()) return false
        val meta = itemStack!!.itemMeta ?: return false
        val enchantName = meta.persistentDataContainer.get(itemEnchantNameDataKey, PersistentDataType.STRING)
        return !enchantName.isNullOrBlank()
    }

    fun getItemEnchant(item: ItemStack): EnchantData? {
        val meta = item.itemMeta ?: return null
        val enchantName =
            meta.persistentDataContainer.get(itemEnchantNameDataKey, PersistentDataType.STRING) ?: return null
        val enchantType = ItemEnchantType.of(enchantName) ?: return null
        return getItemEnchant(enchantType, item)
    }

    fun getItemEnchant(enchantType: ItemEnchantType, item: ItemStack): EnchantData? {
        val dataContainer = tryGetEnchantItemData(enchantType, item) ?: return null

        val itemUID = dataContainer.get(itemUIDDataKey, PersistentDataType.LONG) ?: return null
        val ownerUID = dataContainer.get(ownerUIDDataKey, PersistentDataType.STRING)?.asUUID() ?: return null
        val ownerName = dataContainer.get(ownerNameDataKey, PersistentDataType.STRING) ?: return null
        val stage = dataContainer.get(stageKey, PersistentDataType.INTEGER) ?: return null
        val level = dataContainer.get(levelKey, PersistentDataType.INTEGER) ?: return null
        val exp = dataContainer.get(expKey, PersistentDataType.DOUBLE) ?: return null
        val skinName = dataContainer.get(skinKey, PersistentDataType.STRING) ?: return null

        val enchantSkin =
            StorageIEP.getPlayerCurrentEnchantSkin(ownerUID, skinName) ?: EnchantSkin(enchantType.defaultSkin(), 1)
        return EnchantData(enchantType, itemUID, ownerUID, ownerName, stage, level, exp, enchantSkin)
    }

    fun addExpToItem(enchantType: ItemEnchantType, item: ItemStack, value: Double) {
        trySetEnchantItemData(enchantType, item) {
            val stage = it.get(stageKey, PersistentDataType.INTEGER)!!
            val level = it.get(levelKey, PersistentDataType.INTEGER)!!
            val exp = it.get(expKey, PersistentDataType.DOUBLE)!!
            val data = ItemEnchantData(stage, level, exp)
            data.exp += value
            handleEnchantLevel(data)
            val maxExp = ConfigIEP.getLevelMaxExp(data.level)
            if (data.exp > maxExp) {
                data.exp = maxExp
            }
            it.set(levelKey, PersistentDataType.INTEGER, data.level)
            it.set(expKey, PersistentDataType.DOUBLE, data.exp)
        }
    }

    private fun handleEnchantLevel(data: ItemEnchantData) {
        if (data.stage >= 3) return
        var maxExp = ConfigIEP.getLevelMaxExp(data.level)
        val maxLevel = ConfigIEP.stageLevel[data.stage]
        while (data.exp >= maxExp && data.level < maxLevel) {
            data.exp -= maxExp
            data.level++
            maxExp = ConfigIEP.getLevelMaxExp(data.level)
        }
    }

    fun setItemSkin(enchantType: ItemEnchantType, item: ItemStack, skinConfig: EnchantSkinConfig) {
        trySetEnchantItemData(enchantType, item) {
            it.set(skinKey, PersistentDataType.STRING, skinConfig.name)
        }
    }

    fun getToNextStageNeedExp(data: EnchantData): Double {
        var rs = 0.0
        val eLevel = data.maxLevel - data.level
        repeat(eLevel) {
            rs += ConfigIEP.getLevelMaxExp(data.level + it)
        }

        rs -= data.exp
        return rs
    }

    fun updateItemMeta(item: ItemStack, player: Player? = null) {
        val enchantData = getItemEnchant(item)
        if (enchantData != null) {
            updateItemMeta(item, enchantData, player)
        }
    }

    fun updateItemMeta(item: ItemStack, data: EnchantData, player: Player? = null) {
        val meta = item.itemMeta ?: return
        val hasExtra = hasExtraData(
            data.enchantType,
            item,
            data.enchantType.plusExtraDataName()
        )
        if (!meta.hasDisplayName()) {
            //            val itemName = data.enchantType
            //                .let { if (hasExtra) it.plusDisplayName() else it.displayName() }
            val itemName = data.enchantSkin.skinConfig.skinEffect[0].menuItemName
            meta.displayName(
                itemName
                    .tvar("stage", stageText[data.stage])
                    .tvar("level", data.level.toString())
                    .deserialize()
            )
        } else {
            val displayName = meta.displayName()!!.serialize()
            if (ConfigIEP.itemSkins.values.any {
                    displayName == it.skinEffect[0].menuItemName
                }) {
                val itemName = data.enchantSkin.skinConfig.skinEffect[0].menuItemName
                meta.displayName(
                    itemName
                        .tvar("stage", stageText[data.stage])
                        .tvar("level", data.level.toString())
                        .deserialize()
                )
            }
        }
        val lore = data.enchantType.let { if (hasExtra) it.plusDescription() else it.description() }.map { line ->
            val progressList = "■■■■■■■■■■".toCharArray().map { it.toString() }.toMutableList()
            val curProgress =
                (BigDecimal(data.exp / data.maxExp).setScale(1, RoundingMode.FLOOR).toDouble() * 10).toInt()
            if (curProgress >= 1) {
                progressList.add(0, "&3")
            }
            progressList.add(curProgress + if (curProgress >= 1) 1 else 0, "&b")
            if (curProgress < 9) {
                progressList.add(curProgress + if (curProgress >= 1) 3 else 2, "&7")
            }
            line
                .tvar("stage", stageText[data.stage])
                .tvar("level", data.level.toString())
                .tvar("exp_progress", progressList.joinToString(""))
                .deserialize()
        } as List<Component>
        if (!meta.hasLore()) {
            meta.lore(lore.toMutableList().apply {
                add(0, enchantLoreSpliter.deserialize())
                add(enchantLoreSpliter.deserialize())
            })
        } else {
            val metaLore = meta.lore()!!
            val loreIndexFirst = metaLore.indexOfFirst { it.serialize() == enchantLoreSpliter }
            val loreIndexLast = metaLore.indexOfLast { it.serialize() == enchantLoreSpliter }
            if (loreIndexFirst == -1 || loreIndexLast == -1 || loreIndexFirst == loreIndexLast) {
                meta.lore(lore.toMutableList().apply {
                    add(0, enchantLoreSpliter.deserialize())
                    add(enchantLoreSpliter.deserialize())
                })
            } else {
                val topLore = metaLore.subList(0, loreIndexFirst + 1)
                val bottomLore = metaLore.subList(loreIndexLast, metaLore.size)
                meta.lore(lore.toMutableList().apply {
                    addAll(0, topLore)
                    addAll(bottomLore)
                })
            }
        }
        val skin = data.enchantSkin.skinConfig
        val skinEffect = skin.skinEffect[data.enchantSkin.stage - 1]
        meta.setCustomModelData(skinEffect.customModelData)
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)

        if (data.enchantType == ItemEnchantType.LAN_REN && data.stage > 0) {
            val config = data.enchantType.enchantConfig() as LanRenEnchantConfig
            val attackDamageModifier = AttributeModifier(
                "IEP_ADD_ad".asUUIDFromByte(),
                "IEP_ADD_ad",
                config.combo1_2.baseDamage[data.stage - 1],
                AttributeModifier.Operation.ADD_NUMBER
            )
            val attackSpeedModifier = AttributeModifier(
                "IEP_ADD_as".asUUIDFromByte(),
                "IEP_ADD_as",
                if (hasExtraData(
                        data.enchantType,
                        item,
                        data.enchantType.plusExtraDataName()
                    )
                ) {
                    config.plusAttackSpeedModifier
                } else config.attackSpeedModifier,
                AttributeModifier.Operation.ADD_NUMBER
            )
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, attackDamageModifier)
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, attackSpeedModifier)
            meta.addAttributeModifier(
                Attribute.GENERIC_ATTACK_DAMAGE,
                attackDamageModifier
            )
            meta.addAttributeModifier(
                Attribute.GENERIC_ATTACK_SPEED,
                attackSpeedModifier
            )
        }

        val conflictEnchants = data.enchantType.conflictEnchant().filter {
            meta.hasEnchant(it)
        }
        if (conflictEnchants.isNotEmpty() && player != null) {
            conflictEnchants.forEach {
                player.sendLang(
                    "updateEnchantDataConflictEnchant",
                    "{enchantName}", data.enchantType.displayName(),
                    "{enchant_text}", Component.translatable(it)
                )
            }
            conflictEnchants.forEach {
                meta.removeEnchant(it)
            }
        }

        item.itemMeta = meta
    }

    fun triggerItemSkinEffect(
        player: Player,
        data: EnchantData,
        type: EffectTriggerType
    ) {
        val enchantSkin = data.enchantSkin
        val skin = enchantSkin.skinConfig
        if (!skin.enable || !enchantSkin.hasOwn) return
        val skinEffect = skin.skinEffect.getOrNull(enchantSkin.stage) ?: return
        skinEffect.effects.filter { it.type == type }.forEach {
            it.value?.execute(player.location)
        }
    }

    private fun trySetEnchantItemData(
        itemStack: ItemStack,
        block: (PersistentDataContainer) -> Unit
    ) {
        val meta = itemStack.itemMeta ?: return
        val enchantName =
            meta.persistentDataContainer.get(itemEnchantNameDataKey, PersistentDataType.STRING) ?: return
        val enchantType = ItemEnchantType.of(enchantName) ?: return
        trySetEnchantItemData(enchantType, itemStack, block)
    }

    private fun trySetEnchantItemData(
        enchantType: ItemEnchantType,
        itemStack: ItemStack,
        block: (PersistentDataContainer) -> Unit
    ) {
        val meta = itemStack.itemMeta ?: return
        val rootTag = meta.persistentDataContainer
        val dataContainer =
            rootTag.get(enchantType.namespacedKey, PersistentDataType.TAG_CONTAINER) ?: return

        block.invoke(dataContainer)

        rootTag.set(enchantType.namespacedKey, PersistentDataType.TAG_CONTAINER, dataContainer)
        rootTag.set(itemEnchantNameDataKey, PersistentDataType.STRING, enchantType.name.lowercase())
        itemStack.itemMeta = meta
    }

    private fun tryGetEnchantItemData(
        itemStack: ItemStack
    ): PersistentDataContainer? {
        val meta = itemStack.itemMeta ?: return null
        val enchantName =
            meta.persistentDataContainer.get(itemEnchantNameDataKey, PersistentDataType.STRING) ?: return null
        val enchantType = ItemEnchantType.of(enchantName) ?: return null
        return tryGetEnchantItemData(enchantType, itemStack)
    }

    private fun tryGetEnchantItemData(
        enchantType: ItemEnchantType,
        itemStack: ItemStack
    ): PersistentDataContainer? {
        val meta = itemStack.itemMeta ?: return null

        return meta.persistentDataContainer.get(enchantType.namespacedKey, PersistentDataType.TAG_CONTAINER)
    }

    data class ItemEnchantData(
        val stage: Int,
        var level: Int,
        var exp: Double
    )

}

data class EnchantData(
    val enchantType: ItemEnchantType,
    val itemUID: Long,
    var ownerUID: UUID,
    var ownerName: String,
    var stage: Int,
    var level: Int,
    var exp: Double,
    val enchantSkin: EnchantSkin
) {

    val hasMaxLevel get() = level == maxLevel
    val hasMaxExp get() = exp == maxExp

    val maxLevel get() = ConfigIEP.getStageMaxLevel(stage)
    val maxExp get() = ConfigIEP.getLevelMaxExp(level)

}