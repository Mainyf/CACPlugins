package io.github.mainyf.soulbind

import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.offlinePlayerArguments
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.soulbind.config.ConfigSB
import io.github.mainyf.soulbind.listeners.PlayerListeners
import io.github.mainyf.soulbind.listeners.QsListeners
import io.github.mainyf.soulbind.menu.RecallItemMenu
import io.github.mainyf.soulbind.storage.StorageSB
import org.apache.logging.log4j.LogManager
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

class SoulBind : JavaPlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("SoulBind")

        lateinit var INSTANCE: SoulBind

    }

    override fun onEnable() {
        INSTANCE = this
        ConfigSB.load()
        StorageSB.init()
        pluginManager().registerEvents(PlayerListeners, this)
        pluginManager().registerEvents(QsListeners, this)
        apiCommand("soulbind") {
            withAliases("soulb", "sb")
            "reload" {
                executeOP {
                    ConfigSB.load()
                    sender.successMsg("[SoulBind] 重载成功")
                }
            }
            "bind" {
                withArguments(
                    playerArguments("玩家")
                )
                executeOP {
                    val player = player()
                    val itemStack = player.inventory.itemInMainHand
                    SBManager.bindItem(itemStack, player.uuid, player.name)
                    sender.msg("绑定成功")
                }
            }
            "bind-player" {
                withArguments(
                    playerArguments("玩家A"),
                    offlinePlayerArguments("玩家B")
                )
                executeOP {
                    val playerA = player()
                    val playerB = offlinePlayer()
                    val itemStack = playerA.inventory.itemInMainHand
                    SBManager.bindItem(itemStack, playerB.uuid, playerB.name)
                    sender.msg("绑定成功")
                }
            }
            "info" {
                withArguments(
                    playerArguments("玩家")
                )
                executeOP {
                    val player = player()
                    val itemStack = player.inventory.itemInMainHand
                    val itemData = SBManager.getBindItemData(itemStack)
                    if (itemData == null) {
                        sender.msg("这不是一个魂绑物品")
                        return@executeOP
                    }
                    sender.msg("魂绑UUID: ${itemData.ownerUUID}")
                    sender.msg("魂绑Name: ${itemData.ownerName}")
                    sender.msg("召回次数: ${itemData.recallCount}")
                }
            }
            "menu" {
                withArguments(
                    playerArguments("玩家")
                )
                executeOP {
                    val player = player()
                    RecallItemMenu().open(player)
                }
            }
            "test" {
                withArguments(
                    playerArguments("玩家")
                )
                executeOP {
                    val player = player()
                    val itemStack = player.inventory.itemInMainHand
                    val base64 = itemStack.toBase64()
                    player.giveItem(base64.toItemStack())
                    player.updateInventory()
                }
            }
        }
    }
}


fun ItemStack.toBase64(): String {
    val outputStream = ByteArrayOutputStream()
    BukkitObjectOutputStream(outputStream).use { it.writeObject(this) }
    return Base64.getEncoder().encodeToString(outputStream.toByteArray())
}

fun String.toItemStack(): ItemStack {
    val inputStream = ByteArrayInputStream(Base64.getDecoder().decode(this))
    val dataInput = BukkitObjectInputStream(inputStream)
    return dataInput.readObject() as ItemStack
}