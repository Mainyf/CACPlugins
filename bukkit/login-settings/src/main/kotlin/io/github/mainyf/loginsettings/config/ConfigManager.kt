@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.loginsettings.config

import io.github.mainyf.loginsettings.LoginSettings
import io.github.mainyf.newmclib.config.*
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.exts.toComp
import io.github.mainyf.newmclib.exts.tvar
import net.kyori.adventure.text.Component
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

object ConfigManager {

    private lateinit var mainConfigFile: FileConfiguration
    lateinit var playerRegister: PlayerRegister
    lateinit var playerLogin: PlayerLogin
    lateinit var resourcePack: ResourcePack
    lateinit var teachingMenuConfig: TeachingMenuConfig

    var noUsageCommand: MultiAction? = null
    var teachingMenuSlotA: MultiAction? = null
    var playRuleNoExpired: MultiAction? = null
    var playRuleSuccess: MultiAction? = null

    fun load() {
        LoginSettings.INSTANCE.saveDefaultConfig()
        LoginSettings.INSTANCE.reloadConfig()

        kotlin.runCatching {
            mainConfigFile = LoginSettings.INSTANCE.config

            loadMainConfig()
        }.onFailure {
            LoginSettings.LOGGER.info("加载配置时出现错误")
            it.printStackTrace()
        }
    }

    private fun loadMainConfig() {
        noUsageCommand = ActionParser.parseAction(mainConfigFile, "noUsageCommand", false)
        playerRegister = PlayerRegister(
            mainConfigFile.getLong("register.stage1.period"),
            ActionParser.parseAction(mainConfigFile, "register.stage1.actions", false),


            mainConfigFile.getLong("register.stage2.period"),
            ActionParser.parseAction(mainConfigFile, "register.stage2.actions", false),
            ActionParser.parseAction(mainConfigFile, "register.stage2.registerNoRepeat", false),
            ActionParser.parseAction(mainConfigFile, "register.stage2.registerSuccess", false)
        )
        playerLogin = PlayerLogin(
            mainConfigFile.getLong("login.stage1.period"),
            ActionParser.parseAction(mainConfigFile, "login.stage1.actions", false),

            ActionParser.parseAction(mainConfigFile, "login.stage1.loginSuccess", false),
            ActionParser.parseAction(mainConfigFile, "login.stage1.passwordWrong", false),

            mainConfigFile.getInt("login.stage1.passwordAttempts", 5),
            mainConfigFile.getLong("login.stage1.passwordWrongBlackListTime", 5L),
            mainConfigFile.getStringList("login.stage1.blackListKickFormat").let { list ->
                Component.text { builder ->
                    list.forEachIndexed { index, s ->
                        builder.append(s.toComp())
                        if(index < list.size - 1) {
                            builder.append(Component.text("\n"))
                        }
                    }
                }
            }
        )
        resourcePack = ResourcePack(
            mainConfigFile.getString("resourcePack.url", "")!!,

            ActionParser.parseAction(mainConfigFile, "resourcePack.declined", false),
            ActionParser.parseAction(mainConfigFile, "resourcePack.failDownload", false),
            ActionParser.parseAction(mainConfigFile, "resourcePack.successLoad", false),
        )
        val teachingMenuSect = mainConfigFile.getConfigurationSection("teachingMenu")!!
        teachingMenuConfig = TeachingMenuConfig(
            teachingMenuSect.asMenuSettingsConfig(),
            teachingMenuSect.asTeachSlotAConfig("slotA"),
            teachingMenuSect.asTeachSlotConfig("slotB"),
        )

        teachingMenuSlotA = ActionParser.parseAction(mainConfigFile, "teachingMenu.slotA.playRules", false)
        playRuleNoExpired = ActionParser.parseAction(mainConfigFile, "playRuleNoExpired", false)
        playRuleSuccess = ActionParser.parseAction(mainConfigFile, "playRuleSuccess", false)
    }

    private fun ConfigurationSection.asTeachSlotAConfig(key: String): TeachSlotAConfig {
        return getConfigurationSection(key)!!.let {
            TeachSlotAConfig(
                it.getIntegerList("slot"),
                it.getStringList("iaIcons").mapNotNull { ii -> ii.asIaIcon() },
                it.asItemSlotConfig()
            )
        }
    }

    private fun ConfigurationSection.asTeachSlotConfig(key: String): TeachSlotConfig {
        return getConfigurationSection(key)!!.let {
            val slot = it.getString("slot", "2-12")!!.split("-")
            TeachSlotConfig(
                slot[0].toInt(),
                slot[1].toInt(),
                it.asItemSlotConfig()
            )
        }
    }

    class PlayerRegister(
        val stage1Period: Long,
        val stage1Actions: MultiAction?,

        val stage2Period: Long,
        val stage2Actions: MultiAction?,
        val registerNoRepeat: MultiAction?,
        val registerSuccess: MultiAction?
    )

    class PlayerLogin(
        val stage1Period: Long,
        val stage1Actions: MultiAction?,

        val loginSuccess: MultiAction?,
        val passwordWrong: MultiAction?,

        val passwordAttempts: Int,
        val passwordWrongBlackListTime: Long,
        val blackListKickFormat: Component
    )

    class ResourcePack(
        val url: String,

        val declined: MultiAction?,
        val failDownload: MultiAction?,
        val successLoad: MultiAction?
    )

    class TeachingMenuConfig(
        val settings: MenuSettingsConfig,
        val slotA: TeachSlotAConfig,
        val slotB: TeachSlotConfig,
    )

    class TeachSlotAConfig(
        val slot: List<Int>,
        val iaIcons: List<IaIcon>,
        val itemSlot: ItemSlotConfig
    )

    class TeachSlotConfig(
        val slotMin: Int,
        val slotMax: Int,
        val itemSlot: ItemSlotConfig
    )

}