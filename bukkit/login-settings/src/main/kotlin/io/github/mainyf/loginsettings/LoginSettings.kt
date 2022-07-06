package io.github.mainyf.loginsettings

import fr.xephi.authme.AuthMe
import fr.xephi.authme.data.auth.PlayerCache
import fr.xephi.authme.datasource.DataSource
import fr.xephi.authme.libs.ch.jalu.injector.Injector
import fr.xephi.authme.service.BukkitService
import fr.xephi.authme.service.CommonService
import fr.xephi.authme.service.ValidationService
import io.github.mainyf.loginsettings.config.ConfigManager
import io.github.mainyf.newmclib.exts.asPlugin
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.toReflect
import org.apache.logging.log4j.LogManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class LoginSettings : JavaPlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("LoginSettings")

        lateinit var INSTANCE: LoginSettings

        lateinit var authMeInject: Injector

        lateinit var validationService: ValidationService

        lateinit var commonService: CommonService

        lateinit var playerCache: PlayerCache

        lateinit var database: DataSource

        lateinit var bukkitService: BukkitService
    }

    override fun onEnable() {
        INSTANCE = this
        val authMe = "AuthMe".asPlugin() as AuthMe

        authMeInject = authMe.toReflect().get("injector")

        validationService = tryGetAuthMeService(ValidationService::class.java, "无法检测到AuthMe插件的密码验证器，插件工作已停止")
        commonService = tryGetAuthMeService(CommonService::class.java, "无法检测到AuthMe插件的通用功能模块，插件工作已停止")
        playerCache = tryGetAuthMeService(PlayerCache::class.java, "无法检测到AuthMe插件的玩家缓存模块，插件工作已停止")
        database = tryGetAuthMeService(DataSource::class.java, "无法检测到AuthMe插件的数据储存模块，插件工作已停止")
        bukkitService = tryGetAuthMeService(BukkitService::class.java, "无法检测到AuthMe插件的Bukkit模块，插件工作已停止")

        ConfigManager.load()
        CommandHandler.register()
        PlayerAuthHandler.runTaskTimer(this, 10, 10)
        pluginManager().registerEvents(PlayerAuthHandler, this)
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