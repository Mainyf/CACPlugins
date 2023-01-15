package io.github.mainyf.itemenchantplus

import com.ticxo.modelengine.api.ModelEngineAPI
import dev.jorel.commandapi.arguments.DoubleArgument
import dev.jorel.commandapi.arguments.IntegerArgument
import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.itemenchantplus.config.ItemEnchantType
import io.github.mainyf.itemenchantplus.enchants.ExpandEnchant
import io.github.mainyf.itemenchantplus.enchants.LanRenEnchant
import io.github.mainyf.itemenchantplus.enchants.LuckEnchant
import io.github.mainyf.itemenchantplus.hook.MyIsLandHooks
import io.github.mainyf.itemenchantplus.listeners.PlayerListeners
import io.github.mainyf.itemenchantplus.menu.DashboardMenu
import io.github.mainyf.itemenchantplus.menu.EnchantSkinMenu
import io.github.mainyf.itemenchantplus.storage.StorageIEP
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.command.stringArguments
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.hooks.addPlaceholderExpansion
import io.github.mainyf.soulbind.RecallSBManager
import io.github.mainyf.soulbind.storage.StorageSB
import org.apache.commons.lang3.EnumUtils
import org.apache.logging.log4j.LogManager
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.max

class ItemEnchantPlus : JavaPlugin(), Listener {

    companion object {

        val LOGGER = LogManager.getLogger("ItemEnchantPlus")

        lateinit var INSTANCE: ItemEnchantPlus

    }

