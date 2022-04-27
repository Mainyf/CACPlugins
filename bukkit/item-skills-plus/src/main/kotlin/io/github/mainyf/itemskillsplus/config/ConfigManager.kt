package io.github.mainyf.itemskillsplus.config

import com.udojava.evalex.Expression
import dev.lone.itemsadder.api.CustomStack
import io.github.mainyf.itemskillsplus.ItemSkillsPlus
import io.github.mainyf.itemskillsplus.isEmpty
import io.github.mainyf.newmclib.config.PlayParser
import io.github.mainyf.newmclib.config.play.MultiPlay
import io.github.mainyf.newmclib.exts.colored
import io.github.mainyf.newmclib.exts.toMap
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.yaml.snakeyaml.Yaml
import java.io.StringReader
import java.util.*


object ConfigManager {

    private lateinit var mainConfig: FileConfiguration
    private lateinit var skinConfig: FileConfiguration
    private lateinit var menuConfig: FileConfiguration

    private val skinMap = mutableMapOf<String, Skin>()
    lateinit var menuSlotConfig: MenuSlotConfig
        private set

//    lateinit var initMenuTipsName: String
//    lateinit var initMenuTipsLore: List<String>
//    lateinit var upgradeMenuTipsName: String
//    lateinit var upgradeMenuTipsLore: List<String>

    private lateinit var levelExpression: String
    private var defaultEntityExp = 0.0
    private lateinit var expEntityMap: Map<String, Double>
    private var defaultBlockExp = 0.0
    private lateinit var expBlockMap: Map<String, Double>

    private var itemaddersNamespace = "itemskills"

    var expandEnable = false
    val expandItemType = Material.NETHERITE_PICKAXE
    lateinit var expandName: String
    lateinit var expandDesc: List<String>
    lateinit var expandAllowBlocks: List<String>
    lateinit var expandSkin: Skin
    lateinit var expandUpgradeMaterials: List<List<Pair<String, Int>>>

    var luckEnable = false
    val luckItemType = Material.NETHERITE_PICKAXE
    lateinit var luckName: String
    var luckMax: Int = 25
    lateinit var luckDesc: List<String>
    lateinit var luckAllowBlocks: List<String>
    lateinit var luckSkin: Skin
    lateinit var luckUpgradeMaterials: List<List<Pair<String, Int>>>

    var sharpEnable = false
    lateinit var sharpName: String
    lateinit var sharpDesc: List<String>
    lateinit var sharpDamageForm: List<Double>
    lateinit var sharpSkin: Skin
    lateinit var sharpUpgradeMaterials: List<List<Pair<String, Int>>>

    var powerEnable = false
    lateinit var powerName: String
    var powerSpeed: Float = 2f
    lateinit var powerDesc: List<String>
    lateinit var powerDamageForm: List<Double>
    lateinit var powerSkin: Skin
    lateinit var powerUpgradeMaterials: List<List<Pair<String, Int>>>

    @JvmStatic
    fun load() {
        kotlin.runCatching {
            ItemSkillsPlus.INSTANCE.saveDefaultConfig()
            ItemSkillsPlus.INSTANCE.reloadConfig()
            val skinFile = ItemSkillsPlus.INSTANCE.dataFolder.resolve("skins.yml")
            if (!skinFile.exists()) {
                ItemSkillsPlus.INSTANCE.saveResource("skins.yml", false)
            }
            val menuFile = ItemSkillsPlus.INSTANCE.dataFolder.resolve("menu.yml")
            if (!menuFile.exists()) {
                ItemSkillsPlus.INSTANCE.saveResource("menu.yml", false)
            }
            mainConfig = ItemSkillsPlus.INSTANCE.config
            skinConfig = YamlConfiguration.loadConfiguration(skinFile)
            menuConfig = YamlConfiguration.loadConfiguration(menuFile)
            loadSkinConfig()
            loadMenuConfig()
            loadMainConfig()
        }.onFailure {
            ItemSkillsPlus.INSTANCE.slF4JLogger.error("加载配置时出现错误", it)
        }
    }

