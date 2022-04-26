package io.github.mainyf.itemskillsplus.menu

import com.google.common.collect.HashBiMap
import io.github.mainyf.newmclib.exts.colored
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

abstract class AbstractMenuHandler {

    val SKILL_MAP = linkedMapOf(
        "凿石" to "expand",
        "福临" to "luck",
        "陵劲" to "sharp",
        "精准" to "power"
    )

    val SKILL_BI_MAP = HashBiMap.create(SKILL_MAP)

    private val iconBlockMap = mutableMapOf<Int, (Player) -> Unit>()

    abstract fun open(player: Player)

    fun triggerClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        if (iconBlockMap.containsKey(slot)) {
            event.isCancelled = true
            iconBlockMap[slot]!!.invoke(player)
        }
        onClick(slot, player, inv, event)
    }

    open fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {}

    open fun onClickPlayerInv(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        event.isCancelled = true
    }

    open fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {}

    fun Inventory.unSetIcon(slot: Int) {
        setItem(slot, null)
        iconBlockMap.remove(slot)
    }

    fun Inventory.unSetIcon(slots: Iterable<Int>) {
        slots.forEach {
            unSetIcon(it)
        }
    }

    fun Inventory.setIcon(
        slot: Int,
        iconType: Material,
        dName: String,
        lore: List<String> = listOf(),
        amount: Int = 1,
        clickBlock: (Player) -> Unit = {}
    ) {
        setItem(slot, ItemStack(
            iconType
        ).apply
        {
            this.amount = amount
            val meta = itemMeta
            if (meta != null) {
                meta.displayName(Component.text(dName.colored()))
                meta.lore(lore.map { Component.text(it.colored()) })
//                meta.setDisplayName(dName.colored())
//                meta.lore = lore
                meta.addItemFlags(*ItemFlag.values())
                itemMeta = meta
            }
        })
        iconBlockMap[slot] = clickBlock
    }

    fun Inventory.setIcon(
        slots: Iterable<Int>,
        iconType: Material,
        dName: String,
        lore: List<String> = listOf(),
        amount: Int = 1,
        clickBlock: (Player) -> Unit = {}
    ) {
        slots.forEach {
            setIcon(
                it,
                iconType, dName, lore, amount, clickBlock
            )
        }
    }

    fun Inventory.setIcon(
        slot: Int,
        itemStack: ItemStack,
        clickBlock: (Player) -> Unit = {}
    ) {
        setItem(slot, itemStack)
        iconBlockMap[slot] = clickBlock
    }

    fun Inventory.setIcon(
        slots: Iterable<Int>,
        itemStack: ItemStack,
        clickBlock: (Player) -> Unit = {}
    ) {
        slots.forEach {
            setIcon(
                it,
                itemStack,
                clickBlock
            )
        }
    }

}