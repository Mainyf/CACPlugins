package io.github.mainyf.questextension

import com.guillaumevdn.questcreator.ConfigQC
import com.guillaumevdn.questcreator.data.user.UserQC
import com.guillaumevdn.questcreator.lib.quest.Quest
import com.guillaumevdn.questcreator.lib.quest.QuestStartCause
import com.guillaumevdn.questcreator.lib.quest.QuestStopCause
import com.guillaumevdn.questcreator.lib.quest.QuestUtilsFlowStart
import com.guillaumevdn.questcreator.lib.quest.QuestUtilsFlowStop
import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.questextension.config.ConfigManager
import io.github.mainyf.questextension.menu.QuestListMenu
import io.github.mainyf.questextension.storage.PlayerDailyQuest
import io.github.mainyf.questextension.storage.StorageManager
import org.bukkit.entity.Player
import org.joda.time.DateTime

object QuestManager {

    private fun getUserQC(player: Player): UserQC? {
        val userQC = UserQC.cachedOrNull(player)
        if (userQC == null) {
            player.errorMsg("发生错误，请告知管理员: USERQC_NULL")
            return null
        }
        return userQC
    }

    private fun getPlayerDailyQuest(userQC: UserQC): List<Quest> {
        return userQC.cachedActiveQuests.filter {
            ConfigManager.questPool.contains(it.modelId)
        }
    }

    fun openQuestListMenu(player: Player) {
        val userQC = getUserQC(player) ?: return
        val currentTime = DateTime.now().withTimeAtStartOfDay()
        var questData = StorageManager.findDailyQuest(player)
        if (questData == null) {
            questData = addDailyQuestToPlayer(player)
        } else {
            if (questData.questStartTime.isBefore(currentTime)) {
//                val quests = getPlayerDailyQuest(userQC)
//                player.msg("任务 ${quests.joinToString(", ") { it.model.displayName.getRawValueLine(0) }} 已失效")
                cleanDailyQuestToPlayer(player)
                questData = addDailyQuestToPlayer(player)
            }
        }
        QuestListMenu(
            userQC,
            getPlayerDailyQuest(userQC),
            questData.questList.mapNotNull { ConfigQC.models.getElement(it).orNull() }).open(player)
    }

    fun addDailyQuestToPlayer(player: Player): PlayerDailyQuest {
        val rs = mutableListOf<Quest>()
        val quests = mutableSetOf<String>()
        val questPool = ConfigManager.questPool.toMutableSet()
        repeat(5) {
            val questName = questPool.random()
            quests.add(questName)
            questPool.remove(questName)
        }
        quests.forEach {
            val model = ConfigQC.models.getElement(it)
            if (!model.isPresent) {
                player.errorMsg("发生错误，请告知管理员: NO_EXISTS_${it}")
                return@forEach
            }
            rs.add(QuestUtilsFlowStart.doStartQuest(model.orNull(), player, QuestStartCause.PLUGIN))
        }
//        player.msg("任务 ${rs.joinToString(", ") { it.model.displayName.getRawValueLine(0) }} 已接受")
//        return rs
        return StorageManager.addDailyQuestData(player, rs.map { it.modelId })
    }

    fun cleanDailyQuestToPlayer(player: Player) {
        val questPool = ConfigManager.questPool
        val userQC = getUserQC(player) ?: return
        questPool.forEach {
            val quest = userQC.getCachedActiveQuest(it) ?: return@forEach
            QuestUtilsFlowStop.attemptStopOrLeave(quest, player.uuid, QuestStopCause.PLUGIN)
        }
        StorageManager.cleanDailyQuest(player)
    }

}