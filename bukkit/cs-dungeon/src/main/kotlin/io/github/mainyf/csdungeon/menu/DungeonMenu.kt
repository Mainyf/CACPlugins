package io.github.mainyf.csdungeon.menu

import io.github.mainyf.csdungeon.CsDungeon
import io.github.mainyf.csdungeon.DungeonBattle
import io.github.mainyf.csdungeon.config.ConfigCSD
import io.github.mainyf.csdungeon.storage.DungeonStructure
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class DungeonMenu(val dungeon: DungeonStructure) : AbstractMenuHandler() {

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

        inv.setIcon(dungeonMenuConfig.level1Slot) {
            CsDungeon.INSTANCE.getBattles(dungeon) { DungeonBattle(dungeon, 1) }.apply {
                addPlayer(it)
            }.startBattle()
            player.closeInventory()
        }
        inv.setIcon(dungeonMenuConfig.level2Slot) {
            CsDungeon.INSTANCE.getBattles(dungeon) { DungeonBattle(dungeon, 2) }.apply {
                addPlayer(it)
            }.startBattle()
            player.closeInventory()
        }
        inv.setIcon(dungeonMenuConfig.level3Slot) {
            CsDungeon.INSTANCE.getBattles(dungeon) { DungeonBattle(dungeon, 3) }.apply {
                addPlayer(it)
            }.startBattle()
            player.closeInventory()
        }
    }

}