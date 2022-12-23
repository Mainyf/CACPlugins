@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.loginsettings.config

import io.github.mainyf.loginsettings.LoginSettings
import io.github.mainyf.newmclib.config.*
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.exts.toComp
import net.kyori.adventure.text.Component
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration

object ConfigLS {

    private lateinit var mainConfigFile: FileConfiguration
    private lateinit var qqConfigFile: FileConfiguration
    lateinit var playerRegister: PlayerRegister
    lateinit var playerLogin: PlayerLogin
    lateinit var resourcePack: ResourcePack

    var qqEnable = true
    var emergencyAction: MultiAction? = null
    var qqBot = 3521083461L
    val qqGroup = mutableListOf(916760896L)
    var monitorEnable = true
    val monitorQQList = mutableListOf(31395967L)
    val monitorQQGroupList = mutableListOf(870756835L)
    lateinit var bindStageConfig: BindStageConfig
    lateinit var codeStageConfig: CodeStageConfig
    lateinit var resetPasswdConfig: ResetPasswdConfig

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
            val qqFile = LoginSettings.INSTANCE.dataFolder.resolve("qq.yml")
            if (!qqFile.exists()) {
                LoginSettings.INSTANCE.saveResource("qq.yml", false)
            }
            qqConfigFile = YamlConfiguration.loadConfiguration(qqFile)

            loadQQConfig()
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
            ActionParser.parseAction(mainConfigFile, "register.stage1.nextStage", false),

            mainConfigFile.getLong("register.stage2.period"),
            ActionParser.parseAction(mainConfigFile, "register.stage2.actions", false),
            ActionParser.parseAction(mainConfigFile, "register.stage2.registerNoRepeat", false),
            ActionParser.parseAction(mainConfigFile, "register.stage2.registerSuccess", false),
        )
        playerLogin = PlayerLogin(
            mainConfigFile.getLong("login.stage1.period"),
            ActionParser.parseAction(mainConfigFile, "login.stage1.actions", false),
            ActionParser.parseAction(mainConfigFile, "login.stage1.nextStage", false),
            ActionParser.parseAction(mainConfigFile, "login.stage1.noBindQQNum", false),

            ActionParser.parseAction(mainConfigFile, "login.stage1.loginSuccess", false),
            ActionParser.parseAction(mainConfigFile, "login.stage1.passwordWrong", false),

            mainConfigFile.getInt("login.stage1.passwordAttempts", 5),
            mainConfigFile.getLong("login.stage1.passwordWrongBlackListTime", 5L),
            mainConfigFile.getStringList("login.stage1.blackListKickFormat").let { list ->
                Component.text { builder ->
                    list.forEachIndexed { index, s ->
                        builder.append(s.toComp())
                        if (index < list.size - 1) {
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

    private fun loadQQConfig() {
        this.qqEnable = qqConfigFile.getBoolean("enable", true)
        this.emergencyAction = ActionParser.parseAction(qqConfigFile, "emergencyAction", false)
        this.qqBot = qqConfigFile.getLong("qqBot")
        this.qqGroup.clear()
        this.qqGroup.addAll(qqConfigFile.getLongList("qqGroup"))
        this.monitorEnable = qqConfigFile.getBoolean("monitor.enable", true)
        this.monitorQQList.clear()
        this.monitorQQList.addAll(qqConfigFile.getLongList("monitor.qqList"))
        this.monitorQQGroupList.clear()
        this.monitorQQGroupList.addAll(qqConfigFile.getLongList("monitor.qqGroupList"))
        bindStageConfig = BindStageConfig(
            ActionParser.parseAction(qqConfigFile, "bindStage.actions", false),
            ActionParser.parseAction(qqConfigFile, "bindStage.nextStage", false),
            qqConfigFile.getString("bindStage.qqRegex")!!.toRegex(),
            ActionParser.parseAction(qqConfigFile, "bindStage.formatError", false),
            ActionParser.parseAction(qqConfigFile, "bindStage.qqAlreadyBind", false)
        )
        codeStageConfig = CodeStageConfig(
            ActionParser.parseAction(qqConfigFile, "codeStage.actions", false),
            qqConfigFile.getString("codeStage.veritySuccess"),
            ActionParser.parseAction(qqConfigFile, "codeStage.loginFinish", false),
            ActionParser.parseAction(qqConfigFile, "codeStage.registerFinish", false)
        )
        resetPasswdConfig = ResetPasswdConfig(
            ActionParser.parseAction(qqConfigFile, "resetPasswd.actions", false),
            ActionParser.parseAction(qqConfigFile, "resetPasswd.nextStage", false),
            qqConfigFile.getString("resetPasswd.veritySuccess"),
            ActionParser.parseAction(qqConfigFile, "resetPasswd.sendNewPasswd", false),
            ActionParser.parseAction(qqConfigFile, "resetPasswd.confirmNewPasswd", false),
            ActionParser.parseAction(qqConfigFile, "resetPasswd.passwdDiscrepancy", false),
            ActionParser.parseAction(qqConfigFile, "resetPasswd.finish", false)
        )
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
        val nextStage: MultiAction?,

        val stage2Period: Long,
        val stage2Actions: MultiAction?,
        val registerNoRepeat: MultiAction?,
        val registerSuccess: MultiAction?
    )

    class PlayerLogin(
        val stage1Period: Long,
        val stage1Actions: MultiAction?,
        val nextStage: MultiAction?,
        val noBindQQNum: MultiAction?,

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
