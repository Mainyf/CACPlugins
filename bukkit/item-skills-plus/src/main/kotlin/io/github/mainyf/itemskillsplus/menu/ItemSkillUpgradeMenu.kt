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
            Component.text("请在背包中选择需要进阶的装备")
        )

        updateInv(player, inv)
        player.openInventory(inv)
    }

    private fun updateInv(player: Player, inv: Inventory) {
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
            inv.setIcon(14, Material.ENCHANTED_BOOK, "请在背包中选择需要赋予的装备")
        } else {
            val text = SKILL_BI_MAP.inverse()[currentSkill] ?: ""
            inv.setIcon(
                14,
                Material.ENCHANTED_BOOK,
                ConfigManager.upgradeMenuTipsName.tvar("skill", text),
                ConfigManager.upgradeMenuTipsLore.map { it.tvar("skill", text) })
        }
//        if (currentSkill.isBlank()) {
//            inv.setIcon(14, Material.ENCHANTED_BOOK, "请在背包中选择需要赋予的装备")
//        } else {
//            val type = ConfigManager.getSkillOnlyItemTypeByName(SKILL_MAP[currentSkill]!!)
//            inv.setIcon(14, type, "请点击背包相同类型的物品，会自动放入装备槽位")
//        }

        if (!equipItemStack.isEmpty()) {
            val materials = ConfigManager.getUpgradeMaterialByName(currentSkill)[currentStage]
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
                    inv.setIcon(
                        MATERIALS_STATUS_SLOT_LIST[index],
                        Material.GREEN_STAINED_GLASS_PANE,
                        "&aok"
                    )
                } else {
                    flag = false
                    inv.setIcon(
                        MATERIALS_STATUS_SLOT_LIST[index],
                        Material.RED_STAINED_GLASS_PANE,
                        "&cno"
                    )
                }

                inv.setIcon(MATERIAL_COUNT_SLOT_LIST[index], Material.TORCH, "x${amount}")
            }
            if (flag && equipItemStack != null) {
                inv.setIcon(49, Material.GREEN_STAINED_GLASS_PANE, "&a赋予") {
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
                inv.unSetIcon(49)
            }
        }

    }

    private fun tryUnEquip(player: Player, inv: Inventory) {
        if (!equipItemStack.isEmpty()) {
            inv.setItem(EQUIP_SLOT, null)
            player.giveItem(equipItemStack!!)
            equipItemStack = null
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
        val materials = ConfigManager.getUpgradeMaterialByName(currentSkill)[currentStage]
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

    override fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        println("[当前slot] $slot")
    }

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
        player.setOpenInventoryTitle("点击进阶附魔")
        updateInv(player, inv)
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        if (equipItemStack == null) return
        player.giveItem(equipItemStack!!)
//        player.world.dropItemNaturally(player.location, equipItemStack!!)
    }

}