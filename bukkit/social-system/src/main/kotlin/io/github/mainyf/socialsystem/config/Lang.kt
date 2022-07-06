@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.socialsystem.config

import io.github.mainyf.newmclib.config.LangUtils
import io.github.mainyf.newmclib.config.action.MultiAction
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

object Lang {

    val map = mutableMapOf<String, MultiAction?>()

    fun load(section: ConfigurationSection) {
        map.clear()
        map.putAll(LangUtils.asLangToMap(section))
    }

}

fun CommandSender.sendLang(key: String, vararg data: Any) {
    if (Lang.map.containsKey(key)) {
        Lang.map[key]?.execute(this, *data)
    }
}
