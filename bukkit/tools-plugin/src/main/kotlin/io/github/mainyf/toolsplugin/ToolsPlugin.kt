package io.github.mainyf.toolsplugin

import dev.jorel.commandapi.arguments.BooleanArgument
import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.stringArguments
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.toolsplugin.config.ConfigTP
import io.github.mainyf.toolsplugin.module.ExportPlayerData
import io.github.mainyf.toolsplugin.module.IaRecipe
import io.github.mainyf.toolsplugin.module.RecycleEnderDragonEgg
import net.luckperms.api.LuckPermsProvider
import org.apache.logging.log4j.LogManager
import org.bukkit.event.Listener
import java.util.*

class ToolsPlugin : BasePlugin(), Listener {

    companion object {

        val LOGGER = LogManager.getLogger("ToolsPlugin")

        lateinit var INSTANCE: ToolsPlugin

    }

    private val luckPerms by lazy { LuckPermsProvider.get() }

    override fun enable() {
        INSTANCE = this
        ConfigTP.load()
        pluginManager().registerEvents(this, this)
        pluginManager().registerEvents(RecycleEnderDragonEgg, this)
        pluginManager().registerEvents(IaRecipe, this)
        ExportPlayerData.init()
        apiCommand("toolsPlugin") {
            withAliases("tools", "toolsp")
            "reload" {
                executeOP {
                    ConfigTP.load()
                    sender.successMsg("[ToolsPlugin] 重载成功")
                }
            }
            "exportGroup" {
                withArguments(
                    stringArguments("组名") { _ -> luckPerms.groupManager.loadedGroups.map { it.name }.toTypedArray() },
                    BooleanArgument("是否加上QQ号")
                )
                executeOP {
                    val groupName = text()
                    val hasWriteQQNum = value<Boolean>()
                    val group = luckPerms.groupManager.getGroup(groupName)
                    if (group == null) {
                        sender.errorMsg("该组不存在")
                        return@executeOP
                    }
                    ExportPlayerData.exportPlayerGroup(groupName, hasWriteQQNum).whenComplete { t, u ->
                        sender.successMsg("导出成功: $t")
                    }
                }
            }
        }.register()
        submitTask(period = 20L) {
            if (!ConfigTP.saturdayFly) return@submitTask
            val calendar = Calendar.getInstance()
            val week = calendar[Calendar.DAY_OF_WEEK]
            if (week != 7) {
                onlinePlayers().forEach { player ->
                    if (player.allowFlight && !player.hasPermission("toolplugin.fly")) {
                        player.allowFlight = false
                    }
                }
            }
        }
    }

    override fun onDisable() {
        ExportPlayerData.close()
    }

}