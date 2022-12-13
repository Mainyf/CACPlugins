package io.github.mainyf.itemenchantplus.storage

import io.github.mainyf.itemenchantplus.EnchantSkin
import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.itemenchantplus.config.EnchantSkinConfig
import io.github.mainyf.itemenchantplus.config.ItemEnchantType
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.joda.time.DateTime
import java.util.UUID
import kotlin.math.min

object StorageIEP : AbstractStorageManager() {

    val stageLevel = arrayOf(
        10,
        20,
        30
    )

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                ItemEnchantDatas,
                EnchantSkinDatas,
                EnchantSkinTemporaryDatas
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun setItemSkin(itemId: Long, skinConfig: EnchantSkinConfig) {
        transaction {
            val data = ItemEnchantData.findById(itemId)
            if (data != null) {
                data.skinName = skinConfig.name
            }
        }
    }

    fun setLevelAndExp(itemId: Long, enchantType: ItemEnchantType, stage: Int, level: Int, exp: Double) {
        transaction {
            val data = ItemEnchantData.findById(itemId)
            if (data == null) {
                handleSkillLevel(ItemEnchantData.new(itemId) {
                    this.stage = stage
                    this.level = level
                    this.exp = exp
                    this.skinName = enchantType.defaultSkin().name
                })
            } else {
                data.stage = stage
                data.level = level
                data.exp = exp
                handleSkillLevel(data)
            }
        }
    }

    fun addExp(itemId: Long, value: Double, enchantType: ItemEnchantType): ItemEnchantData {
        return transaction {
            val data = getEnchantData(itemId, enchantType)
            data.exp += value
            handleSkillLevel(data)
            val maxExp = ConfigIEP.getLevelMaxExp(data.level)
            if (data.exp > maxExp) {
                data.exp = maxExp
            }
            data
        }
    }

    fun getEnchantData(itemId: Long, enchantType: ItemEnchantType): ItemEnchantData {
        return transaction {
            var data = ItemEnchantData.findById(itemId)
            if (data == null) {
                data = ItemEnchantData.new(itemId) {
                    this.stage = 0
                    this.level = 1
                    this.exp = 0.0
                    this.skinName = enchantType.defaultSkin().name
                }
            }
            data
        }
    }

    fun exists(itemId: Long): Boolean {
        return transaction {
            ItemEnchantData.findById(itemId) != null
        }
    }

    fun getStageMaxLevel(stage: Int): Int {
        return stageLevel.getOrNull(stage) ?: 30
    }

    private fun handleSkillLevel(data: ItemEnchantData) {
        if (data.stage >= 3) return
        var maxExp = ConfigIEP.getLevelMaxExp(data.level)
        val maxLevel = stageLevel[data.stage]
        while (data.exp >= maxExp && data.level < maxLevel) {
            data.exp -= maxExp
            data.level++
            maxExp = ConfigIEP.getLevelMaxExp(data.level)
        }
    }

    fun nextItemLongID(): Long {
        return transaction {
            ItemEnchantData.count()
        }
    }

    fun getPlayerEnchantSkins(playerUID: UUID): List<EnchantSkin> {
        return transaction {
            val rs = mutableListOf<EnchantSkin>()
            EnchantSkinData.find { EnchantSkinDatas.playerUID eq playerUID }.forEach {
                val skinConfig = ConfigIEP.getSkinByName(it.skinName) ?: return@forEach
                rs.add(EnchantSkin(skinConfig, it.stage))
            }
            val expiredData = mutableListOf<EnchantSkinTemporaryData>()
            EnchantSkinTemporaryData.find { EnchantSkinTemporaryDatas.playerUID eq playerUID }.forEach {
                if (it.isExpired()) {
                    expiredData.add(it)
                    return@forEach
                }
                val skinConfig = ConfigIEP.getSkinByName(it.skinName) ?: return@forEach
                rs.add(EnchantSkin(skinConfig, it.stage, it.expiredTime))
            }
            expiredData.forEach {
                it.delete()
            }
            rs
        }
    }

    fun getAllEnchantSkin(playerUID: UUID, enchantType: ItemEnchantType): List<EnchantSkin> {
        val defaultSkin = enchantType.defaultSkin()
        val playerOwnSkins =
            getPlayerEnchantSkins(playerUID).filter { it.skinConfig.enchantType.contains(enchantType) }.toMutableList()
        if (!playerOwnSkins.any {
                it.skinConfig.name == defaultSkin.name
            }) {
            playerOwnSkins.add(
                EnchantSkin(
                    defaultSkin,
                    1,
                    null,
                    true
                )
            )
        }
        ConfigIEP.itemSkins.values.forEach { skinConfig ->
            if (!playerOwnSkins.any { it.skinConfig.name == skinConfig.name } && skinConfig.enchantType.contains(
                    enchantType
                )) {
                playerOwnSkins.add(
                    EnchantSkin(
                        skinConfig,
                        1,
                        null,
                        false
                    )
                )
            }
        }

        return playerOwnSkins
    }

