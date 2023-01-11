package io.github.mainyf.questextension.menu

import com.guillaumevdn.gcore.lib.element.type.basic.ElementCurrency
import com.guillaumevdn.gcore.lib.element.type.basic.ElementString
import com.guillaumevdn.gcore.lib.element.type.basic.ElementStringList
import com.guillaumevdn.questcreator.data.user.UserQC
import com.guillaumevdn.questcreator.lib.`object`.element.ElementQuestObjectType
import com.guillaumevdn.questcreator.lib.quest.Quest
import com.guillaumevdn.questcreator.lib.quest.QuestEndType
import io.github.mainyf.newmclib.config.DefaultSlotConfig
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.questextension.QuestManager
import io.github.mainyf.questextension.config.ConfigQE
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory

class QuestDetailMenu(
    val userQC: UserQC,
    val quest: Quest
) : AbstractMenuHandler() {

    companion object {

        val QUEST_OBJECT_IDS = arrayOf("a", "b", "c")

        fun asIndex(id: String): Int {
            return QUEST_OBJECT_IDS.indexOf(id)
        }

        fun asObjectName(index: Int): String {
            return QUEST_OBJECT_IDS[index]
        }

        val TOKEN_1_COMMAND_KEY = "customeconomy give {player} token_1 "

    }

    val questModel = quest.model

    val questBranch = quest.getFirstBranch(true, true)

    val branchModel = questModel.branches.getActualValue("MAIN").orNull()

    val hasTwoObject = branchModel.objects.size() == 2

    override fun open(player: Player) {
        if (hasTwoObject) {
            setup(ConfigQE.questDetailMenuConfig.settings)
        } else {
            setup(ConfigQE.questDetailMenuConfig.settings.copy(background = ConfigQE.questDetail3XMenuConfig.background))
        }
        val inv = createInv(player)

        updateInv(inv)

        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val questDetailMenuConfig = ConfigQE.questDetailMenuConfig
        val questDetail2XMenuConfig = ConfigQE.questDetail2XMenuConfig
        val questDetail3XMenuConfig = ConfigQE.questDetail3XMenuConfig
        val icons = mutableListOf<IaIcon>()

        val stepSlots = if (hasTwoObject) arrayOf(
            questDetail2XMenuConfig.step1Slot,
            questDetail2XMenuConfig.step2Slot
        ) else arrayOf(
            questDetail3XMenuConfig.step1Slot,
            questDetail3XMenuConfig.step2Slot,
            questDetail3XMenuConfig.step3Slot
        )
        stepSlots.forEachIndexed { index, slotConfig ->
            if(hasCompleteQuestBranch(index)) {
                icons.addAll(slotConfig.iaIcon("complete"))
            } else {
                icons.addAll(slotConfig.iaIcon())
            }
        }

        icons.addAll(questDetailMenuConfig.fsSlot.iaIcon())
        icons.addAll(questDetailMenuConfig.xypSlot.iaIcon())
        icons.addAll(questDetailMenuConfig.moneySlot.iaIcon())
        icons.addAll(questDetailMenuConfig.rewardSlot.iaIcon())
        return applyTitle(player, icons)
    }

    private fun updateInv(inv: Inventory) {
        val questDetailMenuConfig = ConfigQE.questDetailMenuConfig
        val questDetail2XMenuConfig = ConfigQE.questDetail2XMenuConfig
        val questDetail3XMenuConfig = ConfigQE.questDetail3XMenuConfig
        if (hasTwoObject) {
            inv.setQuestStepIcon(questDetail2XMenuConfig.step1Slot, 0)
            inv.setQuestStepIcon(questDetail2XMenuConfig.step2Slot, 1)
        } else {
            inv.setQuestStepIcon(questDetail3XMenuConfig.step1Slot, 0)
            inv.setQuestStepIcon(questDetail3XMenuConfig.step2Slot, 1)
            inv.setQuestStepIcon(questDetail3XMenuConfig.step3Slot, 2)
        }
        val playerPoints = getLogicMoney("PLAYER_POINTS")
        if (playerPoints != null) {
            inv.setIcon(questDetailMenuConfig.fsSlot, itemBlock = {
                setDisplayName {
                    it?.serialize()?.tvar("count", playerPoints)?.deserialize()
                }
            })
        }
        val money = getLogicMoney("VAULT")
        if (money != null) {
            inv.setIcon(questDetailMenuConfig.moneySlot, itemBlock = {
                setDisplayName {
                    it?.serialize()?.tvar("count", money)?.deserialize()
                }
            })
        }
        val token1 = getXYP()
        if (token1 != null) {
            inv.setIcon(questDetailMenuConfig.xypSlot, itemBlock = {
                setDisplayName {
                    it?.serialize()?.tvar("count", token1)?.deserialize()
                }
            })
        }
        inv.setIcon(questDetailMenuConfig.rewardSlot)
        inv.setIcon(questDetailMenuConfig.backSlot) {
            QuestManager.openQuestListMenu(it)
        }
    }

    private fun getXYP(): Double? {
        val endObject = questModel.endObjects.getValue(QuestEndType.SUCCESS)
        val endObjectElements = endObject.orNull().elements
        endObjectElements.map { it.b }.forEach {
            val type = it.actualValue.getElement("type").orNull() as ElementQuestObjectType
            val typeValue = type.getRawValueLine(0)
            if (typeValue != "SERVER_COMMANDS_PERFORM") {
                return@forEach
            }
            val commands = it.actualValue.getElement("commands").orNull() as ElementStringList
            // customeconomy give {player} token_1 1
            val commandsLine = commands.rawValue
            commandsLine.forEach { command ->
                if (command.startsWith(TOKEN_1_COMMAND_KEY)) {
                    return command.replace(TOKEN_1_COMMAND_KEY, "").toDouble()
                }
            }
        }
        return null
    }

    private fun getLogicMoney(key: String): Double? {
        val endObject = questModel.endObjects.getValue(QuestEndType.SUCCESS)
        val endObjectElements = endObject.orNull().elements
        endObjectElements.map { it.b }.forEach {
            val type = it.actualValue.getElement("type").orNull() as ElementQuestObjectType
            val typeValue = type.getRawValueLine(0)
            if (typeValue != "SERVER_LOGIC_MONEY") {
                return@forEach
            }
            val currency = it.actualValue.getElement("currency").orNull() as ElementCurrency
            val currencyValue = currency.getRawValueLine(0)
            if (currencyValue == key) {
                val valueFormula = it.actualValue.getElement("value_formula").orNull() as ElementString
                val valueText = valueFormula.getRawValueLine(0)
                return valueText.split("+")[1].trim().toDouble()
            }
        }
        return null
    }

    fun Inventory.setQuestStepIcon(slotConfig: DefaultSlotConfig, stepIndex: Int) {
        val questObject = branchModel.objects.getActualElementValue(asObjectName(stepIndex)).orNull()
        val proText = if (questBranch.objectId == questObject.id) {
            val pro = questBranch.getObjectProgression(questObject.id)
            "${pro.currentTotal.toInt()}/${pro.goalTotal.toInt()}"
        } else {
            if (quest.path.contains(questBranch.branchId, questObject.id, null)) "已完成" else "未开启"
        }
        setIcon(slotConfig, if (hasCompleteQuestBranch(stepIndex)) "complete" else "default", itemBlock = {
            withMeta(
                loreBlock = { lore ->
                    if (lore == null || lore.isEmpty()) return@withMeta lore
                    lore.mapToSerialize()
                        .tvarList("desc", questObject.objectiveDetail.rawValue)
                        .map { it.tvar("progression", proText) }
                        .map {
                            if (questBranch.objectId == questObject.id) {
                                val pro = questBranch.getObjectProgression(questObject.id)
                                it
                                    .tvar("objective_progression", pro.currentTotal.toDisplayText())
                                    .tvar("objective_goal", pro.goalTotal.toDisplayText())
                            } else it
                        }
                        .mapToDeserialize()
                }
            )
        })
    }

    private fun hasCompleteQuestBranch(stepIndex: Int): Boolean {
        val questObject = branchModel.objects.getActualElementValue(asObjectName(stepIndex)).orNull()
        return if (questBranch.objectId == questObject.id) false else quest.path.contains(
            questBranch.branchId,
            questObject.id,
            null
        )
    }

    override fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
//        println(slot)
    }

}