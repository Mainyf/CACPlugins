package io.github.mainyf.questextension.`object`

import com.guillaumevdn.gcore.lib.compatibility.material.CommonMats
import com.guillaumevdn.gcore.lib.element.struct.Need
import com.guillaumevdn.gcore.lib.location.position.PositionNeed
import com.guillaumevdn.gcore.lib.string.TextElement
import com.guillaumevdn.gcore.lib.string.placeholder.Replacer
import com.guillaumevdn.questcreator.lib.`object`.*
import com.guillaumevdn.questcreator.lib.`object`.element.AbstractElementQuestObject
import com.guillaumevdn.questcreator.lib.`object`.element.ElementQuestObject
import com.guillaumevdn.questcreator.lib.`object`.type.QuestObjectTypeEventGoalCustom
import com.guillaumevdn.questcreator.lib.quest.call.QuestCallBranchProgress
import io.github.mainyf.questextension.events.ManualCompleteQuestEvent
import org.bukkit.Location
import org.bukkit.entity.Player


class TypePlayerCommandCode : QuestObjectTypeEventGoalCustom<ManualCompleteQuestEvent>(
    "PLAYER_COMMAND_CODE",
    CommonMats.PLAYER_HEAD,
    PositionNeed.OPTIONAL,
    AllowDynamicProgression.YES,
    AllowItemsNeeded.YES,
    AllowProgressConditions.YES,
    EVENT
) {

    companion object {

        val EVENT: QuestObjectEvent<ManualCompleteQuestEvent> =
            EventRegistry.inst().register(ManualCompleteQuestEvent::class.java, "getPlayer")

    }


    override fun doFillSettingsElements(`object`: AbstractElementQuestObject) {
        super.doFillSettingsElements(`object`)
        `object`.addStringList("operation_type", Need.required(), TextElement("操作类型"))
    }

    override fun match(
        `object`: AbstractElementQuestObject,
        call: QuestCallBranchProgress,
        progression: ObjectProgression,
        event: ManualCompleteQuestEvent,
        eventPlayer: Player
    ): Boolean {
        val operationType = `object`.directParseOrNull<List<String>>("operation_type", call).firstOrNull()
        if (operationType != event.name) {
            return false
        }
        return true
    }

    override fun findGPSCompassLocationIfNoPosition(
        `object`: ElementQuestObject,
        player: Player,
        replacer: Replacer
    ): Location? {
        return player.location
    }

}