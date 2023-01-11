package io.github.mainyf.toolsplugin.util

import io.github.mainyf.newmclib.exts.formatYMDHMS
import io.github.mainyf.newmclib.exts.submitTask
import org.bukkit.plugin.Plugin
import java.io.File
import java.io.FileWriter
import java.util.*

class TextUtils(
    private val plugin: Plugin,
    var logFile: File
) {

    companion object {

        fun newText(plugin: Plugin, name: String): TextUtils {
            val logFile = plugin.dataFolder.resolve("${name}.log")
            if (!logFile.exists()) {
                logFile.parentFile.mkdirs()
                logFile.createNewFile()
            }
            return TextUtils(plugin, logFile)
        }

    }

    fun info(text: String) {
        info(listOf(text))
    }

    fun info(text: List<String>) {
        plugin.submitTask(async = true) {
            synchronized(logFile) {
                FileWriter(logFile, true).use { writer ->
                    text.forEach {
                        writer.write("[${Date().formatYMDHMS()}] $it\n")
                    }
                }
            }
        }
    }

}