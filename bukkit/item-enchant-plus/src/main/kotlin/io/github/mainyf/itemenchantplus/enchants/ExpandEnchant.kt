package io.github.mainyf.itemenchantplus.enchants

import io.github.mainyf.itemenchantplus.EnchantManager
import io.github.mainyf.itemenchantplus.ItemEnchantPlus
import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.itemenchantplus.config.EffectTriggerType
import io.github.mainyf.itemenchantplus.config.ItemEnchantType
import io.github.mainyf.newmclib.exts.isEmpty
import io.github.mainyf.newmclib.exts.onlinePlayers
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.submitTask
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

object ExpandEnchant : Listener {

    private val recursiveFixer = mutableSetOf<UUID>()
    private val playerHeldItem = mutableSetOf<UUID>()

    fun init() {
        ItemEnchantPlus.INSTANCE.submitTask(period = 2 * 20L) {
            if (!ConfigIEP.expandEnchantConfig.enable) return@submitTask
            onlinePlayers().forEach { p ->
                val item = p.inventory.itemInMainHand
                if (item.isEmpty()) return@forEach

                val data = EnchantManager.getItemEnchant(ItemEnchantType.EXPAND, item) ?: return@forEach
                if (data.stage >= 2) {
                    p.addPotionEffect(PotionEffect(PotionEffectType.FAST_DIGGING, 5 * 20, 1))
                    playerHeldItem.add(p.uniqueId)
                }
            }
        }
    }

