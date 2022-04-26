package io.github.mainyf.itemskillsplus.menu

import io.github.mainyf.itemskillsplus.*
import io.github.mainyf.itemskillsplus.config.ConfigManager
import io.github.mainyf.newmclib.exts.pagination
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import kotlin.math.ceil

class InitItemSkillMenu : AbstractMenuHandler() {

    companion object {

        val SKILL_SLOT_LIST = arrayOf(
            0,
            9,
            18,
            27,
            36,
            45
        )

        val SCROLL_SLOT_LIST = arrayOf(
            1,
            10,
            19,
            28,
            37,
            46
        )

        val MATERIALS_STATUS_SLOT_LIST = arrayOf(
            21,
            22,
            23,
            24,
            25
        )

        val MATERIALS_SLOT_LIST = arrayOf(
            30,
            31,
            32,
            33,
            34
        )

        // 32\64\128
        val MATERIAL_COUNT_SLOT_LIST = arrayOf(
            39,
            40,
            41,
            42,
            43
        )

        val EQUIP_SLOT = 13

    }

    private var pageIndex = 1
    private val maxPageIndex = ceil(SKILL_MAP.size.toDouble() / SKILL_SLOT_LIST.size.toDouble()).toInt()
    private val currentSkillList = mutableListOf<String>()

    private var currentSkill = ""
    private var equipItemStack: ItemStack? = null

    override fun open(player: Player) {
        val inv = Bukkit.createInventory(
            MenuHolder(
                player,
                this
            ),
            6 * 9,
            Component.text(ConfigManager.menuSlotConfig.initItemSkillTitle1)
        )

        updateInv(player, inv)

        player.openInventory(inv)
    }

    private fun tryUnEquip(player: Player, inv: Inventory) {
        if (!equipItemStack.isEmpty()) {
            inv.unSetIcon(ConfigManager.menuSlotConfig.equipSlot.initSlots)
            player.giveItem(equipItemStack!!)
            equipItemStack = null
        }
    }

    private fun updateInv(player: Player, inv: Inventory) {
        updateInvSkillList(inv)

        val menuSlot = ConfigManager.menuSlotConfig
        if (equipItemStack.isEmpty()) {
            val equipSlot = menuSlot.equipSlot
            inv.setIcon(equipSlot.initSlots, equipSlot.itemDisplay.toItemStack())
        } else {
            inv.setIcon(menuSlot.equipSlot.initSlots, equipItemStack!!) {
                tryUnEquip(it, inv)
                player.setOpenInventoryTitle(ConfigManager.menuSlotConfig.initItemSkillTitle2)
                updateInv(player, inv)
            }
        }
        if (currentSkill.isBlank()) {
            inv.setIcon(menuSlot.enchantSlot.initSlots, menuSlot.enchantSlot.initItemDisplay.toItemStack())
        } else {
            val skill = menuSlot.enchantSlot.skills[currentSkill]!![0]
            inv.setIcon(menuSlot.enchantSlot.initSlots, skill.toItemStack())
        }

        if (currentSkill.isNotBlank()) {
            val materials = ConfigManager.getUpgradeMaterialByName(currentSkill)[0]
            var flag = true
            for ((index, slot) in menuSlot.materialsSlot.initSlots.withIndex()) {
                val (id, amount) = materials.getOrNull(index) ?: break

                val itemStack = ConfigManager.getItemByUPMaterialID(id)
//                itemStack.amount = amount
                inv.setIcon(slot, itemStack)

                val mof = menuSlot.materialsOfAdequacySlot
                if (hasMeetQuantity(player, id, amount)) {
                    inv.setIcon(mof.initSlots[index], mof.satisfied.toItemStack())
                } else {
                    flag = false
                    inv.setIcon(mof.initSlots[index], mof.unSatisfied.toItemStack())
                }

                inv.setIcon(
                    menuSlot.materialsCountSlot.initSlots[index], (when (amount) {
                        32 -> menuSlot.materialsCountSlot.selectx32
                        64 -> menuSlot.materialsCountSlot.selectx64
                        128 -> menuSlot.materialsCountSlot.selectx128
                        192 -> menuSlot.materialsCountSlot.selectx192
                        else -> menuSlot.materialsCountSlot.selectx192
                    }).toItemStack()
                )
            }
            if (flag && equipItemStack != null) {
                inv.setIcon(menuSlot.completeSlot.initSlots, menuSlot.completeSlot.itemDisplay.toItemStack()) {
                    if (currentSkill.isBlank()) return@setIcon
//                    val skillType = SKILL_MAP[] ?: return@setIcon
                    val skillType = currentSkill
                    val dataKey = SkillManager.getSkillByName(skillType)
                    val equipItem = equipItemStack ?: return@setIcon
                    if (!tryRemoveMaterial(it)) {
                        updateInv(it, inv)
                        return@setIcon
                    }
                    SkillManager.initItemSkill(player, dataKey, equipItem)
                    val data = SkillManager.getItemSkill(dataKey, equipItem)
                    SkillManager.updateItemMeta(equipItem, dataKey, data!!)
                    equipItemStack = null
                    ResultViewMenu(equipItem).open(it)
                }
            } else {
                inv.unSetIcon(menuSlot.completeSlot.initSlots)
            }
        } else {
            inv.setIcon(
                menuSlot.materialsOfAdequacySlot.initSlots,
                menuSlot.materialsOfAdequacySlot.default.toItemStack()
            )
            inv.unSetIcon(menuSlot.materialsSlot.initSlots)
            inv.setIcon(
                menuSlot.materialsCountSlot.initSlots,
                menuSlot.materialsCountSlot.default.toItemStack()
            )
        }

    }