    override fun onEnable() {
        INSTANCE = this
        MyIsLandHooks.init()
        ConfigIEP.init()
        StorageIEP.init()
        ExpandEnchant.init()
        LuckEnchant.init()
        Bukkit.getServer().pluginManager.registerEvents(ExpandEnchant, this)
        Bukkit.getServer().pluginManager.registerEvents(LuckEnchant, this)
        Bukkit.getServer().pluginManager.registerEvents(LanRenEnchant, this)
        Bukkit.getServer().pluginManager.registerEvents(PlayerListeners, this)
        RecallSBManager.addRecallSBItemPredicate(this.name) { itemStack ->
            EnchantManager.hasEnchantItem(itemStack)
        }
        //        RecallSBManager.addRecallSBItemIDProvider(this.name) { itemStack ->
        //            val enchantData = EnchantManager.getItemEnchant(itemStack)
        //            enchantData?.itemUID ?: -1L
        //        }
        addPlaceholderExpansion("itemenchantplus") papi@{ offlinePlayer, params ->
            val player = offlinePlayer?.player ?: return@papi null
            val paramType = params?.substringBefore("_") ?: return@papi null
            when (paramType) {
                "extra" -> {
                    val typeText = params.substringAfter("_")
                    val type = ItemEnchantType.of(typeText) ?: return@papi null
                    val items = StorageSB.getPlayerRecallItems(player.uuid).values

                    if (items.any {
                            EnchantManager.hasExtraData(type, it, type.plusExtraDataName())
                        }) {
                        "on"
                    } else "off"
                }

                else -> null
            }
        }
        //        RecallSBManager.addRecallSBItemListProvider(this.name) { player ->
        //            player
        //        }
        apiCommand("itemEnchantPlus") {
            withAliases("iep")
            "reload" {
                executeOP {
                    ConfigIEP.init()
                    sender.successMsg("[ItemEnchantPlus] 重载成功")
                }
            }
            "rangeKill" {
                withArguments(playerArguments("玩家"), DoubleArgument("范围"))
                executeOP {
                    val player = player()
                    val range = double()
                    val entities = player.getNearbyEntities(range, range, range)
                    entities.forEach {
                        player.msg(it.type.toString())
                        val entity = ModelEngineAPI.getModeledEntity(it.uuid)
                        if (entity != null) {
                            kotlin.runCatching {
                                entity.destroy()
                            }.onFailure { e ->
                                e.printStackTrace()
                            }
                            (entity.base.original as? Entity)?.remove()
                        }
                    }
                }
            }
            "menu" {
                withArguments(playerArguments("玩家"))
                executeOP {
                    val player = player()
                    DashboardMenu().open(player)
                }
            }
            "skinmenu" {
                withArguments(playerArguments("玩家"))
                executeOP {
                    val player = player()

                    EnchantSkinMenu().open(player)
                }
            }
            "info" {
                withArguments(playerArguments("玩家"))
                executeOP {
                    val player = player()
                    val item = player.inventory.itemInMainHand
                    if (item.isEmpty()) return@executeOP
                    val data = EnchantManager.getItemEnchant(item)
                    if (data == null) {
                        player.msg("手上的物品没有数据")
                        return@executeOP
                    }
                    EnchantManager.updateItemMeta(item, data)
                    player.msg("物品uuid: ${data.itemUID}")
                    player.msg("主人uuid: ${data.ownerUID}")
                    player.msg("主人名字: ${data.ownerName}")
                    player.msg("阶段: ${data.stage}")
                    player.msg("等级: ${data.level}")
                    player.msg("经验: ${data.exp}/${data.maxExp}")
                }
            }
            "stage" {
                withArguments(playerArguments("玩家"), IntegerArgument("阶段"))
                executeOP {
                    val player = player()
                    val stage = int()
                    val item = player.inventory.itemInMainHand
                    if (item.isEmpty()) return@executeOP
                    val data = EnchantManager.getItemEnchant(item)
                    if (data == null) {
                        player.errorMsg("该物品无进阶附魔数据")
                        return@executeOP
                    }
                    data.stage = stage
                    val startLevels = listOf(1, *ConfigIEP.stageLevel)
                    if (stage < 3) {
                        data.level = startLevels[data.stage]
                        data.exp = 0.0
                    } else if (stage == 3) {
                        data.level = startLevels[data.stage]
                        data.exp = 0.0
                    }
                    EnchantManager.setItemEnchantData(item, data)
                    EnchantManager.updateItemMeta(item, data)
                    player.msg("&6设置成功")
                }
            }
            "level" {
                withArguments(playerArguments("玩家"), IntegerArgument("等级"))
                executeOP {
                    val player = player()
                    val level = max(1, int())
                    val item = player.inventory.itemInMainHand
                    if (item.isEmpty()) return@executeOP
                    val data = EnchantManager.getItemEnchant(item)
                    if (data == null) {
                        player.errorMsg("该物品无进阶附魔数据")
                        return@executeOP
                    }
                    data.level = level
                    EnchantManager.setItemEnchantData(item, data)
                    EnchantManager.updateItemMeta(item, data)
                    player.msg("&6设置成功")
                }
            }
            "addExp" {
                withArguments(playerArguments("玩家"), DoubleArgument("经验"))
                executeOP {
                    val player = player()
                    val exp = double()
                    val item = player.inventory.itemInMainHand
                    if (item.isEmpty()) return@executeOP
                    val data = EnchantManager.getItemEnchant(item)
                    if (data == null) {
                        player.errorMsg("该物品无进阶附魔数据")
                        return@executeOP
                    }
                    EnchantManager.addExpToItem(data.enchantType, item, exp)
                    EnchantManager.updateItemMeta(item, data)
                    player.msg("获得经验 $exp, 阶段: ${data.stage} 等级: ${data.level} 当前经验: ${data.exp}/${data.maxExp}")
                }
            }
            "needExp" {
                withArguments(playerArguments("玩家"))
                executeOP {
                    val player = player()
                    val item = player.inventory.itemInMainHand
                    if (item.isEmpty()) return@executeOP
                    val data = EnchantManager.getItemEnchant(item)
                    if (data == null) {
                        player.errorMsg("该物品无进阶附魔数据")
                        return@executeOP
                    }
                    val exp = EnchantManager.getToNextStageNeedExp(data)
                    sender.msg("升到满级需要: $exp, ${data.exp}/${data.maxExp}")
                }
            }
            "test" {
                withArguments(playerArguments("玩家"))
                executeOP {
                    val player = player()
                    //                    LanRenEnchant.launchBullet(player)
                }
            }
            "giveEnchant" {
                withArguments(
                    playerArguments("玩家"),
                    stringArguments("附灵类型") { _ -> ItemEnchantType.values().map { it.name }.toTypedArray() })
                executeOP {
                    val player = player()
                    val enchantType =
                        EnumUtils.getEnum(ItemEnchantType::class.java, text().uppercase()) ?: return@executeOP
                    val item = player.inventory.itemInMainHand
                    if (item.isEmpty()) return@executeOP
                    val data = EnchantManager.getItemEnchant(item)
                    if (data != null) {
                        player.errorMsg("该物品已经是一个附灵物品了")
                        return@executeOP
                    }
                    EnchantManager.initItemEnchant(player, enchantType, item)
                    EnchantManager.updateItemMeta(item, EnchantManager.getItemEnchant(item)!!)
                    player.msg("&6附灵成功")
                }
            }
            "giveExtra" {
                withArguments(
                    playerArguments("玩家"),
                    stringArguments("附灵类型") { _ -> ItemEnchantType.values().map { it.name }.toTypedArray() })
                executeOP {
                    val player = player()
                    val enchantType =
                        EnumUtils.getEnum(ItemEnchantType::class.java, text().uppercase()) ?: return@executeOP
                    val item = player.inventory.itemInMainHand
                    if (item.isEmpty()) return@executeOP
                    val data = EnchantManager.getItemEnchant(item)
                    if (data == null) {
                        player.errorMsg("该物品不是一个附灵物品")
                        return@executeOP
                    }
                    EnchantManager.setExtraDataToItem(enchantType, item, enchantType.plusExtraDataName())
                    EnchantManager.updateItemMeta(item, EnchantManager.getItemEnchant(item)!!)
                    player.msg("&6给与额外数据成功")
                }
            }
            "takeExtra" {
                withArguments(
                    playerArguments("玩家"),
                    stringArguments("附灵类型") { _ -> ItemEnchantType.values().map { it.name }.toTypedArray() })
                executeOP {
                    val player = player()
                    val enchantType =
                        EnumUtils.getEnum(ItemEnchantType::class.java, text().uppercase()) ?: return@executeOP
                    val item = player.inventory.itemInMainHand
                    if (item.isEmpty()) return@executeOP
                    val data = EnchantManager.getItemEnchant(item)
                    if (data == null) {
                        player.errorMsg("该物品不是一个附灵物品")
                        return@executeOP
                    }
                    EnchantManager.removeExtraDataToItem(enchantType, item, enchantType.plusExtraDataName())
                    EnchantManager.updateItemMeta(item, EnchantManager.getItemEnchant(item)!!)
                    player.msg("&6删除额外数据成功")
                }
            }
            "maxExp" {
                withArguments(playerArguments("玩家"))
                executeOP {
                    val player = player()
                    val item = player.inventory.itemInMainHand
                    if (item.isEmpty()) return@executeOP
                    val data = EnchantManager.getItemEnchant(item)
                    if (data == null) {
                        player.errorMsg("该物品无进阶附魔数据")
                        return@executeOP
                    }
                    val exp = EnchantManager.getToNextStageNeedExp(data)
                    EnchantManager.addExpToItem(data.enchantType, item, exp)
                    EnchantManager.updateItemMeta(item, data)
                    player.msg("&6升级成功")
                }
            }
            "addSkin" {
                withArguments(
                    playerArguments("玩家"),
                    stringArguments("皮肤名") { _ -> ConfigIEP.itemSkins.keys.toTypedArray() }
                )
                executeOP {
                    val player = player()
                    val skinName = text()
                    val skinConfig = ConfigIEP.getSkinByName(skinName)
                    if (skinConfig == null) {
                        sender.errorMsg("对玩家 ${player.name} 的添加皮肤操作失败，原因：没有叫${skinName}的皮肤，请检查配置文件skins.yml")
                        return@executeOP
                    }
                    StorageIEP.addEnchantSkinToPlayer(player.uuid, skinConfig)
                }
            }
            "removeSkin" {
                withArguments(
                    playerArguments("玩家"),
                    stringArguments("皮肤名") { _ -> ConfigIEP.itemSkins.keys.toTypedArray() }
                )
                executeOP {
                    val player = player()
                    val skinName = text()
                    val skinConfig = ConfigIEP.getSkinByName(skinName)
                    if (skinConfig == null) {
                        sender.errorMsg("对玩家 ${player.name} 的删除皮肤操作失败，原因：没有叫${skinName}的皮肤，请检查配置文件skins.yml")
                        return@executeOP
                    }
                    StorageIEP.removeEnchantSkinToPlayer(player.uuid, skinConfig)
                }
            }
            "addSkinTemp" {
                withArguments(
                    playerArguments("玩家"),
                    stringArguments("皮肤名") { _ -> ConfigIEP.itemSkins.keys.toTypedArray() },
                    IntegerArgument("阶段"),
                    IntegerArgument("生效时间(小时)")
                )
                executeOP {
                    val player = player()
                    val skinName = text()
                    val stage = int()
                    val hour = int()
                    val skinConfig = ConfigIEP.getSkinByName(skinName)
                    if (skinConfig == null) {
                        sender.errorMsg("对玩家 ${player.name} 的添加临时皮肤操作失败，原因：没有叫${skinName}的皮肤，请检查配置文件skins.yml")
                        return@executeOP
                    }
                    StorageIEP.addEnchantSkinTemporaryToPlayer(player.uuid, skinConfig, stage, hour)
                }
            }
            "removeSkinTemp" {
                withArguments(
                    playerArguments("玩家"),
                    stringArguments("皮肤名") { _ -> ConfigIEP.itemSkins.keys.toTypedArray() }
                )
                executeOP {
                    val player = player()
                    val skinName = text()
                    val skinConfig = ConfigIEP.getSkinByName(skinName)
                    if (skinConfig == null) {
                        sender.errorMsg("对玩家 ${player.name} 的删除临时皮肤操作失败，原因：没有叫${skinName}的皮肤，请检查配置文件skins.yml")
                        return@executeOP
                    }
                    StorageIEP.removeEnchantSkinTemporaryToPlayer(player.uuid, skinConfig)
                }
            }
            "skinList" {
                withArguments(
                    playerArguments("玩家")
                )
                executeOP {
                    val player = player()
                    val list = StorageIEP.getPlayerEnchantSkins(player.uuid)
                    if (list.isEmpty()) {
                        sender.msg("该玩家没有皮肤")
                        return@executeOP
                    }
                    list.forEach {
                        sender.msg("皮肤名: ${it.skinConfig.name}, 阶级: ${it.stage}, 过期时间: ${it.expiredTime?.formatYMDHM() ?: "永久"}")
                    }
                }
            }
        }
    }

    override fun onDisable() {
        onlinePlayers().forEach {
            it.closeInventory()
        }
        LanRenEnchant.clean()
    }

}

fun Block.getKey() = getKey(x, y, z)
fun getKey(x: Int, y: Int, z: Int) =
    x.toLong() and 0x7FFFFFF or (z.toLong() and 0x7FFFFFF shl 27) or (y.toLong() shl 54)
