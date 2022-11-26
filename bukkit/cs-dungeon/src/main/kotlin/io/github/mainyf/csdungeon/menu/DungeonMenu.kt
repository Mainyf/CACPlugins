package io.github.mainyf.csdungeon.menu

import io.github.mainyf.csdungeon.CsDungeon
import io.github.mainyf.csdungeon.DungeonBattle
import io.github.mainyf.csdungeon.config.ConfigCSD
import io.github.mainyf.csdungeon.config.sendLang
import io.github.mainyf.csdungeon.storage.DungeonStructure
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.hooks.money
import io.github.mainyf.newmclib.hooks.takeMoney
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import kotlin.collections.any

class DungeonMenu(val dungeon: DungeonStructure) : AbstractMenuHandler() {

    val dungeonConfig get() = ConfigCSD.dungeonConfigMap[dungeon.dungeonName]!!

    override fun open(player: Player) {
        setup(ConfigCSD.dungeonMenuConfig.settings)
        val inv = createInv(player)

        player.openInventory(inv)
        updateInv(player, inv)
    }

    override fun updateTitle(player: Player): String {
        val menuConfig = ConfigCSD.dungeonMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(menuConfig.level1Slot.iaIcon())
        icons.addAll(menuConfig.level2Slot.iaIcon())
        icons.addAll(menuConfig.level3Slot.iaIcon())

        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val dungeonMenuConfig = ConfigCSD.dungeonMenuConfig

        inv.setIcon(dungeonMenuConfig.level1Slot, itemBlock = {
            setLevelItemDisplay(1)
        }) {
            startBattle(player, 1)
        }
        inv.setIcon(dungeonMenuConfig.level2Slot, itemBlock = {
            setLevelItemDisplay(2)
        }) {
            startBattle(player, 2)
        }
        inv.setIcon(dungeonMenuConfig.level3Slot, itemBlock = {
            setLevelItemDisplay(3)
        }) {
            startBattle(player, 3)
        }
    }

    private fun ItemStack.setLevelItemDisplay(level: Int) {
        withMeta(
            {
                dungeonConfig.menuItemInfo[level - 1].menuName.deserialize()
            },
            {
                dungeonConfig.menuItemInfo[level - 1].menuLore.mapToDeserialize()
            }
        )
    }

    private fun startBattle(player: Player, level: Int) {
        val dungeonMaterial = dungeonConfig.dungeonMaterials[level - 1]
        if (!dungeonMaterial.item.any { dmItem ->
                val count = player.countByItem { it?.equalsByIaNamespaceID(dmItem.iaName) == true }
                count >= dmItem.amount
            }) {
            player.sendLang(
                "dungeonItemDeficiency",
                "{item}",
                dungeonMaterial.item.joinToString(", ") { it.displayName })
            return
        }
        if (player.money() <= dungeonMaterial.money) {
            player.sendLang(
                "dungeonMoneyDeficiency",
                "{money}",
                dungeonMaterial.money
            )
            return
        }
        if (player.totalExperience <= dungeonMaterial.exp) {
            player.sendLang(
                "dungeonExpDeficiency",
                "{exp}",
                dungeonMaterial.exp
            )
            return
        }
        dungeonMaterial.item.forEach { dmItem ->
            player.takeItem(dmItem.amount) {
                it?.equalsByIaNamespaceID(dmItem.iaName) == true
            }
        }
        if (dungeonMaterial.money > 0.0) {
            player.takeMoney(dungeonMaterial.money)
        }
        if (dungeonMaterial.exp > 0) {
            player.totalExperience -= dungeonMaterial.exp
        }
        CsDungeon.INSTANCE.getBattles(dungeon) { DungeonBattle(dungeon, level) }.apply {
            addPlayer(player)
        }.startBattle()
        player.closeInventory()
    }

}