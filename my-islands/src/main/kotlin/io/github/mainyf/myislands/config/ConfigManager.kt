@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.myislands.config

import io.github.mainyf.myislands.MyIslands
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.util.Vector

object ConfigManager {

    private lateinit var mainConfig: FileConfiguration
    val schematicMap = mutableMapOf<String, PlotSchematicConfig>()

    fun load() {
        MyIslands.INSTANCE.saveDefaultConfig()
        MyIslands.INSTANCE.reloadConfig()
        mainConfig = MyIslands.INSTANCE.config
        val schematicListSection = mainConfig.getConfigurationSection("schematics")!!
        schematicListSection.getKeys(false).forEach { schematicKey ->
            val schematicSection = schematicListSection.getConfigurationSection(schematicKey)!!
            val uiName = schematicSection.getString("ui.name")!!
            val uiLore = schematicSection.getStringList("ui.lore")
            val core = schematicSection.getString("core")!!.let {
                val pair = it.split(",")
                val x = pair[0].toDouble()
                val y = pair[1].toDouble()
                val z = pair[2].toDouble()
                Vector(x, y, z)
            }
            schematicMap[schematicKey] = PlotSchematicConfig(
                schematicKey,
                PlotSchematicUIConfig(uiName, uiLore),
                core
            )
        }
    }

    class PlotSchematicConfig(
        val name: String,
        val ui: PlotSchematicUIConfig,
        val core: Vector
    )

    class PlotSchematicUIConfig(
        val name: String,
        val lore: List<String>
    )

}