    private fun tryRemoveMaterial(player: Player): Boolean {
        val materials = ConfigManager.getUpgradeMaterialByName(currentSkill)[0]
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

    private fun updateInvSkillList(inv: Inventory) {
        val menuSlot = ConfigManager.menuSlotConfig
        SKILL_SLOT_LIST.forEach {
            inv.setItem(it, ItemStack(Material.AIR))
        }
        currentSkillList.clear()
        currentSkillList.addAll(SKILL_MAP.map { it.value }.pagination(pageIndex, SKILL_SLOT_LIST.size))
        currentSkillList.forEachIndexed { index, skillName ->
            inv.setIcon(SKILL_SLOT_LIST[index], menuSlot.skills[skillName]!!.toItemStack()) {
                currentSkill = skillName
                val type = ConfigManager.getSkillOnlyItemTypeByName(currentSkill)
                if (equipItemStack?.type != type) {
                    tryUnEquip(it, inv)
                }
                it.setOpenInventoryTitle(ConfigManager.menuSlotConfig.initItemSkillTitle2)
                updateInv(it, inv)
            }
        }
        SCROLL_SLOT_LIST.forEachIndexed { index, slot ->
            if (index == pageIndex - 1) {
                inv.setIcon(slot, Material.DETECTOR_RAIL, "")
            } else {
                inv.setIcon(slot, Material.RAIL, "") {
                    if (index > maxPageIndex - 1) {
                        return@setIcon
                    }
                    pageIndex = index + 1
                    updateInvSkillList(inv)
                }
            }
        }
    }

    override fun onClickPlayerInv(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        super.onClickPlayerInv(slot, player, inv, event)
        if (slot < 0) return
        val item = player.inventory.getItem(slot)
        if (currentSkill.isBlank()) return
        if (item.isEmpty()) return
        val type = ConfigManager.getSkillOnlyItemTypeByName(currentSkill)
        if (item!!.type == type && !SkillManager.hasSkillItem(item)) {
            if (!equipItemStack.isEmpty()) {
                player.inventory.setItem(slot, equipItemStack)
                equipItemStack = item
            } else {
                equipItemStack = item
                player.inventory.setItem(slot, null)
            }
            player.setOpenInventoryTitle(ConfigManager.menuSlotConfig.initItemSkillTitle3)
            updateInv(player, inv)
        }
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        if (equipItemStack == null) return
        player.giveItem(equipItemStack!!)
//        player.world.dropItemNaturally(player.location, equipItemStack!!)
    }

}