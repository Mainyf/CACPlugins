package io.github.mainyf.loginsettings.config

import io.github.mainyf.newmclib.config.action.MultiAction

data class BindStageConfig(
    val actions: MultiAction? = null,
    val nextStage: MultiAction? = null,
    val qqRegex: Regex,
    val formatError: MultiAction? = null,
    val qqAlreadyBind: MultiAction? = null
)

data class CodeStageConfig(
    val actions: MultiAction? = null,
    val veritySuccess: String? = null,
    val loginFinish: MultiAction? = null,
    val registerFinish: MultiAction? = null
)

data class ResetPasswdConfig(
    val actions: MultiAction? = null,
    val nextStage: MultiAction? = null,
    val veritySuccess: String? = null,
    val sendNewPasswd: MultiAction? = null,
    val confirmNewPasswd: MultiAction? = null,
    val passwdDiscrepancy: MultiAction? = null,
    val finish: MultiAction? = null
)