    private fun loadMainConfig() {
//        initMenuTipsName = mainConfig.getString("initMenuTipsName")!!
//        initMenuTipsLore = mainConfig.getStringList("initMenuTipsLore")
//        upgradeMenuTipsName = mainConfig.getString("upgradeMenuTipsName")!!
//        upgradeMenuTipsLore = mainConfig.getStringList("upgradeMenuTipsLore")

        levelExpression = mainConfig.getString("exp.level")!!
        expEntityMap = mainConfig.getStringList("exp.entitys")
            .toMap { it.split(":").run { this[0].lowercase(Locale.getDefault()) to this[1].toDouble() } }
        defaultEntityExp = expEntityMap.getOrDefault("other", 0.0)
        expEntityMap = expEntityMap.filter { it.key != "other" }

        expBlockMap = mainConfig.getStringList("exp.blocks")
            .toMap { it.split(":").run { this[0].lowercase(Locale.getDefault()) to this[1].toDouble() } }
        defaultBlockExp = expBlockMap.getOrDefault("other", 0.0)
        expBlockMap = expBlockMap.filter { it.key != "other" }

        itemaddersNamespace = mainConfig.getString("itemaddersNamespace") ?: itemaddersNamespace

        mainConfig.getConfigurationSection("equipments.expand")!!.let { section ->
            expandEnable = section.getBoolean("enabled")
            expandName = section.getString("name")!!
            expandDesc = section.getStringList("description")
            expandAllowBlocks = section.getStringList("allowBlocks")
            expandSkin = skinMap[section.getString("skin")]!!
            expandUpgradeMaterials = getUpgradeMaterial(section)
        }

        mainConfig.getConfigurationSection("equipments.luck")!!.let { section ->
            luckEnable = section.getBoolean("enabled")
            luckName = section.getString("name")!!
            luckMax = section.getInt("max")
            luckDesc = section.getStringList("description")
            luckAllowBlocks = section.getStringList("allowBlocks")
            luckSkin = skinMap[section.getString("skin")]!!
            luckUpgradeMaterials = getUpgradeMaterial(section)
        }

        mainConfig.getConfigurationSection("equipments.sharp")!!.let { section ->
            sharpEnable = section.getBoolean("enabled")
            sharpName = section.getString("name")!!
            sharpDesc = section.getStringList("description")
            sharpDamageForm = section.getString("damageForm")!!.split(",").map { it.trim().toDouble() / 100.0 }
            sharpSkin = skinMap[section.getString("skin")]!!
            sharpUpgradeMaterials = getUpgradeMaterial(section)
        }

        mainConfig.getConfigurationSection("equipments.power")!!.let { section ->
            powerEnable = section.getBoolean("enabled")
            powerName = section.getString("name")!!
            powerSpeed = section.getString("speed")!!.toFloatOrNull() ?: 2f
            powerDesc = section.getStringList("description")
            powerDamageForm = section.getString("damageForm")!!.split(",").map { it.trim().toDouble() / 100.0 }
            powerSkin = skinMap[section.getString("skin")]!!
            powerUpgradeMaterials = getUpgradeMaterial(section)
        }
    }

    private fun loadSkinConfig() {
        skinMap.clear()
        skinConfig.getKeys(false).forEach { skinKey ->
            val skinSection = skinConfig.getConfigurationSection(skinKey)!!
            val enabled = skinSection.getBoolean("enable")
            val equipType = skinSection.getString("equipType")!!
            skinMap[skinKey] = Skin(skinKey, enabled, equipType, skinSection.getList("skin")!!.map { map ->
                val equipConfig = YamlConfiguration.loadConfiguration(StringReader(Yaml().dump(map)))
                val customModelData = equipConfig.getInt("customModelData")
                val effectSection = equipConfig.getConfigurationSection("effect")!!
                val effectMap = mutableMapOf<String, SkinEffect>()
                effectSection.getKeys(false).forEach { effectKey ->
                    val type = EffectTriggerType.valueOf(
                        effectSection.getString("${effectKey}.type")!!.uppercase(Locale.getDefault())
                    )
                    val multiPlay = MultiPlay()
                    effectSection.getStringList("${effectKey}.value").forEach effectValueLoop@{
                        val play = PlayParser.parsePlay(it)
                        if (play == null) {
                            println("[ItemSkillsPlus] $it 解析错误")
                            return@effectValueLoop
                        }
                        multiPlay.addPlay(play)
                    }
                    effectMap[effectKey] = SkinEffect(type, multiPlay)
                }
                EquipmentSkin(customModelData, effectMap)
            })
        }
    }

