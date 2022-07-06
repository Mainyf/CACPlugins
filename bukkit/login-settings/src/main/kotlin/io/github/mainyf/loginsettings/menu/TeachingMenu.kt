package io.github.mainyf.loginsettings.menu

import io.github.mainyf.loginsettings.LoginSettings
import io.github.mainyf.loginsettings.config.ConfigManager
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.colored
import io.github.mainyf.newmclib.exts.runTaskLaterBR
import io.github.mainyf.newmclib.exts.setOpenInventoryTitle
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import kotlin.random.Random

class TeachingMenu : AbstractMenuHandler() {

    var ok = false

    private var slotA = 0
    private var slotAIndex = 0
    private var slotB = 0

    override fun open(player: Player) {
        this.cooldownTime = ConfigManager.teachingMenuConfig.cooldown
        val slotAList = ConfigManager.teachingMenuConfig.slotA.slot
        slotAIndex = Random.nextInt(slotAList.size)
        slotA = slotAList.elementAt(slotAIndex)
        val slotBConfig = ConfigManager.teachingMenuConfig.slotB
        val slotBList = (slotBConfig.slotMin..slotBConfig.slotMax).toList().toMutableList()
        if (slotBList.contains(slotA)) {
            slotBList.remove(slotA)
        }
        slotB = slotBList.random()

        val inv = Bukkit.createInventory(
            createHolder(player),
            ConfigManager.teachingMenuConfig.row * 9,
            Component.text(updateTitle(player).colored())
        )

        updateInv(inv)

        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val teachingMenuConfig = ConfigManager.teachingMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.add(teachingMenuConfig.slotA.iaIcons[slotAIndex])
        icons.addAll(teachingMenuConfig.slotB.itemDisplay!!.iaIcons.icons())
        val title = "${teachingMenuConfig.background} ${icons.sortedBy { it.priority }.joinToString(" ") { it.value }}"
        player.setOpenInventoryTitle(title)
        return title
    }

    private fun updateInv(inv: Inventory) {
        val teachingMenuConfig = ConfigManager.teachingMenuConfig
        val slotAConfig = teachingMenuConfig.slotA
        inv.setRightIcon(slotA, slotAConfig.itemDisplay!!.toItemStack()) {
            ok = true
            slotAConfig.action?.execute(it)
        }
        val slotBConfig = teachingMenuConfig.slotB
        inv.setIcon(slotB, slotBConfig.itemDisplay!!.toItemStack()) {
            ok = true
            slotBConfig.action?.execute(it)
        }
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        if (!ok) {
            LoginSettings.INSTANCE.runTaskLaterBR(5L) {
                TeachingMenu().open(player)
            }
        }
    }

}