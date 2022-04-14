package io.github.mainyf.itemskillsplus.menu

import io.github.mainyf.itemskillsplus.*
import io.github.mainyf.itemskillsplus.config.ConfigManager
import io.github.mainyf.newmclib.exts.pagination
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
            "请在在左侧选择附魔"
        )

        updateInv(player, inv)

        player.openInventory(inv)
    }

    private fun tryUnEquip(player: Player, inv: Inventory) {
        if (!equipItemStack.isEmpty()) {
            inv.setItem(EQUIP_SLOT, null)
            player.giveItem(equipItemStack!!)
            equipItemStack = null
        }
    }

    private fun updateInv(player: Player, inv: Inventory) {
        updateInvSkillList(inv)

        if (equipItemStack.isEmpty()) {
            inv.setIcon(EQUIP_SLOT, Material.RED_STAINED_GLASS_PANE, "装备槽位")
        } else {
            inv.setIcon(EQUIP_SLOT, equipItemStack!!) {
                tryUnEquip(it, inv)
                player.setOpenInventoryTitle("请在背包中选择需要赋予的装备")
                updateInv(player, inv)
            }
        }
        if (currentSkill.isBlank()) {
            inv.setIcon(15, Material.ENCHANTED_BOOK, "&c暂未选择附魔")
        } else {
            val type = ConfigManager.getSkillOnlyItemTypeByName(SKILL_MAP[currentSkill]!!)
            inv.setIcon(
                15,
                type,
                ConfigManager.initMenuTipsName.tvar("skill", currentSkill),
                ConfigManager.initMenuTipsLore.map { it.tvar("skill", currentSkill) })
        }

        if (currentSkill.isNotBlank()) {
            val materials = ConfigManager.getUpgradeMaterialByName(currentSkill)[0]
            for ((index, slot) in MATERIALS_STATUS_SLOT_LIST.withIndex()) {
                val (id, amount) = materials.getOrNull(index) ?: break

                val itemStack = ConfigManager.getItemByUPMaterialID(id)
                itemStack.amount = amount
                inv.setIcon(slot, itemStack)

            }
            var flag = true
            for ((index, slot) in MATERIALS_SLOT_LIST.withIndex()) {
                val (id, amount) = materials.getOrNull(index) ?: break

                val itemStack = ConfigManager.getItemByUPMaterialID(id)
//                itemStack.amount = amount
                inv.setIcon(slot, itemStack)

                if (hasMeetQuantity(player, id, amount)) {
                    inv.setIcon(MATERIALS_STATUS_SLOT_LIST[index], Material.GREEN_STAINED_GLASS_PANE, "&aok")
                } else {
                    flag = false
                    inv.setIcon(MATERIALS_STATUS_SLOT_LIST[index], Material.RED_STAINED_GLASS_PANE, "&cno")
                }

                inv.setIcon(MATERIAL_COUNT_SLOT_LIST[index], Material.TORCH, "x${amount}")
            }
            if (flag && equipItemStack != null) {
                inv.setIcon(50, Material.GREEN_STAINED_GLASS_PANE, "&a赋予") {
                    if (currentSkill.isBlank()) return@setIcon
                    val skillType = SKILL_MAP[currentSkill] ?: return@setIcon
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
                inv.unSetIcon(50)
            }
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
        SKILL_SLOT_LIST.forEach {
            inv.setItem(it, ItemStack(Material.AIR))
        }
        currentSkillList.clear()
        currentSkillList.addAll(SKILL_MAP.map { it.key }.pagination(pageIndex, SKILL_SLOT_LIST.size))
        currentSkillList.forEachIndexed { index, skillName ->
            inv.setIcon(SKILL_SLOT_LIST[index], Material.BOOK, skillName) {
                currentSkill = skillName
                val type = ConfigManager.getSkillOnlyItemTypeByName(SKILL_MAP[currentSkill]!!)
                if (equipItemStack?.type != type) {
                    tryUnEquip(it, inv)
                }
                it.setOpenInventoryTitle("请在背包中选择需要赋予的装备")
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
        val type = ConfigManager.getSkillOnlyItemTypeByName(SKILL_MAP[currentSkill]!!)
        if (item!!.type == type && !SkillManager.hasSkillItem(item)) {
            if (!equipItemStack.isEmpty()) {
                player.inventory.setItem(slot, equipItemStack)
                equipItemStack = item
            } else {
                equipItemStack = item
                player.inventory.setItem(slot, null)
            }
            player.setOpenInventoryTitle("点击赋予附魔")
            updateInv(player, inv)
        }
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        if (equipItemStack == null) return
        player.giveItem(equipItemStack!!)
//        player.world.dropItemNaturally(player.location, equipItemStack!!)
    }

}