    private fun loadMenuConfig() {
        val initItemSkillTitle1 = menuConfig.getString("initItemSkillTitle1")!!.colored()
        val initItemSkillTitle2 = menuConfig.getString("initItemSkillTitle2")!!.colored()
        val initItemSkillTitle3 = menuConfig.getString("initItemSkillTitle3")!!.colored()

        val upgradeItemSkillTitle1 = menuConfig.getString("upgradeItemSkillTitle1")!!.colored()
        val upgradeItemSkillTitle2 = menuConfig.getString("upgradeItemSkillTitle2")!!.colored()

        val skills = mutableMapOf<String, ItemDisplayConfig>()
        val skillListSect = menuConfig.getConfigurationSection("skillList")!!
        skillListSect.getKeys(false).forEach {
            skills[it] = skillListSect.getConfigurationSection(it)!!.asItemDisplay()
        }
        val equipSlot = menuConfig.getConfigurationSection("equipSlot")!!.let {
            val initSlots = it.getIntegerList("initSlots")
            val upgradeSlots = it.getIntegerList("upgradeSlots")
            val itemDisplay = it.asItemDisplay()
            SlotConfig(initSlots, upgradeSlots, itemDisplay)
        }

        val enchantSlot = menuConfig.getConfigurationSection("enchantSlot")!!.let {
            val initSlots = it.getIntegerList("initSlots")
            val upgradeSlots = it.getIntegerList("upgradeSlots")
            val initItemDisplay = it.getConfigurationSection("init")!!.asItemDisplay()
            val upgradeItemDisplay = it.getConfigurationSection("upgrade")!!.asItemDisplay()
            val enchantSkills = it.getConfigurationSection("skill")!!.let { skillSect ->
                val skillMap = mutableMapOf<String, List<ItemTypeWrapper>>()
                skillSect.getKeys(false).forEach { key ->
                    skillMap[key] = skillSect.getStringList(key).map { l ->
                        l.asItemTypeWrapper()
                    }
                }
                skillMap
            }
            EnchantSlot(initSlots, upgradeSlots, initItemDisplay, upgradeItemDisplay, enchantSkills)
        }

        val materialsOfAdequacySlot = menuConfig.getConfigurationSection("materialsOfAdequacySlot")!!.let {
            val initSlots = it.getIntegerList("initSlots")
            val upgradeSlots = it.getIntegerList("upgradeSlots")
            val default = it.getConfigurationSection("default")!!.asItemDisplay()
            val satisfied = it.getConfigurationSection("satisfied")!!.asItemDisplay()
            val unSatisfied = it.getConfigurationSection("unSatisfied")!!.asItemDisplay()
            MaterialsOfAdequacySlot(initSlots, upgradeSlots, default, satisfied, unSatisfied)
        }
        val materialsSlot = menuConfig.getConfigurationSection("materialsSlot")!!.let {
            val initSlots = it.getIntegerList("initSlots")
            val upgradeSlots = it.getIntegerList("upgradeSlots")
            SlotConfig(initSlots, upgradeSlots, ItemDisplayConfig.AIR)
        }
        val materialsCountSlot = menuConfig.getConfigurationSection("materialsCountSlot")!!.let {
            val initSlots = it.getIntegerList("initSlots")
            val upgradeSlots = it.getIntegerList("upgradeSlots")
            val default = it.getConfigurationSection("default")!!.asItemDisplay()
            val selectx32 = it.getConfigurationSection("selectx32")!!.asItemDisplay()
            val selectx64 = it.getConfigurationSection("selectx64")!!.asItemDisplay()
            val selectx128 = it.getConfigurationSection("selectx128")!!.asItemDisplay()
            val selectx192 = it.getConfigurationSection("selectx192")!!.asItemDisplay()
            MaterialsCountSlot(initSlots, upgradeSlots, default, selectx32, selectx64, selectx128, selectx192)
        }
        val completeSlot = menuConfig.getConfigurationSection("completeSlot")!!.let {
            val initSlots = it.getIntegerList("initSlots")
            val upgradeSlots = it.getIntegerList("upgradeSlots")
            val itemDisplay = it.asItemDisplay()
            SlotConfig(initSlots, upgradeSlots, itemDisplay)
        }
        menuSlotConfig = MenuSlotConfig(
            initItemSkillTitle1,
            initItemSkillTitle2,
            initItemSkillTitle3,
            upgradeItemSkillTitle1,
            upgradeItemSkillTitle2,
            skills,
            equipSlot,
            enchantSlot,
            materialsOfAdequacySlot,
            materialsSlot,
            materialsCountSlot,
            completeSlot
        )
    }

//    private fun ConfigurationSection.asSlotConfig(): SlotConfig {
//        val name = getString("name")?.colored()
//        val lore = getStringList("lore").map { t -> t.colored() }
//        return SlotConfig(name = name, lore = lore)
//    }

//    private fun ConfigurationSection.asMultiSlotConfig(): MultiGroupsSlotConfig {
//        val slots = getIntegerList("slots")
//        val groups = mutableMapOf<String, SlotConfig>()
//        getKeys(false).forEach { key ->
//            if (key != "slots") {
//                val type = Material.valueOf(getString("${key}.type")!!.uppercase())
//                val name = getString("${key}.name")?.colored()
//                groups[key] = SlotConfig(name = name, type = type)
//            }
//        }
//        return MultiGroupsSlotConfig(slots = slots, groups)
//    }

