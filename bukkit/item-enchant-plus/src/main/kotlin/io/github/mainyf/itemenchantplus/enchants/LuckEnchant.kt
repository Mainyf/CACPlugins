package io.github.mainyf.itemenchantplus.enchants

import co.aikar.timings.Timing
import co.aikar.timings.Timings
import io.github.mainyf.itemenchantplus.EnchantManager
import io.github.mainyf.itemenchantplus.ItemEnchantPlus
import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.itemenchantplus.config.EffectTriggerType
import io.github.mainyf.itemenchantplus.config.ItemEnchantType
import io.github.mainyf.itemenchantplus.getKey
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.random.Percentage
import io.github.mainyf.soulbind.SBManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.BlockPlaceEvent
import java.util.*

object LuckEnchant : Listener {

    private val recursiveFixer = mutableSetOf<UUID>()
    private val blockKey = mutableSetOf<Long>()
    private val breakBlock = mutableMapOf<Long, BlockData>()

    private lateinit var luckStage1Timings: Timing
    private lateinit var luckStage2Timings: Timing

    fun init() {
        luckStage1Timings = Timings.of(ItemEnchantPlus.INSTANCE, "luck stage 1")
        luckStage2Timings = Timings.of(ItemEnchantPlus.INSTANCE, "luck stage 2")
    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        if (!ConfigIEP.luckEnchantConfig.enable) return
        if (event.player.isOp) return
        blockKey.add(event.block.getKey())
    }

    @EventHandler(ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        handleSkill(event.player, event.block, event)
    }

