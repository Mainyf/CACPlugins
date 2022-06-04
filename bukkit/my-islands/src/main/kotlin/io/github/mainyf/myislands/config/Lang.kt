package io.github.mainyf.myislands.config

import io.github.mainyf.newmclib.config.ActionParser
import io.github.mainyf.newmclib.config.action.MultiAction
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.joor.Reflect

object Lang {

    var kudoRepeat: MultiAction? = null

    var kudoSuccess: MultiAction? = null

    fun load(section: ConfigurationSection) {
        val ref = Reflect.on(Lang)
        Lang::class.java.declaredFields.forEach {
            if (it.name != "INSTANCE") {
                ref.set(it.name, ActionParser.parseAction(section.getStringList(it.name)))
            }
        }
    }

}

fun MultiAction?.send(sender: CommandSender) {
    this?.execute(sender)
}