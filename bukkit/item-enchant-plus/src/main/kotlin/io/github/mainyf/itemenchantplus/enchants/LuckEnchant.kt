package io.github.mainyf.itemenchantplus.enchants

import io.github.mainyf.itemenchantplus.EnchantManager
import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.itemenchantplus.config.EffectTriggerType
import io.github.mainyf.itemenchantplus.config.ItemEnchantType
import io.github.mainyf.itemenchantplus.getKey
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.random.Percentage
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
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

    fun init() {

    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        if (!ConfigIEP.luckEnchantConfig.enable) return
        if (event.player.isOp) return
        blockKey.add(event.block.getKey())
    }

    @EventHandler(ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        if (!ConfigIEP.luckEnchantConfig.enable) return
        val item = event.player.inventory.itemInMainHand
        if (item.isEmpty()) return
        val data = EnchantManager.getItemEnchant(ItemEnchantType.LUCK, item) ?: return
        val player = event.player

        //        val exp = ConfigIEP.getBlockExp(event.block)
        //        if (exp > 0.0) {
        //            EnchantManager.addExpToItem(data, exp)
        //        }
        if (blockKey.contains(event.block.getKey())) {
            blockKey.remove(event.block.getKey())
            return
        }

        if (hasRecursive(player)) {
            return
        }
        when (data.stage) {
            1 -> {
                if (Percentage.hasHit(ConfigIEP.luckEnchantConfig.luckPercentage.stage1_2x)) {
                    breakBlock[event.block.getKey()] = BlockData(event.block.type, event.block.isLiquid, 2)
                }
            }

            2 -> {
                if (Percentage.hasHit(ConfigIEP.luckEnchantConfig.luckPercentage.stage2_2x)) {
                    breakBlock[event.block.getKey()] = BlockData(event.block.type, event.block.isLiquid, 2)
                }
            }

            3 -> {
                if (Percentage.hasHit(ConfigIEP.luckEnchantConfig.luckPercentage.stage3_2x)) {
                    breakBlock[event.block.getKey()] =
                        BlockData(
                            event.block.type,
                            event.block.isLiquid,
                            if (Percentage.hasHit(ConfigIEP.luckEnchantConfig.luckPercentage.stage3_3x)) 2 else 3
                        )
                }
            }
        }
        //        EnchantManager.updateItemMeta(item, data)
        EnchantManager.triggerItemSkinEffect(
            player,
            data,
            EffectTriggerType.BREAK_BLOCK
        )
        unMarkRecursive(player)
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