package io.github.mainyf.loginsettings

import fr.xephi.authme.AuthMe
import fr.xephi.authme.data.auth.PlayerCache
import fr.xephi.authme.datasource.DataSource
import fr.xephi.authme.libs.ch.jalu.injector.Injector
import fr.xephi.authme.process.changepassword.AsyncChangePassword
import fr.xephi.authme.service.BukkitService
import fr.xephi.authme.service.CommonService
import fr.xephi.authme.service.ValidationService
import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.loginsettings.config.ConfigLS
import io.github.mainyf.loginsettings.module.AuthmeListeners
import io.github.mainyf.loginsettings.module.BindQQs
import io.github.mainyf.loginsettings.module.PlayerAuths
import io.github.mainyf.loginsettings.module.ResetPasswords
import io.github.mainyf.loginsettings.storage.StorageLS
import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.LogFile
import io.github.mainyf.newmclib.exts.asPlugin
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.submitTask
import io.github.mainyf.newmclib.exts.toReflect
import me.dreamvoid.miraimc.api.MiraiBot
import org.apache.logging.log4j.LogManager

class LoginSettings : BasePlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("LoginSettings")

        lateinit var INSTANCE: LoginSettings

        lateinit var authMeInject: Injector

        lateinit var asyncChangePassword: AsyncChangePassword

        lateinit var validationService: ValidationService

        lateinit var commonService: CommonService

        lateinit var playerCache: PlayerCache

        lateinit var database: DataSource

        lateinit var bukkitService: BukkitService
    }

    lateinit var log: LogFile

    override fun enable() {
        INSTANCE = this
        this.log = LogFile(dataFolder.resolve("qq.log"))
        val authMe = "AuthMe".asPlugin() as AuthMe

        StorageLS.init()
        authMeInject = authMe.toReflect().get("injector")

        asyncChangePassword = tryGetAuthMeService(AsyncChangePassword::class.java, "无法检测到AuthMe插件的密码验证器，插件工作已停止")
        validationService = tryGetAuthMeService(ValidationService::class.java, "无法检测到AuthMe插件的密码验证器，插件工作已停止")
        commonService = tryGetAuthMeService(CommonService::class.java, "无法检测到AuthMe插件的通用功能模块，插件工作已停止")
        playerCache = tryGetAuthMeService(PlayerCache::class.java, "无法检测到AuthMe插件的玩家缓存模块，插件工作已停止")
        database = tryGetAuthMeService(DataSource::class.java, "无法检测到AuthMe插件的数据储存模块，插件工作已停止")
        bukkitService = tryGetAuthMeService(BukkitService::class.java, "无法检测到AuthMe插件的Bukkit模块，插件工作已停止")

        ConfigLS.load()
        CommandHandler.register()
        PlayerAuths.init()
        PlayerAuths.runTaskTimer(this, 10, 10)
        pluginManager().registerEvents(PlayerAuths, this)
        pluginManager().registerEvents(BindQQs, this)
        BindQQs.init()
        pluginManager().registerEvents(ResetPasswords, this)
        ResetPasswords.init()
        pluginManager().registerEvents(AuthmeListeners, this)
        submitTask(period = 5L) {
            if(!ConfigLS.qqEnable) return@submitTask
            val bot = MiraiBot.getBot(ConfigLS.qqBot)
            if(bot.friendList.isEmpty()) {
                return@submitTask
            }
            this.cancel()
            submitTask(period = 30 * 60 * 1000L) submitTask2@{
                if (!ConfigLS.monitorEnable) return@submitTask2
                LOGGER.info("开始获取已登录的机器人: ${ConfigLS.qqBot}")
                val qqBot = MiraiBot.getBot(ConfigLS.qqBot)
                LOGGER.info("获取成功")
                LOGGER.info("好友列表: ${qqBot.friendList.joinToString(", ")}")
                ConfigLS.monitorQQList.forEach {
                    kotlin.runCatching {
                        LOGGER.info("正在给服务器管理员: $it 发送消息")
                        val friend = qqBot.getFriend(it)
                        if (friend == null) {
                            LOGGER.info("无法获取: $it,没有在好友列表找到")
                            return@forEach
                        }
                        friend.sendMessage("当前服务器在线人数：${CrossServerManager.playerMap.size}")
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
                ConfigLS.monitorQQGroupList.forEach {
                    kotlin.runCatching {
                        LOGGER.info("正在给群组: $it 发送消息")
                        val group = qqBot.getGroup(it)
                        if (group == null) {
                            LOGGER.info("无法获取: $it,没有在群组列表找到")
                            return@forEach
                        }
                        group.sendMessage("当前服务器在线人数：${CrossServerManager.playerMap.size}")
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> tryGetAuthMeService(clazz: Class<*>, msg: String): T {
        val rs = authMeInject.getIfAvailable(clazz)
        if (rs == null) {
            pluginManager().disablePlugin(this)
            throw java.lang.RuntimeException(msg)
        }
        return rs as T
    }

}