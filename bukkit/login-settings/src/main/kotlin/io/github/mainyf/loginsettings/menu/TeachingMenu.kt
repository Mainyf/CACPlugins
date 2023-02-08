package io.github.mainyf.loginsettings.menu

import io.github.mainyf.loginsettings.LoginSettings
import io.github.mainyf.loginsettings.config.ConfigLS
import io.github.mainyf.loginsettings.module.BindQQs
import io.github.mainyf.loginsettings.storage.StorageLS
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.runTaskLaterBR
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
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
        setup(ConfigLS.teachingMenuConfig.settings)
        val slotAList = ConfigLS.teachingMenuConfig.slotA.slot
        slotAIndex = Random.nextInt(slotAList.size)
        slotA = slotAList.elementAt(slotAIndex)
        val slotBConfig = ConfigLS.teachingMenuConfig.slotB
        val slotBList = (slotBConfig.slotMin .. slotBConfig.slotMax).toList().toMutableList()
        if (slotBList.contains(slotA)) {
            slotBList.remove(slotA)
        }
        slotB = slotBList.random()

        val inv = createInv(player)

        updateInv(inv)

        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val teachingMenuConfig = ConfigLS.teachingMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.add(teachingMenuConfig.slotA.iaIcons[slotAIndex])
        icons.addAll(teachingMenuConfig.slotB.itemSlot.iaIcons.icons())
        return applyTitle(player, icons)
    }

    private fun updateInv(inv: Inventory) {
        val teachingMenuConfig = ConfigLS.teachingMenuConfig
        val slotAConfig = teachingMenuConfig.slotA
        inv.setIcon(slotA, slotAConfig.itemSlot.toItemStack(), leftClickBlock = {
            ok = true
            if (ConfigLS.qqEnable && !BindQQs.hasLinkQQ(it.uuid)) {
                it.closeInventory()
                BindQQs.startLinkQQ(it, true) {}
                return@setIcon
            }
            if (!StorageLS.hasAgreePlayRuleInWeek(it)) {
                ConfigLS.teachingMenuSlotA?.execute(it)
            } else {
                slotAConfig.itemSlot.execAction(it)
            }
        })
        val slotBConfig = teachingMenuConfig.slotB
        inv.setIcon(slotB, slotBConfig.itemSlot.toItemStack()) {
            ok = true
            slotBConfig.itemSlot.execAction(it)
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