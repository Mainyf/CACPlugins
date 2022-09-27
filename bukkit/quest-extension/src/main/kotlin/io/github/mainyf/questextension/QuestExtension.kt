package io.github.mainyf.questextension

import com.guillaumevdn.gcore.lib.economy.Currency
import com.guillaumevdn.questcreator.data.user.UserQC
import com.guillaumevdn.questcreator.lib.`object`.element.ElementQuestObject
import com.guillaumevdn.questcreator.lib.quest.QuestEndType
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.successMsg
import io.github.mainyf.questextension.config.ConfigManager
import io.github.mainyf.questextension.menu.QuestListMenu
import io.github.mainyf.questextension.storage.StorageManager
import org.apache.logging.log4j.LogManager
import org.bukkit.plugin.java.JavaPlugin

class QuestExtension : JavaPlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("QuestExtension")

        lateinit var INSTANCE: QuestExtension

        val TOKEN_1 = Currency.register(CurrencyToken1())

    }

    override fun onEnable() {
        INSTANCE = this
        ConfigManager.load()
        StorageManager.init()
        apiCommand("qext") {
            "add" {
                withArguments(
                    playerArguments("玩家名")
                )
                executeOP {
                    val player = player()
                    QuestManager.addDailyQuestToPlayer(player)
                    player.msg("添加成功")
                }
            }
            "clean" {
                withArguments(
                    playerArguments("玩家名")
                )
                executeOP {
                    val player = player()
                    QuestManager.cleanDailyQuestToPlayer(player)
                    player.msg("删除成功")
                }
            }
            "menu" {
                withArguments(
                    playerArguments("玩家名")
                )
                executeOP {
                    val player = player()
                    QuestManager.openQuestListMenu(player)
                }
            }
            "test" {
                withArguments(
                    playerArguments("玩家名")
                )
                executeOP {
                    val player = player()
                    val userQC = UserQC.cachedOrNull(player)!!
                    userQC.cachedActiveQuests.forEach { quest ->
                        player.msg("=========================")
                        val model = quest.model
//                        val branche = quest.getBranches(true, true).first()
                        model.branches.actualValues.forEach { modelBranch ->
                            val branch = quest.getBranch(modelBranch.id)!!
                            modelBranch.objects.values().forEach { questObjectNode ->
                                val questObject = questObjectNode.actualValue
                                val detail = questObject.objectiveDetail.rawValue.joinToString(", ")
                                val proText = if (branch.objectId == questObject.id) {
                                    val pro = branch.getObjectProgression(questObject.id)
                                    "${pro.currentTotal.toInt()}/${pro.goalTotal.toInt()}"
                                } else {
                                    if (quest.path.contains(branch.branchId, questObject.id, null)) "已完成" else "未开启"
                                }
                                player.msg("$detail - $proText")
                            }
                        }
                        player.msg("=========================")
                    }
                }
            }
            "reload" {
                executeOP {
                    ConfigManager.load()
                    sender.successMsg("[QuestExtension] 重载完成")
                }
            }
        }.register()
    }
}