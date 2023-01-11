package io.github.mainyf.toolsplugin.module

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.bungeesettingsbukkit.events.ServerPacketReceiveEvent
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.toolsplugin.ToolsPlugin
import io.github.mainyf.toolsplugin.config.ConfigTP
import io.github.mainyf.toolsplugin.util.TextUtils
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta

object CheckPlayerInventory : Listener {

    private lateinit var LOG: TextUtils

    fun init() {
        LOG = TextUtils.newText(ToolsPlugin.INSTANCE, "check-player-inventory")
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val inventory = player.inventory
        inventory.forEach { item ->
            if (item.isEmpty()) return@forEach
            val meta = item.itemMeta
            if (meta is BlockStateMeta) {
                val shulker = meta.blockState
                if (shulker is ShulkerBox) {
                    checkPlayerInv(player, shulker.inventory.contents.toList(), true)
                }
            }
        }
        checkPlayerInv(player, inventory.toList(), false)
    }

    private fun checkPlayerInv(player: Player, items: List<ItemStack?>, inShulkerBox: Boolean) {
        val itemList = ConfigTP.checkPlayerInventoryItems
        val checkItems = mutableListOf<Pair<ItemStack, Int>>()
        itemList.forEach { checkItem ->
            var amount = 0
            items.forEach {
                if (it != null && checkItem.item.equalsItem(it)) {
                    amount += it.amount
                }
            }
            if (amount >= checkItem.amount) {
                checkItems.add(checkItem.item.toItemStack() to amount)
            }
        }
        if (checkItems.isNotEmpty()) {
            val itemText = checkItems.joinToString(", ") { "${it.first.displayName().serialize()} - ${it.second}" }
            LOG.info(ConfigTP.checkPlayerInventoryLog.tvar(
                "player", player.name,
                "status", if (inShulkerBox) "在潜影盒中" else "在背包中",
                "itemText", itemText
            ))
            CrossServerManager.sendData(ToolsPlugin.OP_MSG) {
                writeString(
                    ConfigTP.checkPlayerInventoryInfo.tvar(
                        "player", player.name,
                        "status", if (inShulkerBox) "在潜影盒中" else "在背包中",
                        "itemText", itemText
                    )
                )
            }
        }
    }


    @EventHandler
    fun onPacket(event: ServerPacketReceiveEvent) {
        val buf = event.buf
        when (event.packet) {
            ToolsPlugin.OP_MSG -> {
                val msg = buf.readString()
                onlinePlayers().forEach {
                    if (it.isOp) {
                        it.msg(msg)
                    }
                }
            }
        }
    }

}