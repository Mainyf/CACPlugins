//package io.github.mainyf.itemenchantplus.storage
//
//import io.github.mainyf.newmclib.storage.BaseModel
//import java.util.*
//
//class StorageIEP {
//
//    private val stageLevel = arrayOf(
//        100,
//        200,
//        300
//    )
//
//    fun setLevelAndExp(uuid: UUID, stage: Int, level: Int, exp: Double) {
//        var data = storage.findAtSync(uuid)
//        if (data == null) {
//            data = SkillData(stage, level, exp).apply {
//                this.id = uuid
//            }
//            handleSkillLevel(data)
//            storage.add(data)
//        } else {
//            data.stage = stage
//            data.level = level
//            data.exp = exp
//            handleSkillLevel(data)
//            storage.update(uuid, data)
//        }
//    }
//
//    fun addExp(uuid: UUID, value: Double): SkillData {
//        var data = storage.findAtSync(uuid)
//        if (data == null) {
//            data = SkillData(0, 0, value).apply {
//                this.id = uuid
//            }
//            handleSkillLevel(data)
//            storage.add(data)
//        } else {
//            data.exp += value
//            handleSkillLevel(data)
//            val maxExp = ConfigManager.getLevelMaxExp(data.level)
//            if (data.exp > maxExp) {
//                data.exp = maxExp
//            }
//            storage.update(uuid, data)
//        }
//        return data
//    }
//
//    fun getSkillData(uuid: UUID): SkillData {
//        var data = storage.findAtSync(uuid)
//        if (data == null) {
//            data = SkillData(0, 1, 0.0)
//            data.id = uuid
//            storage.add(data)
//        }
//        return data
//    }
//
//    fun exists(uuid: UUID): Boolean {
//        return storage.findAtSync(uuid) != null
//    }
//
//    fun getStageMaxLevel(stage: Int): Int {
//        return stageLevel.getOrNull(stage) ?: 300
//    }
//
//    private fun handleSkillLevel(data: SkillData) {
//        if (data.stage >= 3) return
//        var maxExp = ConfigManager.getLevelMaxExp(data.level)
//        val maxLevel = stageLevel[data.stage]
//        while (data.exp >= maxExp && data.level < maxLevel) {
//            data.exp -= maxExp
//            data.level++
//            maxExp = ConfigManager.getLevelMaxExp(data.level)
//        }
//    }
//
//    data class SkillData(
//        var stage: Int = 0,
//        var level: Int = 0,
//        var exp: Double = 0.0
//    ) : BaseModel()
//
//}