    private fun tryRemovePlayerBuff(player: Player) {
        if (playerHeldItem.contains(player.uniqueId)) {
            player.removePotionEffect(PotionEffectType.FAST_DIGGING)
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onBreak(event: BlockBreakEvent) {
        if (!ConfigIEP.expandEnchantConfig.enable) return
        val item = event.player.inventory.itemInMainHand
        if (item.isEmpty()) return
        val data = EnchantManager.getItemEnchant(ItemEnchantType.EXPAND, item) ?: return
        val player = event.player

        val world = player.world
        val b = event.block
        val loc = player.location

        //        val exp = ConfigIEP.getBlockExp(event.block)
        //        if (exp > 0.0) {
        //            EnchantManager.addExpToItem(data, exp)
        //            //            player.msg("你破坏了 ${event.block.type.name} 获得经验 $exp, 阶段: ${data.stage} 等级: ${data.level} 当前经验: ${data.exp}/${data.maxExp}")
        //        }

        if (hasRecursive(player)) {
            return
        }
        var clockwiseBlockList = mutableListOf<Block>()
        val facing = player.facing
        //        player.msg(facing.toString())
        val hasBottom = loc.blockY > b.y
        val hasTop = loc.blockY + 1 < b.y
        val hasFront = loc.blockX < b.x
        val hasBack = loc.blockX > b.x
        val hasLeft = loc.blockZ > b.z
        val hasRight = loc.blockZ < b.z
        when {
            hasBottom || hasTop -> {
                val eastList = mutableListOf(
                    world.getBlockAt(b.x + 1, b.y, b.z - 1),
                    world.getBlockAt(b.x + 1, b.y, b.z),
                    world.getBlockAt(b.x + 1, b.y, b.z + 1),

                    world.getBlockAt(b.x, b.y, b.z + 1),
                    world.getBlockAt(b.x - 1, b.y, b.z + 1),

                    world.getBlockAt(b.x - 1, b.y, b.z),
                    world.getBlockAt(b.x - 1, b.y, b.z - 1),
                    world.getBlockAt(b.x, b.y, b.z - 1)
                )
                if (hasBottom) {
                    val index = arrayOf(
                        BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH
                    ).indexOf(player.facing)
                    if (index != -1) {
                        repeat(index) {
                            eastList.add(0, eastList.removeLast())
                            eastList.add(0, eastList.removeLast())
                        }
                        clockwiseBlockList = eastList
                    }
                }
                if (hasTop) {
                    eastList.reverse()
                    eastList.add(eastList.removeFirst())
                    val index = arrayOf(
                        BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH
                    ).indexOf(player.facing)
                    if (index != -1) {
                        repeat(index) {
                            eastList.add(eastList.removeFirst())
                            eastList.add(eastList.removeFirst())
                        }
                        clockwiseBlockList = eastList
                    }
                }
            }

            (facing == BlockFace.EAST || facing == BlockFace.WEST) && (hasFront || hasBack) -> {
                val eastList = mutableListOf(
                    world.getBlockAt(b.x, b.y + 1, b.z - 1),
                    world.getBlockAt(b.x, b.y + 1, b.z),
                    world.getBlockAt(b.x, b.y + 1, b.z + 1),

                    world.getBlockAt(b.x, b.y, b.z + 1),
                    world.getBlockAt(b.x, b.y - 1, b.z + 1),

                    world.getBlockAt(b.x, b.y - 1, b.z),
                    world.getBlockAt(b.x, b.y - 1, b.z - 1),
                    world.getBlockAt(b.x, b.y, b.z - 1)
                )
                if (facing == BlockFace.WEST) {
                    eastList.reverse()
                    val l = mutableListOf<Block>()
                    repeat(3) {
                        l.add(eastList.removeAt(5))
                    }
                    eastList.addAll(0, l)
                }
                clockwiseBlockList = eastList
            }

            (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) && hasLeft || hasRight -> {
                val northList = mutableListOf(
                    world.getBlockAt(b.x - 1, b.y + 1, b.z),
                    world.getBlockAt(b.x, b.y + 1, b.z),
                    world.getBlockAt(b.x + 1, b.y + 1, b.z),

                    world.getBlockAt(b.x + 1, b.y, b.z),
                    world.getBlockAt(b.x + 1, b.y - 1, b.z),

                    world.getBlockAt(b.x, b.y - 1, b.z),
                    world.getBlockAt(b.x - 1, b.y - 1, b.z),
                    world.getBlockAt(b.x - 1, b.y, b.z)
                )
                if (facing == BlockFace.SOUTH) {
                    northList.reverse()
                    val l = mutableListOf<Block>()
                    repeat(3) {
                        l.add(northList.removeAt(5))
                    }
                    northList.addAll(0, l)
                }
                clockwiseBlockList = northList
            }
        }
        val bList = clockwiseBlockList.filter { hasValid(it) }
        if (bList.isNotEmpty()) {
            if (data.stage == 1) {
                event.isCancelled = true
                markRecursive(player)
                playerBreakBlock(player, bList.first())
//                bList.first().breakNaturally()
            } else if (data.stage >= 3) {
                markRecursive(player)
                bList.forEach {
                    playerBreakBlock(player, it)
//                    it.breakNaturally()
                }
            }
        }

        //        EnchantManager.updateItemMeta(item, ItemEnchantType.EXPAND, data)
        EnchantManager.triggerItemSkinEffect(
            player, data, EffectTriggerType.BREAK_BLOCK
        )
        unMarkRecursive(player)
    }

    private fun playerBreakBlock(player: Player, block: Block) {
        val event = BlockBreakEvent(block, player)
        pluginManager().callEvent(event)
        if (!event.isCancelled) {
            block.breakNaturally()
        }
    }

    private fun markRecursive(player: Player) {
        if (!recursiveFixer.contains(player.uniqueId)) {
            recursiveFixer.add(player.uniqueId)
        }
    }

    private fun hasRecursive(player: Player): Boolean {
        return recursiveFixer.contains(player.uniqueId)
    }

    private fun unMarkRecursive(player: Player) {
        recursiveFixer.remove(player.uniqueId)
    }

    private fun hasValid(block: Block): Boolean {
        if (block.type == Material.AIR) {
            return false
        }
        if (block.isLiquid) {
            return false
        }
        if (!ConfigIEP.expandEnchantConfig.allowBlocks.any { it.equalsBlock(block) }) {
            return false
        }
        return true
    }

}