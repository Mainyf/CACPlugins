@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.playeraccount.config

import io.github.mainyf.playeraccount.PlayerAccount
import org.bukkit.configuration.file.FileConfiguration

object ConfigManager {

    var debug = false

    private lateinit var mainConfigFile: FileConfiguration

    var accessKeyId: String? = null
    var accessKeySecret: String? = null
    var regionID: String? = null

    var signName: String? = null
    var templateCode: String? = null

    fun load() {
        PlayerAccount.INSTANCE.saveDefaultConfig()
        PlayerAccount.INSTANCE.reloadConfig()
        mainConfigFile = PlayerAccount.INSTANCE.config

        kotlin.runCatching {
            loadMainConfig()
        }.onFailure {
            PlayerAccount.LOGGER.info("加载配置时出现错误")
            it.printStackTrace()
        }
    }

    private fun loadMainConfig() {
        debug = mainConfigFile.getBoolean("debug", false)
        accessKeyId = mainConfigFile.getString("accessKeyId")
        accessKeySecret = mainConfigFile.getString("accessKeySecret")
        regionID = mainConfigFile.getString("regionID")
        signName = mainConfigFile.getString("signName")
        templateCode = mainConfigFile.getString("templateCode")
    }

}