    private fun handleSkill(player: Player, block: Block, event: Cancellable) {
        if (!ConfigIEP.luckEnchantConfig.enable) return
        val item = player.inventory.itemInMainHand
        if (item.isEmpty()) return
        luckStage1Timings.startTiming()
        val bindData = SBManager.getBindItemData(item)
        if (!player.isOp && bindData != null && bindData.ownerUUID != player.uuid) {
            return
        }
        EnchantManager.updateItemMeta(item, player)
        val data = EnchantManager.getItemEnchant(ItemEnchantType.LUCK, item) ?: return

        //        val exp = ConfigIEP.getBlockExp(event.block)
        //        if (exp > 0.0) {
        //            EnchantManager.addExpToItem(data, exp)
        //        }
        if (blockKey.contains(block.getKey())) {
            blockKey.remove(block.getKey())
            return
        }

        if (hasRecursive(player)) {
            return
        }
        val luckPercentage = ConfigIEP.luckEnchantConfig.luckPercentage
        when (data.stage) {
            1 -> {
                if (Percentage.hasHit(luckPercentage.stage1_2x)) {
                    breakBlock[block.getKey()] = BlockData(block.type, block.isLiquid, 2)
                }
            }

            2 -> {
                if (Percentage.hasHit(luckPercentage.stage2_2x)) {
                    breakBlock[block.getKey()] = BlockData(block.type, block.isLiquid, 2)
                }
            }

            3 -> {
                if (Percentage.hasHit(luckPercentage.stage3_2x)) {
                    breakBlock[block.getKey()] =
                        BlockData(
                            block.type,
                            block.isLiquid,
                            if (Percentage.hasHit(luckPercentage.stage3_3x)) 3 else 2
                        )
                }
            }
        }

        if (EnchantManager.hasExtraData(ItemEnchantType.LUCK, item, ItemEnchantType.LUCK.plusExtraDataName())) {
            markRecursive(player)
            getNearBlocks(block.location).forEach {
                breakBlock[it.getKey()] = BlockData(
                    it.type, it.isLiquid, when (data.stage) {
                        1 -> if (Percentage.hasHit(luckPercentage.stage1_2x)) 2 else 1
                        2 -> if (Percentage.hasHit(luckPercentage.stage2_2x)) 2 else 1
                        3 -> if (Percentage.hasHit(luckPercentage.stage3_2x)) {
                            if (Percentage.hasHit(luckPercentage.stage3_3x)) 3 else 2
                        } else 1

                        else -> 1
                    }
                )
                player.breakBlock(it)
            }
        }
        //        EnchantManager.updateItemMeta(item, data)
        EnchantManager.triggerItemSkinEffect(
            player,
            data,
            EffectTriggerType.BREAK_BLOCK
        )
        unMarkRecursive(player)
        luckStage1Timings.stopTiming()
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

    @EventHandler
    fun onDropItem(event: BlockDropItemEvent) {
        if (!ConfigIEP.luckEnchantConfig.enable) return
        luckStage2Timings.startTiming()
        val item = event.player.inventory.itemInMainHand
        if (item.isEmpty()) return
        if (EnchantManager.getItemEnchant(ItemEnchantType.LUCK, item) == null) return
        val key = event.block.getKey()
        if (breakBlock.containsKey(key)) {
            repeat(breakBlock[key]!!.amount - 1) {
                event.items.forEach {
                    event.block.world.dropItem(event.block.location, it.itemStack)
                }
            }
        }
        breakBlock.remove(key)
        luckStage2Timings.stopTiming()
    }

    private fun addBlockToList(
        x: Int,
        y: Int,
        z: Int,
        oKey: Long,
        world: World,
        list: MutableList<Block>,
        map: MutableMap<Long, Block>,
        max: Int
    ) {
        if (map.size < max) {
            val key = getKey(x, y, z)
            if (oKey != key && !map.containsKey(key)) {
                val block = world.getBlockAt(x, y, z)
                if (hasValid(block)) {
                    map[key] = block
                    list.add(block)
                }
            }
        }
    }

    private fun addConnectedBlockToMap(
        loc: Location,
        map: MutableMap<Long, Block>,
        max: Int
    ): List<Block> {
        val world = loc.world!!
        val blockX = loc.blockX
        val blockY = loc.blockY
        val blockZ = loc.blockZ

        val rs = mutableListOf<Block>()
        val oKey = getKey(blockX, blockY, blockZ)
        var x = blockX
        var y = blockY + 1
        var z = blockZ
        addBlockToList(x, y, z, oKey, world, rs, map, max)
        y = blockY - 1
        addBlockToList(x, y, z, oKey, world, rs, map, max)
        x = blockX + 1
        y = blockY
        addBlockToList(x, y, z, oKey, world, rs, map, max)
        x = blockX - 1
        addBlockToList(x, y, z, oKey, world, rs, map, max)
        x = blockX
        z = blockZ + 1
        addBlockToList(x, y, z, oKey, world, rs, map, max)
        z = blockZ - 1
        addBlockToList(x, y, z, oKey, world, rs, map, max)

        return rs
    }

    private fun getNearBlock(loc: Location): Block? {
        return addConnectedBlockToMap(
            loc,
            mutableMapOf(),
            ConfigIEP.luckEnchantConfig.max
        ).minByOrNull { if (it.y == loc.blockY) 0 else 1 }
    }

    //    private fun hasValid(data: BlockData?): Boolean {
    //        if (data == null) {
    //            return false
    //        }
    //        if (data.type == Material.AIR) {
    //            return false
    //        }
    //        if (data.isLiquid) {
    //            return false
    //        }
    //        if (!ConfigIEP.luckEnchantConfig.allowBlocks.any { it -> it.equalsBlock(data.type, data.customBlock) }) {
    //            return false
    //        }
    //        //        if (blockKey.contains(block.blockKey)) {
    //        //            return false
    //        //        }
    //        return true
    //    }

    private fun hasValid(block: Block?): Boolean {
        if (block == null) {
            return false
        }
        if (block.type == Material.AIR) {
            return false
        }
        if (block.isLiquid) {
            return false
        }
        if (!ConfigIEP.luckEnchantConfig.allowBlocks.any { it -> it.equalsBlock(block) }) {
            return false
        }
        //        if (blockKey.contains(block.blockKey)) {
        //            return false
        //        }
        return true
    }

    private fun getNearBlocks(loc: Location): List<Block> {
        val map = mutableMapOf<Long, Block>()
        val luckMax = ConfigIEP.luckEnchantConfig.max
        var list = addConnectedBlockToMap(loc, map, luckMax)
        if (list.isEmpty()) {
            return emptyList()
        } else {
            while (map.size < luckMax) {
                val l = mutableListOf<Block>()
                list.forEach {
                    l.addAll(addConnectedBlockToMap(it.location, map, luckMax))
                }
                if (l.isEmpty()) {
                    break
                } else {
                    list = l
                }
            }
        }
        return map.values.sortedByDescending { it.y }
    }

    data class BlockData(
        val type: Material,
        val isLiquid: Boolean,
        val amount: Int
    )

}