package io.github.mainyf.itemskillsplus.menu

import io.github.mainyf.itemskillsplus.*
import io.github.mainyf.itemskillsplus.config.ConfigManager
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class ItemSkillUpgradeMenu : AbstractMenuHandler() {

    companion object {

        val MATERIALS_STATUS_SLOT_LIST = arrayOf(
            20,
            21,
            22,
            23,
            24
        )

        val MATERIALS_SLOT_LIST = arrayOf(
            29,
            30,
            31,
            32,
            33
        )

        // 32\64\128
        val MATERIAL_COUNT_SLOT_LIST = arrayOf(
            38,
            39,
            40,
            41,
            42
        )

        val EQUIP_SLOT = 12

    }

    private var currentSkill = ""
    private var currentStage: Int = 1
    private var equipItemStack: ItemStack? = null

    override fun open(player: Player) {
        val inv = Bukkit.createInventory(
            MenuHolder(
                player,
                this
            ),
            6 * 9,
            Component.text(ConfigManager.menuSlotConfig.upgradeItemSkillTitle1)
        )

        updateInv(player, inv)
        player.openInventory(inv)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val menuSlot = ConfigManager.menuSlotConfig
        val equipSlot = menuSlot.equipSlot
        if (equipItemStack.isEmpty()) {
            inv.setIcon(equipSlot.upgradeSlots, equipSlot.itemDisplay.toItemStack())
        } else {
            inv.setIcon(equipSlot.upgradeSlots, equipItemStack!!) {
                tryUnEquip(it, inv)
                player.setOpenInventoryTitle(ConfigManager.menuSlotConfig.upgradeItemSkillTitle1)
                updateInv(player, inv)
            }
        }
        if (equipItemStack.isEmpty()) {
            inv.setIcon(
                menuSlot.enchantSlot.upgradeSlots,
                menuSlot.enchantSlot.upgradeItemDisplay.toItemStack()
            )
        } else {
            val skill = menuSlot.enchantSlot.skills[currentSkill]!![0]
            inv.setIcon(
                menuSlot.enchantSlot.upgradeSlots,
                skill.toItemStack()
            )
        }

        if (!equipItemStack.isEmpty()) {
            val materials = ConfigManager.getUpgradeMaterialByName(currentSkill)[currentStage + 1]
//            for ((index, slot) in menuSlot.materialsSlot.upgradeSlots.withIndex()) {
//                val (id, amount) = materials.getOrNull(index) ?: break
//
//                val itemStack = ConfigManager.getItemByUPMaterialID(id)
////                itemStack.amount = amount
//                inv.setIcon(slot, itemStack)
//
//            }
            var flag = true
            for ((index, slot) in menuSlot.materialsSlot.upgradeSlots.withIndex()) {
                val (id, amount) = materials.getOrNull(index) ?: break

                val itemStack = ConfigManager.getItemByUPMaterialID(id)
//                itemStack.amount = amount
                inv.setIcon(slot, itemStack)

                val mof = menuSlot.materialsOfAdequacySlot
                if (hasMeetQuantity(player, id, amount)) {
                    inv.setIcon(mof.upgradeSlots[index], mof.satisfied.toItemStack())
                } else {
                    flag = false
                    inv.setIcon(mof.upgradeSlots[index], mof.unSatisfied.toItemStack())
                }

                inv.setIcon(
                    menuSlot.materialsCountSlot.upgradeSlots[index], (when (amount) {
                        32 -> menuSlot.materialsCountSlot.selectx32
                        64 -> menuSlot.materialsCountSlot.selectx64
                        128 -> menuSlot.materialsCountSlot.selectx128
                        192 -> menuSlot.materialsCountSlot.selectx192
                        else -> menuSlot.materialsCountSlot.selectx192
                    }).toItemStack()
                )
//                inv.setIcon(MATERIAL_COUNT_SLOT_LIST[index], Material.TORCH, "x${amount}")
            }
            if (flag && equipItemStack != null) {
                inv.setIcon(menuSlot.completeSlot.upgradeSlots, menuSlot.completeSlot.itemDisplay.toItemStack()) {
                    if (currentSkill.isBlank()) return@setIcon
                    val dataKey = SkillManager.getSkillByName(currentSkill)
                    val equipItem = equipItemStack ?: return@setIcon
                    val data = SkillManager.getItemSkill(equipItem) ?: return@setIcon
                    if (data.stage >= 3) return@setIcon
                    if (data.level < data.maxLevel) return@setIcon
                    if (!tryRemoveMaterial(it)) {
                        updateInv(it, inv)
                        return@setIcon
                    }
                    data.stage++
                    data.exp = 0.0
                    SkillManager.setItemSkillData(dataKey, equipItem, data)
                    SkillManager.updateItemMeta(equipItem, dataKey, data)
                    equipItem.itemMeta
                    equipItemStack = null
                    ResultViewMenu(equipItem).open(it)
                }
            } else {
                inv.unSetIcon(menuSlot.completeSlot.upgradeSlots)
            }
        }

    }

    private fun tryUnEquip(player: Player, inv: Inventory) {
        if (!equipItemStack.isEmpty()) {
            inv.unSetIcon(ConfigManager.menuSlotConfig.equipSlot.initSlots)
            player.giveItem(equipItemStack!!)
            equipItemStack = null
            currentSkill = ""
        }
    }

    private fun hasMeetQuantity(player: Player, id: String, amount: Int): Boolean {
        var _amount = amount
        for (itemStack in player.inventory) {
            if (itemStack.isEmpty()) continue
            if (!ConfigManager.hasMatchMaterial(id, itemStack)) continue
            _amount -= itemStack.amount
            if (_amount <= 0) {
                return true
            }
        }
        return _amount <= 0
    }

    private fun tryRemoveMaterial(player: Player): Boolean {
        val materials = ConfigManager.getUpgradeMaterialByName(currentSkill)[currentStage + 1]
        if (!materials.all { hasMeetQuantity(player, it.first, it.second) }) {
            return false
        }
        materials.forEach { (id, amount) ->
            var _amount = amount
            player.inventory.forEachIndexed { index, itemStack ->
                if (itemStack.isEmpty()) return@forEachIndexed
                if (!ConfigManager.hasMatchMaterial(id, itemStack)) return@forEachIndexed
                if (itemStack.amount <= _amount) {
                    _amount -= itemStack.amount
                    player.inventory.setItem(index, null)
                } else {
                    itemStack.amount -= _amount
                    player.inventory.setItem(index, itemStack)
                    _amount = 0
                }
                if (_amount <= 0) {
                    return@forEach
                }
            }
            if (_amount > 0) {
                return false
            }
        }
        player.updateInventory()
        return true
    }

//    override fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
//        println("[当前slot] $slot")
//    }

    override fun onClickPlayerInv(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        super.onClickPlayerInv(slot, player, inv, event)
        if (slot < 0) return
        val item = player.inventory.getItem(slot)
        if (item.isEmpty()) return
        val skillData = SkillManager.getItemSkill(item!!) ?: return
        currentSkill = skillData.skillType
        if (skillData.stage >= 3) return
        if (skillData.level < skillData.maxLevel) return
        currentStage = skillData.stage
        if (!equipItemStack.isEmpty()) {
            player.inventory.setItem(slot, equipItemStack)
            equipItemStack = item
        } else {
            equipItemStack = item
            player.inventory.setItem(slot, null)
        }
        player.setOpenInventoryTitle(ConfigManager.menuSlotConfig.upgradeItemSkillTitle2)
        updateInv(player, inv)
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        if (equipItemStack == null) return
        player.giveItem(equipItemStack!!)
//        player.world.dropItemNaturally(player.location, equipItemStack!!)
    }

}