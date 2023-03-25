package io.github.mainyf.miningcheck.config

import io.github.mainyf.miningcheck.MiningCheck
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.exts.getAction
import io.github.mainyf.newmclib.exts.getSection
import io.github.mainyf.newmclib.exts.saveResourceToFileAsConfiguration
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import java.lang.RuntimeException

object ConfigMC {

    private lateinit var mainConfigFile: FileConfiguration

    var enable = true
    var debug = false
    val worlds = mutableMapOf<String, WorldConfig>()

    fun load() {
        kotlin.runCatching {
            mainConfigFile = MiningCheck.INSTANCE.saveResourceToFileAsConfiguration("config.yml")
            loadMainConfig()
        }.onFailure {
            MiningCheck.LOGGER.info("加载配置时出现错误")
            it.printStackTrace()
        }
    }

    private fun loadMainConfig() {
        enable = mainConfigFile.getBoolean("enable", enable)
        debug = mainConfigFile.getBoolean("debug", debug)
        worlds.clear()
        val worldsSect = mainConfigFile.getSection("worlds")
        for (worldName in worldsSect.getKeys(false)) {
            val worldSect = worldsSect.getSection(worldName)
            kotlin.runCatching {
                val actionResetTime = worldSect.getLong("actionResetTime")
                val countingTime = worldSect.getLong("countingTime")
                val countMax = worldSect.getInt("countMax")
                val caveIgnoreCountTime = worldSect.getLong("caveIgnoreCountTime", 2)
                val expandIgnoreSecound = worldSect.getLong("expandIgnoreSecound")
                val expandStage3Ratio = worldSect.getDouble("expandStage3Ratio")
                val expandPlusRatio = worldSect.getDouble("expandPlusRatio")
                val countActions = worldSect.getSection("countActions").let { countActionsSect ->
                    countActionsSect.getKeys(false).associateWith {
                        val num = countActionsSect.getInt("${it}.num")
                        val action = countActionsSect.getAction("${it}.action")
                        CountAction(num, action)
                    }
                }
                val blocks = worldSect.getSection("blocks").let { blocksSect ->
                    blocksSect.getKeys(false).associate {
                        val material = EnumUtils.getEnum(Material::class.java, it)
                            ?: throw RuntimeException("$it 不是一个有效的类型")
                        material to blocksSect.getInt(it)
                    }
                }
                worlds[worldName] = WorldConfig(
                    worldName,
                    actionResetTime,
                    countingTime,
                    countMax,
                    caveIgnoreCountTime,
                    expandIgnoreSecound,
                    expandStage3Ratio,
                    expandPlusRatio,
                    countActions,
                    blocks
                )
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    class WorldConfig(
        val worldName: String,
        val actionResetTime: Long,
        val countingTime: Long,
        val countMax: Int,
        val caveIgnoreCountTime: Long,
        val expandIgnoreSecound: Long,
        val expandStage3Ratio: Double,
        val expandPlusRatio: Double,
        val countActions: Map<String, CountAction>,
        val blocks: Map<Material, Int>
    )

    data class CountAction(
        val num: Int,
        val action: MultiAction?
    )

}