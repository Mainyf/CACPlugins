@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.itemmanager

import io.github.mainyf.newmclib.exts.getSection
import io.github.mainyf.newmclib.exts.saveResourceToFileAsConfiguration
import org.apache.commons.lang3.EnumUtils
import org.bukkit.configuration.ConfigurationSection

object ConfigIM {

    val iaItemAutoUpdateItems = mutableMapOf<String, IaItemAutoUpdateConfig>()

    lateinit var mainConfig: ConfigurationSection

    fun load() {
        mainConfig = ItemManager.INSTANCE.saveResourceToFileAsConfiguration("config.yml")
        loadMainConfig()
    }

    private fun loadMainConfig() {
        iaItemAutoUpdateItems.clear()
        val iaItemAutoUpdatesSect = mainConfig.getSection("iaItemAutoUpdate")
        iaItemAutoUpdatesSect.getKeys(false).forEach loop1@{ iaItemName ->
            val iaItemAutoUpdateSect = iaItemAutoUpdatesSect.getSection(iaItemName)
            val namespaceID = iaItemAutoUpdateSect.getString("namespaceID")!!
            val update = iaItemAutoUpdateSect.getBoolean("update", true)
            val triggersSect = iaItemAutoUpdateSect.getSection("trigger")
            val map = mutableMapOf<ItemTrigger, String>()
            triggersSect.getKeys(false).forEach {
                val itemTrigger = EnumUtils.getEnumIgnoreCase(ItemTrigger::class.java, it)
                if (itemTrigger == null) {
                    ItemManager.LOGGER.warn("触发器 $it 不存在，检查配置")
                    return@forEach
                }
                map[itemTrigger] = triggersSect.getString("${it}.mmSkill")!!
            }
            iaItemAutoUpdateItems[namespaceID] = IaItemAutoUpdateConfig(
                namespaceID,
                update,
                map
            )
        }
    }

    class IaItemAutoUpdateConfig(
        val namespaceID: String,
        val update: Boolean,
        val triggers: Map<ItemTrigger, String>
    )

    enum class ItemTrigger {
        ATTACK,
        LEFT_CLICK,
        RIGHT_CLICK
    }

}