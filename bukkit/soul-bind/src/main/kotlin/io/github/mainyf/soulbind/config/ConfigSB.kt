package io.github.mainyf.soulbind.config

import io.github.mainyf.newmclib.config.BaseLang
import io.github.mainyf.newmclib.config.asDefaultSlotConfig
import io.github.mainyf.newmclib.config.asMenuSettingsConfig
import io.github.mainyf.newmclib.exts.createFileConfiguration
import io.github.mainyf.newmclib.exts.getSection
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
    private lateinit var menuConfig: FileConfiguration
    private lateinit var langConfig: FileConfiguration

    val autoBindIAList = mutableListOf<String>()
    val bindItemLore = mutableListOf<String>()

    var recallCost = 1
    var abandonCost = 1
    lateinit var recallItemMenuConfig: RecallItemMenuConfig
    lateinit var abandonConfirmMenu: AbandonConfirmMenu

    lateinit var lang: BaseLang

    fun load() {
        kotlin.runCatching {
            mainConfig = SoulBind.INSTANCE.createFileConfiguration("config.yml")
            menuConfig = SoulBind.INSTANCE.createFileConfiguration("menu.yml")
            langConfig = SoulBind.INSTANCE.createFileConfiguration("lang.yml")

            lang = BaseLang()

            loadMenuConfig()
            loadMainConfig()
            lang.load(langConfig)
        }.onFailure {
            SoulBind.LOGGER.info("加载配置时出现错误")
            it.printStackTrace()
        }
    }

    private fun loadMenuConfig() {
        val recallItemMenuSect = menuConfig.getSection("recallItemMenu")
        recallItemMenuConfig = RecallItemMenuConfig(
            recallItemMenuSect.asMenuSettingsConfig(),
            recallItemMenuSect.asDefaultSlotConfig("recallItemSlot"),
            recallItemMenuSect.asDefaultSlotConfig("prevSlot"),
            recallItemMenuSect.asDefaultSlotConfig("nextSlot"),
            recallItemMenuSect.asDefaultSlotConfig("infoSlot"),
            recallItemMenuSect.asDefaultSlotConfig("backSlot")
        )

        val abandonConfirmMenuSect = menuConfig.getSection("abandonConfirmMenu")
        abandonConfirmMenu = AbandonConfirmMenu(
            abandonConfirmMenuSect.asMenuSettingsConfig(),
            abandonConfirmMenuSect.asDefaultSlotConfig("itemSlot"),
            abandonConfirmMenuSect.asDefaultSlotConfig("confirmSlot"),
            abandonConfirmMenuSect.asDefaultSlotConfig("backSlot")
        )
    }

    private fun loadMainConfig() {
        bindItemLore.clear()
        bindItemLore.addAll(mainConfig.getStringList("bindItemLore"))

        autoBindIAList.clear()
        autoBindIAList.addAll(mainConfig.getStringList("autoBindIAList"))

        recallCost = mainConfig.getInt("recallCost")
        abandonCost = mainConfig.getInt("abandonCost")
    }

}