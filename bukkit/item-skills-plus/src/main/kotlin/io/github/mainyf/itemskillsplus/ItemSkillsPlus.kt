package io.github.mainyf.itemskillsplus

import io.github.mainyf.itemskillsplus.config.ConfigManager
import io.github.mainyf.itemskillsplus.skill.LuckSkill
import io.github.mainyf.itemskillsplus.skill.PowerSkill
import io.github.mainyf.itemskillsplus.skill.SharpSkill
import io.github.mainyf.itemskillsplus.skill.ExpandSkill
import io.github.mainyf.itemskillsplus.storage.StorageManager
import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.exts.registerCommand
import org.bukkit.Bukkit

class ItemSkillsPlus : BasePlugin() {

    companion object {
        lateinit var INSTANCE: ItemSkillsPlus
    }

    override fun enable() {
        INSTANCE = this
        kotlin.runCatching {
            ConfigManager.load()
        }.onFailure {
            it.printStackTrace()
        }
        StorageManager.init()
        this.registerCommand("isp", CommandHandler())
        ExpandSkill.init()
        LuckSkill.init()
        SharpSkill.init()
        PowerSkill.init()
        Bukkit.getServer().pluginManager.registerEvents(ExpandSkill, this)
        Bukkit.getServer().pluginManager.registerEvents(LuckSkill, this)
        Bukkit.getServer().pluginManager.registerEvents(SharpSkill, this)
        Bukkit.getServer().pluginManager.registerEvents(PowerSkill, this)

        Bukkit.getServer().pluginManager.registerEvents(PlayerInventoryListeners, this)
    }

    override fun onDisable() {
        StorageManager.destory()
    }

}