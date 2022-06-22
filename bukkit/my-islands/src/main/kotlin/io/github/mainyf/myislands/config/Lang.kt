@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.myislands.config

import io.github.mainyf.newmclib.config.LangUtils
import io.github.mainyf.newmclib.config.action.MultiAction
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection

object Lang {

    val map = mutableMapOf<String, MultiAction?>()

    fun load(section: ConfigurationSection) {
        map.clear()
        map.putAll(LangUtils.asLangToMap(section))
    }

}

fun CommandSender.sendLang(key: String) {
    if (Lang.map.containsKey(key)) {
        Lang.map[key]?.execute(this)
    }
}
