package io.github.mainyf.mcrmbmigration

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

fun CommandSender.sendLang(key: String, vararg data: Any) {
    if (Lang.map.containsKey(key)) {
//        val list = data.toMutableList()
//        if (list.isEmpty() && this is Player) {
//            list.add(mapOf(
//                "{player}" to this.name
//            ))
//        }
        Lang.map[key]?.execute(this, *data)
    }
}