    fun getEnchantSkin(playerUID: UUID, skinName: String): EnchantSkin? {
        return transaction {
            val skinData = EnchantSkinData
                .find { (EnchantSkinDatas.playerUID eq playerUID) and (EnchantSkinDatas.skinName eq skinName) }
                .firstOrNull()
            if (skinData != null) {
                val skinConfig = ConfigIEP.getSkinByName(skinData.skinName)
                if (skinConfig != null) {
                    return@transaction EnchantSkin(skinConfig, skinData.stage)
                }
            }
            val skinTemporaryData = EnchantSkinTemporaryData
                .find { (EnchantSkinTemporaryDatas.playerUID eq playerUID) and (EnchantSkinTemporaryDatas.skinName eq skinName) }
                .firstOrNull()
            if (skinTemporaryData != null) {
                val skinConfig = ConfigIEP.getSkinByName(skinTemporaryData.skinName)
                if (skinConfig != null) {
                    if (skinTemporaryData.isExpired()) {
                        skinTemporaryData.delete()
                        return@transaction null
                    }
                    return@transaction EnchantSkin(skinConfig, skinTemporaryData.stage, skinTemporaryData.expiredTime)
                }
            }
            return@transaction null
        }
    }

    fun hasEnchantSkin(playerUID: UUID, skinName: String): Boolean {
        return getEnchantSkin(playerUID, skinName) != null
    }

    fun addEnchantSkinToPlayer(playerUID: UUID, skinConfig: EnchantSkinConfig) {
        transaction {
            val enchantSkinData =
                EnchantSkinData.find { (EnchantSkinDatas.playerUID eq playerUID) eq (EnchantSkinDatas.skinName eq skinConfig.name) }
                    .firstOrNull()
            if (enchantSkinData == null) {
                EnchantSkinData.new(EnchantSkinData.count()) {
                    this.playerUID = playerUID
                    this.skinName = skinConfig.name
                    this.stage = 1
                }
            } else {
                enchantSkinData.stage = min(enchantSkinData.stage + 1, skinConfig.skinEffect.size)
            }
        }
    }

    fun removeEnchantSkinToPlayer(playerUID: UUID, skinConfig: EnchantSkinConfig) {
        transaction {
            val enchantSkinData =
                EnchantSkinData.find { (EnchantSkinDatas.playerUID eq playerUID) eq (EnchantSkinDatas.skinName eq skinConfig.name) }
                    .firstOrNull()
            enchantSkinData?.delete()
        }
    }

    fun addEnchantSkinTemporaryToPlayer(playerUID: UUID, skinConfig: EnchantSkinConfig, stage: Int, hour: Int) {
        transaction {
            val enchantSkinData =
                EnchantSkinTemporaryData.find { (EnchantSkinTemporaryDatas.playerUID eq playerUID) eq (EnchantSkinTemporaryDatas.skinName eq skinConfig.name) }
                    .firstOrNull()
            if (enchantSkinData == null) {
                EnchantSkinTemporaryData.new(EnchantSkinTemporaryData.count()) {
                    this.playerUID = playerUID
                    this.skinName = skinConfig.name
                    this.stage = stage
                    var now = DateTime.now()
                    val nowMinute = now.minuteOfHour
                    if (nowMinute > 0) {
                        now = now.plusHours(1)
                    }
                    this.expiredTime =
                        now.plusHours(hour)
                            .withMinuteOfHour(0)
                            .withSecondOfMinute(0)
                            .withMillisOfSecond(0)
                }
            } else {
                if (enchantSkinData.isExpired()) {
                    enchantSkinData.delete()
                    return@transaction
                }
                if (stage >= enchantSkinData.stage) {
                    enchantSkinData.stage = stage
                    enchantSkinData.expiredTime = enchantSkinData.expiredTime.plusHours(hour)
                }
            }
        }
    }

    fun removeEnchantSkinTemporaryToPlayer(playerUID: UUID, skinConfig: EnchantSkinConfig) {
        transaction {
            EnchantSkinTemporaryData.find { (EnchantSkinTemporaryDatas.playerUID eq playerUID) eq (EnchantSkinTemporaryDatas.skinName eq skinConfig.name) }
                .firstOrNull()?.delete()
        }
    }

    fun getPlayerCurrentEnchantSkin(playerUID: UUID, skinName: String): EnchantSkin? {
        val skinConfig = ConfigIEP.getSkinByName(skinName) ?: return null
        if (ItemEnchantType.values().any {
                skinConfig.name == it.defaultSkin().name
            }) {
            return EnchantSkin(skinConfig, 1, null, true)
        }
        return transaction {
            val skinData = EnchantSkinData
                .find { (EnchantSkinDatas.playerUID eq playerUID) and (EnchantSkinDatas.skinName eq skinName) }
                .firstOrNull()
            val skinTemporaryData = EnchantSkinTemporaryData
                .find { (EnchantSkinTemporaryDatas.playerUID eq playerUID) and (EnchantSkinTemporaryDatas.skinName eq skinName) }
                .firstOrNull()

            when {
                skinData != null && skinTemporaryData == null -> {
                    return@transaction EnchantSkin(skinConfig, skinData.stage)
                }

                skinData == null && skinTemporaryData != null -> {
                    if (skinTemporaryData.isExpired()) {
                        skinTemporaryData.delete()
                        return@transaction null
                    }
                    return@transaction EnchantSkin(skinConfig, skinTemporaryData.stage, skinTemporaryData.expiredTime)
                }

                skinData != null && skinTemporaryData != null -> {
                    if (skinTemporaryData.isExpired()) {
                        skinTemporaryData.delete()
                        return@transaction EnchantSkin(skinConfig, skinData.stage)
                    }
                    if (skinTemporaryData.stage > skinData.stage) {
                        return@transaction EnchantSkin(
                            skinConfig,
                            skinTemporaryData.stage,
                            skinTemporaryData.expiredTime
                        )
                    } else {
                        return@transaction EnchantSkin(skinConfig, skinData.stage)
                    }
                }

                else -> return@transaction null
            }
        }
    }

}
