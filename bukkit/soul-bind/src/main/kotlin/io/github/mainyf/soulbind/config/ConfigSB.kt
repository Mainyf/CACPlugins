package io.github.mainyf.soulbind.config

import io.github.mainyf.newmclib.config.BaseLang
import io.github.mainyf.newmclib.exts.createFileConfiguration
import io.github.mainyf.soulbind.SoulBind
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration

fun CommandSender.sendLang(key: String, vararg data: Any) {
    ConfigSB.lang.apply {
        sendLang(key, *data)
    }
}

object ConfigSB {

    private lateinit var mainConfig: FileConfiguration
    private lateinit var langConfig: FileConfiguration

    val bindItemLore = mutableListOf<String>()

    lateinit var lang: BaseLang

    fun load() {
        kotlin.runCatching {
            mainConfig = SoulBind.INSTANCE.createFileConfiguration("config.yml")
            langConfig = SoulBind.INSTANCE.createFileConfiguration("lang.yml")

            lang = BaseLang()

            loadMainConfig()
            lang.load(langConfig)
        }.onFailure {
            SoulBind.LOGGER.info("加载配置时出现错误")
            it.printStackTrace()
        }
    }

    private fun loadMainConfig() {
        bindItemLore.clear()
        bindItemLore.addAll(mainConfig.getStringList("bindItemLore"))
    }

}