package io.github.mainyf.itemskillsplus.storage

import io.github.mainyf.itemskillsplus.config.ConfigManager
import io.github.mainyf.newmclib.storage.BaseModel
import io.github.mainyf.newmclib.storage.GeneralStorage
import io.github.mainyf.newmclib.storage.GeneralStorage.Companion.of
import java.util.UUID

object StorageManager {

    private lateinit var storage: GeneralStorage<SkillData>
    private val stageLevel = arrayOf(
        100,
        200,
        300
    )

    fun init() {
        storage = of(SkillData::class) {
            jdbcUrl = "jdbc:mysql://${ConfigManager.host}:${ConfigManager.port}/${ConfigManager.databaseName}"
            username = ConfigManager.user
            password = ConfigManager.password
            driverClassName = "com.mysql.jdbc.Driver"
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
    }

    fun setLevelAndExp(uuid: UUID, stage: Int, level: Int, exp: Double) {
        var data = storage.find(uuid)
        if (data == null) {
            data = SkillData(stage, level, exp).apply {
                this.id = uuid
            }
            handleSkillLevel(data)
            storage.add(data)
        } else {
            data.stage = stage
            data.level = level
            data.exp = exp
            handleSkillLevel(data)
            storage.update(uuid, data)
        }
    }

    fun addExp(uuid: UUID, value: Double): SkillData {
        var data = storage.find(uuid)
        if (data == null) {
            data = SkillData(0, 0, value).apply {
                this.id = uuid
            }
            handleSkillLevel(data)
            storage.add(data)
        } else {
            data.exp += value
            handleSkillLevel(data)
            val maxExp = ConfigManager.getLevelMaxExp(data.level)
            if (data.exp > maxExp) {
                data.exp = maxExp
            }
            storage.update(uuid, data)
        }
        return data
    }

    fun getSkillData(uuid: UUID): SkillData {
        var data = storage.find(uuid)
        if (data == null) {
            data = SkillData(0, 1, 0.0)
            data.id = uuid
            storage.add(data)
        }
        return data
    }

    fun exists(uuid: UUID): Boolean {
        return storage.find(uuid) != null
    }

    fun getStageMaxLevel(stage: Int): Int {
        return stageLevel.getOrNull(stage - 1) ?: 300
    }

    private fun handleSkillLevel(data: SkillData) {
        if (data.stage >= 3) return
        var maxExp = ConfigManager.getLevelMaxExp(data.level)
        val maxLevel = stageLevel[data.stage - 1]
        while (data.exp >= maxExp && data.level < maxLevel) {
            data.exp -= maxExp
            data.level++
            maxExp = ConfigManager.getLevelMaxExp(data.level)
        }
    }

    fun destory() {
        storage.close()
    }

    data class SkillData(
        var stage: Int = 0,
        var level: Int = 0,
        var exp: Double = 0.0
    ) : BaseModel()

}
