package io.github.mainyf.itemenchantplus.enchants

import io.github.mainyf.itemenchantplus.EnchantManager
import io.github.mainyf.itemenchantplus.ItemEnchantPlus
import io.github.mainyf.itemenchantplus.blockBreakRecursiveFixer
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

    private val playerHeldItem = mutableSetOf<UUID>()
    private val useStage3EnchantMap = mutableMapOf<UUID, Long>()
    private val usePlusEnchantMap = mutableMapOf<UUID, Long>()

    fun getUseStage3EnchantTime(uuid: UUID): Long {
        return useStage3EnchantMap[uuid] ?: -1L
    }

    fun getUsePlusEnchantTime(uuid: UUID): Long {
        return usePlusEnchantMap[uuid] ?: -1L
    }

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
        if (blockBreakRecursiveFixer.has(player)) {
            return
        }
        var t = currentTime()
        val bindData = SBManager.getBindItemData(item)
        if (!player.isOp && bindData != null && bindData.ownerUUID != player.uuid) {
            return
        }
        log("[expand] 获取附灵物品绑定数据", t)

        t = currentTime()
        EnchantManager.updateItemMeta(item, player)

        log("[expand] 更新附灵物品数据", t)

        t = currentTime()
        val data = EnchantManager.getItemEnchant(ItemEnchantType.EXPAND, item) ?: return
        log("[expand] 获取附灵物品数据", t)

        t = currentTime()
        val world = player.world
        val loc = player.location

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
        log("[expand] 计算周围方块", t)

        t = currentTime()
        val bList = clockwiseBlockList.filter { hasValid(it) }
        if (bList.isNotEmpty()) {
            if (data.stage == 1 || data.stage == 2) {
                event.isCancelled = true
                blockBreakRecursiveFixer.mark(player)
                playerBreakBlock(player, bList.first())
                //                bList.first().breakNaturally()
            } else if (data.stage >= 3) {
                blockBreakRecursiveFixer.mark(player)
                useStage3EnchantMap[player.uuid] = currentTime()
                bList.forEach {
                    playerBreakBlock(player, it)
                    //                    it.breakNaturally()
                }
            }
        }
        log("[expand] 破坏周围方块", t)

        t = currentTime()
        if (EnchantManager.hasExtraData(ItemEnchantType.EXPAND, item, ItemEnchantType.EXPAND.plusExtraDataName())) {
            usePlusEnchantMap[player.uuid] = currentTime()
        }

        //        EnchantManager.updateItemMeta(item, ItemEnchantType.EXPAND, data)
        EnchantManager.triggerItemSkinEffect(
            player, data, EffectTriggerType.BREAK_BLOCK
        )
        log("[expand] 触发皮肤效果", t)
        blockBreakRecursiveFixer.unMark(player)
    }

    private fun playerBreakBlock(player: Player, block: Block) {
        player.breakBlock(block)
        //        val event = BlockBreakEvent(block, player)
        //        pluginManager().callEvent(event)
        //        if (!event.isCancelled) {
        //            player.breakBlock(block)
        ////            block.breakNaturally()
        //        }
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

    private fun log(text: String, t: Long) {
        ItemEnchantPlus.iepLog.info("${text}: ${currentTime() - t}ms")
    }

}