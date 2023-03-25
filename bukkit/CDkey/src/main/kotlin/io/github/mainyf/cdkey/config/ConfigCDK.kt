package io.github.mainyf.cdkey.config

import io.github.mainyf.cdkey.CDkey
import io.github.mainyf.newmclib.config.BaseLang
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.exts.getAction
import io.github.mainyf.newmclib.exts.getEnum
import io.github.mainyf.newmclib.exts.getSection
import io.github.mainyf.newmclib.exts.saveResourceToFileAsConfiguration
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection

fun CommandSender.sendLang(key: String, vararg data: Any) {
    ConfigCDK.lang.apply {
        sendLang(key, *data)
    }
}

object ConfigCDK {

    lateinit var mainConfig: ConfigurationSection
    lateinit var langConfigFile: ConfigurationSection

    lateinit var lang: BaseLang

    var propagandistCDKeyValidTime = 20L
    val propagandistMap = mutableMapOf<String, CDKeyConfig>()
    val codeMap = mutableMapOf<String, CDKeyConfig>()

    fun load() {
        runCatching {
            mainConfig = CDkey.INSTANCE.saveResourceToFileAsConfiguration("config.yml")
            langConfigFile = CDkey.INSTANCE.saveResourceToFileAsConfiguration("lang.yml")

            lang = BaseLang()
            lang.load(langConfigFile)

            loadMainConfig()
        }.onFailure {
            CDkey.LOGGER.info("加载配置时出现错误")
            it.printStackTrace()
        }
    }

    private fun loadMainConfig() {
        propagandistCDKeyValidTime = mainConfig.getLong("propagandistCDKeyValidTime", propagandistCDKeyValidTime)
        val propagandistsSect = mainConfig.getSection("propagandist")
        propagandistsSect.getKeys(false).forEach { pName ->
            val propagandistSect = propagandistsSect.getSection(pName)
            val code = propagandistSect.getString("code")!!
            val actions = propagandistSect.getAction("actions")
            propagandistMap[pName] = CDKeyConfig(
                pName,
                CDKeyType.PROPAGANDIST,
                code,
                actions
            )
        }
        val codesSect = mainConfig.getSection("codes")
        codesSect.getKeys(false).forEach { cName ->
            val codeSect = codesSect.getSection(cName)
            val type = codeSect.getEnum("type", CDKeyType.DEFINE)!!
            val code = codeSect.getString("code", "")!!
            val actions = codeSect.getAction("actions")
            codeMap[cName] = CDKeyConfig(
                cName,
                type,
                code,
                actions
            )
        }
    }

    fun getAllDefaultCDKey(): Set<String> {
        val rs = mutableSetOf<String>()
        rs.addAll(propagandistMap.values.map { it.code })
        rs.addAll(codeMap.values.filter { it.type == CDKeyType.DEFINE }.map { it.code })
        return rs
    }

}

class CDKeyConfig(
    val codeName: String,
    val type: CDKeyType,
    val code: String,
    val actions: MultiAction?
)

enum class CDKeyType {
    PROPAGANDIST,
    DEFINE,
    CONSUME
}