package io.github.mainyf.questextension

import com.guillaumevdn.questcreator.QuestCreator
import com.guillaumevdn.questcreator.api.QCPreReloadElementsEvent
import com.guillaumevdn.questcreator.data.user.UserQC
import com.guillaumevdn.questcreator.lib.`object`.type.QuestObjectType
import com.guillaumevdn.questcreator.lib.quest.Quest
import dev.jorel.commandapi.arguments.GreedyStringArgument
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.command.stringArguments
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.hooks.addPlaceholderExpansion
import io.github.mainyf.questextension.config.ConfigQE
import io.github.mainyf.questextension.events.ManualCompleteQuestEvent
import io.github.mainyf.questextension.menu.QuestDetailMenu
import io.github.mainyf.questextension.`object`.TypePlayerCommandCode
import io.github.mainyf.questextension.storage.StorageManager
import org.apache.logging.log4j.LogManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import kotlin.collections.find


class QuestExtension : JavaPlugin(), Listener {

    companion object {

        val LOGGER = LogManager.getLogger("QuestExtension")

        lateinit var INSTANCE: QuestExtension

    }

    override fun onEnable() {
        INSTANCE = this
        ConfigQE.load()
        StorageManager.init()
        pluginManager().registerEvents(this, this)
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
            "reset" {
                withArguments(
                    playerArguments("玩家名")
                )
                executeOP {
                    val player = player()
                    QuestManager.cleanDailyQuestToPlayer(player)
                    QuestManager.addDailyQuestToPlayer(player)
                    player.msg("重置成功")
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
            "menu-quest" {
                withArguments(
                    playerArguments("玩家名"),
                    stringArguments("任务ID")
                )
                executeOP {
                    val player = player()
                    val questModelID = text()
                    val userQC = QuestManager.getUserQC(player) ?: return@executeOP
                    val quest = userQC.cachedActiveQuests.find {
                        it.modelId == questModelID
                    } ?: return@executeOP
                    QuestDetailMenu(userQC, quest).open(player)
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
                                    if (quest.path.contains(
                                            branch.branchId,
                                            questObject.id,
                                            null
                                        )
                                    ) "已完成" else "未开启"
                                }
                                player.msg("$detail - $proText")
                            }
                        }
                        player.msg("=========================")
                    }
                }
            }
            "complete" {
                withArguments(
                    playerArguments("玩家名"),
                    GreedyStringArgument("类型")
                )
                executeOP {
                    val player = player()
                    val name = text()
                    val event = ManualCompleteQuestEvent(player, name)
                    pluginManager().callEvent(event)
                }
            }
            "reload" {
                executeOP {
                    ConfigQE.load()
                    sender.successMsg("[QuestExtension] 重载完成")
                }
            }
        }.register()
        addPlaceholderExpansion("questextension") papi@{ offlinePlayer, params ->
            val player = offlinePlayer?.player ?: return@papi "no"
            when (params) {
                "tutorial" -> {
                    val quest = getTutorialQuest(player)
                    quest?.getFirstBranch(true, true)?.objectId ?: "no"
                }

                else -> "no"
            }
        }
        submitTask(period = 20) {
            onlinePlayers().forEach {
                val data = UserQC.cachedOrNull(it)
                if (data == null) {
                    submitTask(async = true) {
                        UserQC.processWithQuests(it.uuid) {}
                    }
                }
            }
        }
        QuestCreator.inst().configuration.toReflect().call("loadElements")
    }

    fun hasInTutorialQuest(player: Player): Boolean {
        return getTutorialQuest(player) != null
    }

    fun getTutorialQuest(player: Player): Quest? {
        val userQC = QuestManager.getUserQC(player, false) ?: return null
        val quests = userQC.cachedActiveQuests
        return quests.find {
            it.model.id == ConfigQE.tutorialQuest
        }
    }

    override fun onDisable() {
        QuestCreator.inst().questObjectTypes.unregister("PLAYER_COMMAND_CODE");
        TypePlayerCommandCode.EVENT.unregisterListener()
    }

    @EventHandler
    fun registerQCTypes(event: QCPreReloadElementsEvent) {
        QuestCreator.inst().questObjectTypes.register<QuestObjectType>(TypePlayerCommandCode())
        TypePlayerCommandCode.EVENT.registerListener()
    }

}