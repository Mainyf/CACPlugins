package io.github.mainyf.itemenchantplus

import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.itemenchantplus.config.EffectTriggerType
import io.github.mainyf.itemenchantplus.config.ItemEnchantType
import io.github.mainyf.itemenchantplus.storage.StorageIEP
import io.github.mainyf.newmclib.exts.*
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
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

    private val stageText = arrayOf("", "I", "II", "III")

    private val enchantLoreSpliter = "§i§t§c§h§a§n§t"

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
        StorageIEP.setLevelAndExp(
            itemUID,
            enchantType,
            0,
            1,
            0.0
        )

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
        val meta = item.itemMeta ?: return
        val rootTag = meta.persistentDataContainer
        val dataContainer = rootTag.get(enchantType.namespacedKey, PersistentDataType.TAG_CONTAINER) ?: return

        dataContainer.set(itemUIDDataKey, PersistentDataType.LONG, data.itemUID)
        dataContainer.set(ownerUIDDataKey, PersistentDataType.STRING, data.ownerUID.toString())
        dataContainer.set(ownerNameDataKey, PersistentDataType.STRING, data.ownerName)
        StorageIEP.setLevelAndExp(data.itemUID, enchantType, data.stage, data.level, data.exp)

        rootTag.set(enchantType.namespacedKey, PersistentDataType.TAG_CONTAINER, dataContainer)
        rootTag.set(itemEnchantNameDataKey, PersistentDataType.STRING, enchantType.name.lowercase())

        item.itemMeta = meta
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
        val meta = item.itemMeta ?: return null
        //        val enchantName = meta.persistentDataContainer.get(itemEnchantNameDataKey, PersistentDataType.STRING) ?: return null
        //        val enchantType = ItemEnchantType.of(enchantName)
        val dataContainer =
            meta.persistentDataContainer.get(enchantType.namespacedKey, PersistentDataType.TAG_CONTAINER) ?: return null
        val itemUID = dataContainer.get(itemUIDDataKey, PersistentDataType.LONG) ?: return null
        if (!StorageIEP.exists(itemUID)) {
            return null
        }
        val ownerUID = dataContainer.get(ownerUIDDataKey, PersistentDataType.STRING)?.asUUID() ?: return null
        val ownerName = dataContainer.get(ownerNameDataKey, PersistentDataType.STRING) ?: return null
        val data = StorageIEP.getEnchantData(itemUID, enchantType)
        val enchantSkin =
            StorageIEP.getPlayerCurrentEnchantSkin(ownerUID, data.skinName) ?: EnchantSkin(enchantType.defaultSkin(), 1)
        return EnchantData(enchantType, itemUID, ownerUID, ownerName, data.stage, data.level, data.exp, enchantSkin)
    }

    fun addExpToItem(data: EnchantData, value: Double): EnchantData {
        val d = StorageIEP.addExp(data.itemUID, value, data.enchantType)
        data.stage = d.stage
        data.level = d.level
        data.exp = d.exp
        return data
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

    fun updateItemMeta(item: ItemStack) {
        updateItemMeta(item, getItemEnchant(item)!!)
    }

    fun updateItemMeta(item: ItemStack, data: EnchantData) {
        val meta = item.itemMeta ?: return
        if (!meta.hasDisplayName()) {
            meta.displayName(
                data.enchantType
                    .displayName()
                    .tvar("stage", stageText[data.stage])
                    .tvar("level", data.level.toString())
                    .deserialize()
            )
        }
        val lore = data.enchantType.description().map { line ->
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

    val maxLevel get() = StorageIEP.getStageMaxLevel(stage)
    val maxExp get() = ConfigIEP.getLevelMaxExp(level)

}