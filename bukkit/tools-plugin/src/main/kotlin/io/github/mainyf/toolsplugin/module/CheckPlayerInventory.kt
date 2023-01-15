package io.github.mainyf.toolsplugin.module

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.bungeesettingsbukkit.events.ServerPacketReceiveEvent
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.toolsplugin.ToolsPlugin
import io.github.mainyf.toolsplugin.config.ConfigTP
import io.github.mainyf.toolsplugin.util.TextUtils
import org.bukkit.Location
import org.bukkit.block.Barrel
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
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
        checkPlayerInv(player, inventory, "")
    }

    @EventHandler
    fun onOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        val inv = event.inventory
        val holder = inv.holder
        when {
            holder is ShulkerBox -> {
                val loc = holder.location
                checkPlayerInv(
                    player,
                    inv,
                    "容器类型: 潜影盒, 方块位置: ${loc.toText()}, 玩家位置: ${player.location.toText()}"
                )
            }

            holder is Chest -> {
                val loc = holder.location
                checkPlayerInv(
                    player,
                    inv,
                    "容器类型: 箱子, 方块位置: ${loc.toText()}, 玩家位置: ${player.location.toText()}"
                )
            }

            holder is DoubleChest -> {
                val loc = holder.location
                checkPlayerInv(
                    player,
                    inv,
                    "容器类型: 大箱子, 方块位置: ${loc.toText()}, 玩家位置: ${player.location.toText()}"
                )
            }

            holder is Barrel -> {
                val loc = holder.location
                checkPlayerInv(
                    player,
                    inv,
                    "容器类型: 木桶, 方块位置: ${loc.toText()}, 玩家位置: ${player.location.toText()}"
                )
            }

            holder == null && inv.type == InventoryType.ENDER_CHEST -> {
                checkPlayerInv(player, inv, "容器类型: 末影箱, 方块位置: 无, 玩家位置: ${player.location.toText()}")
            }
        }
    }

    fun Location.toText(): String {
        return "${world?.name} $blockX $blockY $blockZ"
    }

    private fun checkPlayerInv(player: Player, inventory: Inventory, startText: String) {
        inventory.forEach { item ->
            if (item.isEmpty()) return@forEach
            val meta = item.itemMeta
            if (meta is BlockStateMeta) {
                val shulker = meta.blockState
                if (shulker is ShulkerBox) {
                    checkPlayerInv(player, shulker.inventory.contents.toList(), startText, true)
                }
            }
        }
        checkPlayerInv(player, inventory.toList(), startText, false)
    }

    private fun checkPlayerInv(player: Player, items: List<ItemStack?>, startText: String, inShulkerBox: Boolean) {
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
            LOG.info(listOf(
                "==========================",
                startText,
                ConfigTP.checkPlayerInventoryLog.tvar(
                    "player", player.name,
                    "status", if (inShulkerBox) "在潜影盒中" else "在容器中",
                    "itemText", itemText
                ),
                "==========================\n"
            ))
            CrossServerManager.sendData(ToolsPlugin.OP_MSG) {
                writeString(startText)
                writeString(
                    ConfigTP.checkPlayerInventoryInfo.tvar(
                        "player", player.name,
                        "status", if (inShulkerBox) "在潜影盒中" else "在容器中",
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
                val startText = buf.readString()
                val msg = buf.readString()
                onlinePlayers().forEach {
                    if (it.isOp) {
                        it.msg(startText)
                        it.msg(msg)
                    }
                }
            }
        }
    }

}