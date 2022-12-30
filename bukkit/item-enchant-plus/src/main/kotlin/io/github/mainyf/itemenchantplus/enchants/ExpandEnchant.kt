package io.github.mainyf.itemenchantplus.enchants

import io.github.mainyf.itemenchantplus.EnchantManager
import io.github.mainyf.itemenchantplus.ItemEnchantPlus
import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.itemenchantplus.config.EffectTriggerType
import io.github.mainyf.itemenchantplus.config.ItemEnchantType
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.soulbind.SBManager
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.collections.any

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
        handleSkill(event.player, event.block, event)
    }

    private fun handleSkill(player: Player, block: Block, event: Cancellable) {
        if (!ConfigIEP.expandEnchantConfig.enable) return
        val item = player.inventory.itemInMainHand
        if (item.isEmpty()) return
        val bindData = SBManager.getBindItemData(item)
        if (!player.isOp && bindData != null && bindData.ownerUUID != player.uuid) {
            return
        }
        EnchantManager.updateItemMeta(item, player)
        val data = EnchantManager.getItemEnchant(ItemEnchantType.EXPAND, item) ?: return

        val world = player.world
        val loc = player.location

        if (hasRecursive(player)) {
            return
        }
        var clockwiseBlockList = mutableListOf<Block>()
        val facing = player.facing
        //        player.msg(facing.toString())
        val hasBottom = loc.blockY > block.y
        val hasTop = loc.blockY + 1 < block.y
        val hasFront = loc.blockX < block.x
        val hasBack = loc.blockX > block.x
        val hasLeft = loc.blockZ > block.z
        val hasRight = loc.blockZ < block.z
        when {
            hasBottom || hasTop -> {
                val eastList = mutableListOf(
                    world.getBlockAt(block.x + 1, block.y, block.z - 1),
                    world.getBlockAt(block.x + 1, block.y, block.z),
                    world.getBlockAt(block.x + 1, block.y, block.z + 1),

                    world.getBlockAt(block.x, block.y, block.z + 1),
                    world.getBlockAt(block.x - 1, block.y, block.z + 1),

                    world.getBlockAt(block.x - 1, block.y, block.z),
                    world.getBlockAt(block.x - 1, block.y, block.z - 1),
                    world.getBlockAt(block.x, block.y, block.z - 1)
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
                    world.getBlockAt(block.x, block.y + 1, block.z - 1),
                    world.getBlockAt(block.x, block.y + 1, block.z),
                    world.getBlockAt(block.x, block.y + 1, block.z + 1),

                    world.getBlockAt(block.x, block.y, block.z + 1),
                    world.getBlockAt(block.x, block.y - 1, block.z + 1),

                    world.getBlockAt(block.x, block.y - 1, block.z),
                    world.getBlockAt(block.x, block.y - 1, block.z - 1),
                    world.getBlockAt(block.x, block.y, block.z - 1)
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
                    world.getBlockAt(block.x - 1, block.y + 1, block.z),
                    world.getBlockAt(block.x, block.y + 1, block.z),
                    world.getBlockAt(block.x + 1, block.y + 1, block.z),

                    world.getBlockAt(block.x + 1, block.y, block.z),
                    world.getBlockAt(block.x + 1, block.y - 1, block.z),

                    world.getBlockAt(block.x, block.y - 1, block.z),
                    world.getBlockAt(block.x - 1, block.y - 1, block.z),
                    world.getBlockAt(block.x - 1, block.y, block.z)
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
            if (data.stage == 1 || data.stage == 2) {
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
            player.breakBlock(block)
//            block.breakNaturally()
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