package io.github.mainyf.questextension.menu

import com.guillaumevdn.questcreator.data.user.UserQC
import com.guillaumevdn.questcreator.lib.model.ElementModel
import com.guillaumevdn.questcreator.lib.quest.Quest
import com.guillaumevdn.questcreator.lib.quest.QuestEndType
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.questextension.config.ConfigManager
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.joda.time.DateTime

class QuestListMenu(
    val userQC: UserQC,
    val quests: List<Quest>,
    val dailyQuests: List<ElementModel>
) : AbstractMenuHandler() {

    override fun open(player: Player) {
        setup(ConfigManager.questListMenuConfig.settings)
        val inv = createInv(player)

        updateInv(inv)

        player.openInventory(inv)
    }

    private fun hasCompleteQuest(modelId: String): Boolean {
        var rs = false
        userQC.questHistory.iterateElements {
            val startedTime = DateTime(it.started).withTimeAtStartOfDay()
            val now = DateTime.now().withTimeAtStartOfDay()

            if (it.endType == QuestEndType.SUCCESS && modelId == it.modelId && startedTime == now) {
                rs = true
            }
        }
        return rs
    }

    override fun updateTitle(player: Player): String {
        val questListMenuConfig = ConfigManager.questListMenuConfig
        val icons = mutableListOf<IaIcon>()
        questListMenuConfig.questSlotList.forEachIndexed { index, slotConfig ->
            val model = dailyQuests.getOrNull(index) ?: return@forEachIndexed
//            val quest = quests.find { it.modelId == model.id }
            if (hasCompleteQuest(model.id)) {
                icons.addAll(slotConfig.iaIcon("complete"))
            } else {
                icons.addAll(slotConfig.iaIcon())
            }
        }
        icons.addAll(questListMenuConfig.rewardSlot.iaIcon())
        icons.addAll(questListMenuConfig.backSlot.iaIcon())
        return applyTitle(player, icons)
    }

    private fun updateInv(inv: Inventory) {
        val questListMenuConfig = ConfigManager.questListMenuConfig

        questListMenuConfig.questSlotList.forEachIndexed { index, slotConfig ->
            val model = dailyQuests.getOrNull(index) ?: return@forEachIndexed
//            val quest = quests.find { it.modelId == model.id }
//            val quest = quests.getOrNull(index)
            inv.setIcon(slotConfig, if (hasCompleteQuest(model.id)) "complete" else "default", itemBlock = {
                withMeta(
                    {
                        it?.serialize()?.tvar("displayName", model.displayName.getRawValueLine(0))?.deserialize()
                    },
                    { lore ->
                        if (lore == null || lore.isEmpty()) return@withMeta lore
                        lore.mapToSerialize().tvarList("desc", model.description.rawValue).mapToDeserialize()
                    }
                )
            }) {
                val quest = quests.find { quest -> quest.modelId == model.id } ?: return@setIcon
                QuestDetailMenu(userQC, quest).open(it)
            }
        }
        inv.setIcon(questListMenuConfig.rewardSlot, itemBlock = {
            setPlaceholder(userQC.player)
        })
        inv.setIcon(questListMenuConfig.backSlot, itemBlock = {
            setPlaceholder(userQC.player)
        })
    }

    override fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
//        println(slot)
    }

}