@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.itemmanager

import dev.lone.itemsadder.api.CustomStack
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.exts.getAction
import io.github.mainyf.newmclib.exts.getSection
import io.github.mainyf.newmclib.exts.saveResourceToFileAsConfiguration
import org.apache.commons.lang3.EnumUtils
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

object ConfigIM {

    var debug = false
    var iaItemAutoUpdateInfo: MultiAction? = null
    val iaItemAutoUpdate = mutableSetOf<String>()

    var repairSuccess: MultiAction? = null
    var repairAllSuccess: MultiAction? = null
    var notAllowedRepairMsg: MultiAction? = null
    var notAllowedRenameMsg: MultiAction? = null

    val iaItemSkillItems = mutableMapOf<String, IaItemAutoUpdateConfig>()

    lateinit var mainConfig: ConfigurationSection

    fun load() {
        mainConfig = ItemManager.INSTANCE.saveResourceToFileAsConfiguration("config.yml")
        loadMainConfig()
    }

    private fun loadMainConfig() {
        debug = mainConfig.getBoolean("debug", false)
        iaItemAutoUpdateInfo = mainConfig.getAction("iaItemAutoUpdate.info")
        iaItemAutoUpdate.clear()
        iaItemAutoUpdate.addAll(mainConfig.getStringList("iaItemAutoUpdate.item"))

        repairSuccess = mainConfig.getAction("repair.repairSuccess")
        repairAllSuccess = mainConfig.getAction("repair.repairAllSuccess")
        notAllowedRepairMsg = mainConfig.getAction("repair.notAllowedRepairMsg")
        notAllowedRenameMsg = mainConfig.getAction("repair.notAllowedRenameMsg")

        iaItemSkillItems.clear()
        val iaItemAutoUpdatesSect = mainConfig.getSection("iaItems")
        iaItemAutoUpdatesSect.getKeys(false).forEach loop1@{ iaItemName ->
            val iaItemAutoUpdateSect = iaItemAutoUpdatesSect.getSection(iaItemName)
            val namespaceID = iaItemAutoUpdateSect.getString("namespaceID")!!
            val triggersSect = iaItemAutoUpdateSect.getSection("trigger")
            val allowRepair = iaItemAutoUpdateSect.getBoolean("allowRepair")
            val allowRename = iaItemAutoUpdateSect.getBoolean("allowRename")
            val pveDamage = iaItemAutoUpdateSect.getDouble("pveDamage", 1.0)
            val map = mutableMapOf<ItemTriggerType, TriggerConfig>()
            triggersSect.getKeys(false).forEach {
                val itemTrigger = EnumUtils.getEnumIgnoreCase(ItemTriggerType::class.java, it)
                if (itemTrigger == null) {
                    ItemManager.LOGGER.warn("触发器 $it 不存在，检查配置")
                    return@forEach
                }
                map[itemTrigger] = TriggerConfig(
                    triggersSect.getString("${it}.mmSkill")!!,
                    triggersSect.getInt("${it}.itemDurabilityDamage", 0),
                    triggersSect.getBoolean("${it}.cancelEvent", false),
                )
            }
            iaItemSkillItems[namespaceID] = IaItemAutoUpdateConfig(
                namespaceID,
                allowRepair,
                allowRename,
                pveDamage,
                map
            )
        }
    }

    fun hasAllowRepair(item: ItemStack): Boolean {
        val cStack = CustomStack.byItemStack(item) ?: return true
        val namespaceID = cStack.namespacedID
        if (!iaItemSkillItems.containsKey(namespaceID)) return true
        val iaItem = iaItemSkillItems[namespaceID]!!
        return iaItem.allowRepair
    }

    fun hasAllowRename(item: ItemStack): Boolean {
        val cStack = CustomStack.byItemStack(item) ?: return true
        val namespaceID = cStack.namespacedID
        if (!iaItemSkillItems.containsKey(namespaceID)) return true
        val iaItem = iaItemSkillItems[namespaceID]!!
        return iaItem.allowRename
    }

    class IaItemAutoUpdateConfig(
        val namespaceID: String,
        val allowRepair: Boolean,
        val allowRename: Boolean,
        val pveDamage: Double,
        val triggers: Map<ItemTriggerType, TriggerConfig>
    )

    class TriggerConfig(
        val skillName: String,
        val itemDurabilityDamage: Int,
        val cancelEvent: Boolean
    )

    enum class ItemTriggerType {
        ATTACK,
        LEFT_CLICK,
        RIGHT_CLICK
    }

}