    private fun getUpgradeMaterial(configSection: ConfigurationSection): List<List<Pair<String, Int>>> {
        val lines = configSection.getStringList("upgradeMaterials")
        val rs = mutableListOf<List<Pair<String, Int>>>()
        lines.forEach { line ->
            val list = mutableListOf<Pair<String, Int>>()
            val pair = line.split(",")
            pair.map {
                val (id, amount) = it.split(":")
                list.add(id to amount.toInt())
            }
            rs.add(list)
        }
        return rs
    }

    fun getSkillDefaultSkinByName(name: String): Skin? {
        return when (name) {
            "expand" -> expandSkin
            "luck" -> luckSkin
            "sharp" -> sharpSkin
            "power" -> powerSkin
            else -> null
        }
    }

    fun getSkillOnlyItemTypeByName(name: String): Material {
        return when (name) {
            "expand" -> expandItemType
            "luck" -> luckItemType
            "sharp" -> Material.NETHERITE_SWORD
            "power" -> Material.BOW
            else -> expandItemType
        }
    }

    fun getUpgradeMaterialByName(name: String): List<List<Pair<String, Int>>> {
        return when (name) {
            "expand" -> expandUpgradeMaterials
            "luck" -> luckUpgradeMaterials
            "sharp" -> sharpUpgradeMaterials
            "power" -> powerUpgradeMaterials
            else -> expandUpgradeMaterials
        }
    }

    fun getItemDisplayNameByDataKey(dataKey: NamespacedKey): String {
        return when (dataKey.key) {
            "expand" -> expandName
            "luck" -> luckName
            "sharp" -> sharpName
            "power" -> powerName
            else -> expandName
        }
    }

    fun getItemDescByDataKey(dataKey: NamespacedKey): List<String> {
        return when (dataKey.key) {
            "expand" -> expandDesc
            "luck" -> luckDesc
            "sharp" -> sharpDesc
            "power" -> powerDesc
            else -> expandDesc
        }
    }

    fun getEntityExp(entity: Entity): Double {
        return expEntityMap[entity.type.getName()!!.lowercase(Locale.getDefault())] ?: defaultEntityExp
    }

    fun getBlockExp(block: Block): Double {
        return expBlockMap[block.type.name.lowercase(Locale.getDefault())] ?: defaultBlockExp
    }

    fun isExpandAllowBlock(material: Material): Boolean {
        val bName = material.name.lowercase(Locale.getDefault())
        return expandAllowBlocks.contains(bName)
    }

    fun isLuckAllowBlock(material: Material): Boolean {
        val bName = material.name.lowercase(Locale.getDefault())
        return luckAllowBlocks.contains(bName)
    }

    fun getSharpDamage(stage: Int): Double {
        return sharpDamageForm[stage]
    }

    fun getPowerDamage(stage: Int): Double {
        return powerDamageForm[stage]
    }

    fun getLevelMaxExp(level: Int): Double {
        return Expression(levelExpression).setVariable("level", level.toString()).eval().toDouble()
    }

    fun getItemByUPMaterialID(id: String): ItemStack {
        val cStack = CustomStack.getInstance(getFullID(id))
        if (cStack != null) {
            return cStack.itemStack
        }
        return ItemStack(kotlin.runCatching { Material.valueOf(id.uppercase()) }.getOrNull() ?: Material.AIR)
    }

    fun hasMatchMaterial(id: String, itemStack: ItemStack?): Boolean {
        if (itemStack.isEmpty()) return false
        val cStack = CustomStack.byItemStack(itemStack)
        if (cStack != null && cStack.namespacedID == getFullID(id)) {
            return true
        }
        return itemStack!!.type.name == id.uppercase()
    }

    fun getFullID(id: String): String {
        return "${itemaddersNamespace}:${id}"
    }

}

