package io.github.mainyf.toolsplugin

import dev.jorel.commandapi.arguments.BooleanArgument
import io.github.mainyf.bungeesettingsbukkit.ServerPacket
import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.command.stringArguments
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.hooks.addPlaceholderExpansion
import io.github.mainyf.toolsplugin.config.ConfigTP
import io.github.mainyf.toolsplugin.module.*
import io.papermc.paper.configuration.GlobalConfiguration
import me.clip.placeholderapi.PlaceholderAPI
import net.luckperms.api.LuckPermsProvider
import org.apache.logging.log4j.LogManager
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.math.ceil

class ToolsPlugin : BasePlugin(), Listener {

    companion object {

        val LOGGER = LogManager.getLogger("ToolsPlugin")

        lateinit var INSTANCE: ToolsPlugin

        val OP_MSG = ServerPacket.registerPacket("broadcast_toolsplugin_op_msg")

    }

    private val luckPerms by lazy { LuckPermsProvider.get() }
    private val playerNVSet = mutableSetOf<UUID>()

    override fun enable() {
        INSTANCE = this
        ConfigTP.load()
        pluginManager().registerEvents(this, this)
        pluginManager().registerEvents(RecycleEnderDragonEgg, this)
        pluginManager().registerEvents(IaRecipe, this)
        pluginManager().registerEvents(ChunkLogger, this)
        pluginManager().registerEvents(CheckPlayerInventory, this)
        pluginManager().registerEvents(IaItemAutoUpdate, this)
        CheckPlayerInventory.init()
        IaItemAutoUpdate.init()
        ExportPlayerData.init()
        MineralSpawns.init()
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
            "test" {
                executeOP {
                    val config = GlobalConfiguration.get().chunkLoading.playerMaxConcurrentLoads
                    val max = GlobalConfiguration.get().chunkLoading.globalMaxConcurrentLoads
                    val maxCount = ceil(
                        (config * onlinePlayers().size).coerceAtMost(if (max <= 1.0) Double.MAX_VALUE else max)
                    ).toInt()
                    sender.msg("一次性最多处理: $maxCount 个区块")
                }
            }
            "toggleNV" {
                withArguments(playerArguments("玩家"))
                executeOP {
                    val player = player()
                    if (playerNVSet.contains(player.uuid)) {
                        playerNVSet.remove(player.uuid)
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                        ConfigTP.saturdayNightVisionToggleAction?.execute(player, "{status}", "关闭")
                    } else {
                        playerNVSet.add(player.uuid)
                        ConfigTP.saturdayNightVisionToggleAction?.execute(player, "{status}", "开启")
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
        submitTask(period = 20L) {
            if (!ConfigTP.saturdayNightVisionEnable) return@submitTask
            val calendar = Calendar.getInstance()
            val week = calendar[Calendar.DAY_OF_WEEK]
            if (week == 7) {
                onlinePlayers().forEach { player ->
                    if (!playerNVSet.contains(player.uuid)) return@forEach
                    if (player.activePotionEffects.any {
                            it.type != PotionEffectType.NIGHT_VISION || it.duration < 12 * 20
                        }) {
                        player.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.NIGHT_VISION,
                                15 * 20,
                                1
                            )
                        )
                    }
                }
            } else {
                onlinePlayers().forEach { player ->
                    if (!playerNVSet.contains(player.uuid)) return@forEach
                    if (!player.hasPermission(ConfigTP.saturdayNightVisionMvpPerm)) return@forEach
                    if (player.activePotionEffects.isEmpty() || player.activePotionEffects.any {
                            it.type != PotionEffectType.NIGHT_VISION || it.duration < 12 * 20
                        }) {
                        player.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.NIGHT_VISION,
                                15 * 20,
                                1
                            )
                        )
                    }
                }
            }
        }
        addPlaceholderExpansion("toolsplugin") { p, params ->
            val placeholder = ConfigTP.placeholders[params] ?: return@addPlaceholderExpansion null
            val targetText = PlaceholderAPI.setPlaceholders(p, placeholder.target)
            PlaceholderAPI.setPlaceholders(p, placeholder.change[targetText] ?: placeholder.other)
        }
    }

    override fun onDisable() {
        ExportPlayerData.close()
    }

}