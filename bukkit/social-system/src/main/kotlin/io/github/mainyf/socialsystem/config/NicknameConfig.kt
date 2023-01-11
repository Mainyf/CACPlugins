package io.github.mainyf.socialsystem.config

import io.github.mainyf.newmclib.config.action.MultiAction

data class NicknameConfig(
    val permission: String,
    val modifyCooldown: Int,
    val minLength: Int,
    val maxLength: Int,
    val cost: Double,
    val sensitiveWord: List<String>,
    val tipsPeriod: Long,
    val tipsAction: MultiAction?,
    val minLengthAction: MultiAction?,
    val maxLengthAction: MultiAction?,
    val chineseAction: MultiAction?,
    val sensitiveAction: MultiAction?,
    val quitAction: MultiAction?,
    val successAction: MultiAction?,
    val costLackAction: MultiAction?,
    val nicknameCooldownAction: MultiAction?,
    val repatNicknameAction: MultiAction?
)