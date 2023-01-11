package io.github.mainyf.celebration.config

import io.github.mainyf.celebration.Celebration
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.exts.createFileConfiguration
import io.github.mainyf.newmclib.exts.getAction
import io.github.mainyf.newmclib.exts.getSection
import org.bukkit.configuration.file.FileConfiguration

object ConfigCEL {

    private lateinit var mainConfig: FileConfiguration
    val togethers = mutableMapOf<String, TogethersReward>()

    fun load() {
        kotlin.runCatching {
            mainConfig = Celebration.INSTANCE.createFileConfiguration("config.yml")

            loadMainConfig()

        }.onFailure {
            Celebration.INSTANCE.slF4JLogger.error("加载配置时出现错误", it)
        }
    }

    private fun loadMainConfig() {
        togethers.clear()
        val togethersSect = mainConfig.getSection("togethers")
        togethersSect.getKeys(false).forEach { togetherName ->
            val togetherSect = togethersSect.getSection(togetherName)
            val selfGive = togetherSect.getBoolean("selfGive", false)
            val duration = togetherSect.getLong("duration")
            val msgLength = togetherSect.getInt("msgLength")
            val chinese = togetherSect.getBoolean("chinese")
            val startActions = togetherSect.getAction("startActions")
            val reward = togetherSect.getAction("reward")
            togethers[togetherName] = TogethersReward(
                togetherName,
                selfGive,
                duration,
                msgLength,
                chinese,
                startActions,
                reward
            )
        }
    }

}

data class TogethersReward(
    val name: String,
    val selfGive: Boolean,
    val duration: Long,
    val msgLength: Int,
    val chinese: Boolean,
    val startActions: MultiAction?,
    val reward: MultiAction?
)