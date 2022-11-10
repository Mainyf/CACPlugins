//package io.github.mainyf.itemenchantplus
//
//import io.github.mainyf.itemenchantplus.config.ItemEnchant
//import io.github.mainyf.newmclib.exts.asUUID
//import io.github.mainyf.newmclib.exts.colored
//import net.kyori.adventure.text.Component
//import org.bukkit.NamespacedKey
//import org.bukkit.entity.Player
//import org.bukkit.inventory.ItemStack
//import org.bukkit.persistence.PersistentDataType
//import java.util.*
//import kotlin.math.round
//
//object EnchantManager {
//
//    // 凿石
//    val expandDataKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "expand")
//
//    // 福临
//    val luckDataKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "luck")
//
//    // 凌劲
//    val sharpDataKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "sharp")
//
//    // 精准
//    val powerDataKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "power")
//
//    private val itemSkillTypeDataKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "itemSkillType")
//
//    // 物品uid
//    private val itemUIDDataKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "itemUID")
//
//    // 主人UID
//    private val ownerUIDDataKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "ownerUID")
//
//    // 主人 Name
//    private val ownerNameDataKey = NamespacedKey(ItemEnchantPlus.INSTANCE, "ownerName")
//
//    private val stageText = arrayOf("", "I", "II", "III")
//
//    fun getSkillByName(name: String): NamespacedKey {
//        return when (name) {
//            "expand" -> expandDataKey
//            "luck" -> luckDataKey
//            "sharp" -> sharpDataKey
//            "power" -> powerDataKey
//            else -> throw RuntimeException("`$name` skill not exists")
//        }
//    }
//
//    fun initItemSkill(
//        player: Player,
//        datakey: NamespacedKey,
//        item: ItemStack
//    ) {
//        val meta = item.itemMeta!!
//
//        val dataContainer = meta.persistentDataContainer
//
//        val root = dataContainer.adapterContext.newPersistentDataContainer()
//
//        val itemUUID = UUID.randomUUID()
//        root.set(itemUIDDataKey, PersistentDataType.STRING, itemUUID.toString())
//        root.set(ownerUIDDataKey, PersistentDataType.STRING, player.uniqueId.toString())
//        root.set(ownerNameDataKey, PersistentDataType.STRING, player.name)
//        StorageManager.setLevelAndExp(
//            itemUUID,
//            0,
//            1,
//            0.0
//        )
//
//        dataContainer.set(
//            datakey,
//            PersistentDataType.TAG_CONTAINER,
//            root
//        )
//        dataContainer.set(
//            itemSkillTypeDataKey,
//            PersistentDataType.STRING,
//            datakey.key
//        )
//        item.itemMeta = meta
//    }
//
//    fun setItemSkillData(item: ItemStack, data: ItemSkillData) {
//        val meta = item.itemMeta ?: return
//        val skillType = meta.persistentDataContainer.get(itemSkillTypeDataKey, PersistentDataType.STRING) ?: return
//        setItemSkillData(getSkillByName(skillType), item, data)
//    }
//
//    fun setItemSkillData(dataKey: NamespacedKey, item: ItemStack, data: ItemSkillData) {
//        val meta = item.itemMeta ?: return
//        val rootTag = meta.persistentDataContainer
//        val dataContainer = rootTag.get(dataKey, PersistentDataType.TAG_CONTAINER) ?: return
//
//        dataContainer.set(itemUIDDataKey, PersistentDataType.STRING, data.itemUID.toString())
//        dataContainer.set(ownerUIDDataKey, PersistentDataType.STRING, data.ownerUID.toString())
//        dataContainer.set(ownerNameDataKey, PersistentDataType.STRING, data.ownerName)
//        StorageManager.setLevelAndExp(data.itemUID, data.stage, data.level, data.exp)
//
//        rootTag.set(dataKey, PersistentDataType.TAG_CONTAINER, dataContainer)
//        rootTag.set(itemSkillTypeDataKey, PersistentDataType.STRING, dataKey.key)
//
//        item.itemMeta = meta
//    }
//
//    fun hasSkillItem(itemStack: ItemStack?): Boolean {
//        if (itemStack.isEmpty()) return false
//        val meta = itemStack!!.itemMeta ?: return false
//        val skillType = meta.persistentDataContainer.get(itemSkillTypeDataKey, PersistentDataType.STRING)
//        return skillType != null && skillType.isNotBlank()
//    }
//
//    fun getItemSkill(item: ItemStack): ItemSkillData? {
//        val meta = item.itemMeta ?: return null
//        val skillType = meta.persistentDataContainer.get(itemSkillTypeDataKey, PersistentDataType.STRING) ?: return null
//        return getItemSkill(getSkillByName(skillType), item)
//    }
//
//    fun getItemSkill(dataKey: NamespacedKey, item: ItemStack): ItemSkillData? {
//        val meta = item.itemMeta ?: return null
//        val skillType = meta.persistentDataContainer.get(itemSkillTypeDataKey, PersistentDataType.STRING) ?: return null
//        val dataContainer = meta.persistentDataContainer.get(dataKey, PersistentDataType.TAG_CONTAINER) ?: return null
//        val uuidStr = dataContainer.get(itemUIDDataKey, PersistentDataType.STRING) ?: return null
//        val uuid = uuidStr.asUUID()
//        if (!StorageManager.exists(uuid)) {
//            return null
//        }
//        val ownerUID = dataContainer.get(ownerUIDDataKey, PersistentDataType.STRING) ?: return null
//        val ownerName = dataContainer.get(ownerNameDataKey, PersistentDataType.STRING) ?: return null
//        val data = StorageManager.getSkillData(uuid)
//        return ItemSkillData(skillType, uuid, ownerUID.asUUID(), ownerName, data.stage, data.level, data.exp)
//    }
//
//    fun addExpToItem(data: ItemSkillData, value: Double): ItemSkillData {
//        val d = StorageManager.addExp(data.itemUID, value)
//        data.stage = d.stage
//        data.level = d.level
//        data.exp = d.exp
//        return data
//    }
//
//    fun updateItemMeta(item: ItemStack, dataKey: NamespacedKey, data: ItemSkillData) {
//        val meta = item.itemMeta ?: return
//        meta.displayName(Component.text(ConfigManager.getItemDisplayNameByDataKey(dataKey)))
//        meta.lore(ConfigManager.getItemDescByDataKey(dataKey).map { line ->
//            val progressList = "■■■■■■■■■■".toCharArray().map { it.toString() }.toMutableList()
//            val curProgress = round((data.exp / data.maxExp) * 10.0).toInt()
//            if (curProgress <= 0) {
//                progressList.add(0, "&7")
//            } else {
//                progressList.add(0, "&3")
//                progressList.add(curProgress + 1, "&b")
//                progressList.add(curProgress + 2, "&7")
//            }
//            Component.text(
//                line
//                    .tvar("stage", stageText[data.stage])
//                    .tvar("level", data.level.toString())
//                    .tvar("exp_progress", progressList.joinToString(""))
//                    .colored()
//            )
//        })
//        val skin = ConfigManager.getSkillDefaultSkinByName(dataKey.key)
//        if (skin != null) {
//            val equipSkin = skin.equipments[data.stage]
//            meta.setCustomModelData(equipSkin.customModelData)
//        }
//        item.itemMeta = meta
//    }
//
//    fun triggerItemSkinEffect(
//        player: Player,
//        dataKey: NamespacedKey,
//        data: ItemSkillData,
//        type: EffectTriggerType
//    ) {
//        val skin = when (dataKey) {
//            expandDataKey -> ConfigManager.expandSkin
//            luckDataKey -> ConfigManager.luckSkin
//            sharpDataKey -> ConfigManager.sharpSkin
//            powerDataKey -> ConfigManager.powerSkin
//            else -> return
//        }
//        if (!skin.enabled) return
//        val equip = skin.equipments.getOrNull(data.stage) ?: return
//        equip.effect.values.filter { it.type == type }.forEach {
//            it.value.execute(player.location)
//        }
//    }
//
//}
//
//data class ItemSkillData(
//    val enchantType: ItemEnchant,
//    val itemUID: UUID,
//    var ownerUID: UUID,
//    var ownerName: String,
//    var stage: Int,
//    var level: Int,
//    var exp: Double
//) {
//
//    val hasMaxLevel get() = level == maxLevel
//    val hasMaxExp get() = exp == maxExp
//
//    val maxLevel get() = StorageManager.getStageMaxLevel(stage)
//    val maxExp get() = ConfigManager.getLevelMaxExp(level)
//
//}