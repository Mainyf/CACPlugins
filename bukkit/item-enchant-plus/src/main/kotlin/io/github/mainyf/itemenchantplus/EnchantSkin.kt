package io.github.mainyf.itemenchantplus

import io.github.mainyf.itemenchantplus.config.EnchantSkinConfig
import org.joda.time.DateTime

class EnchantSkin(
    val skinConfig: EnchantSkinConfig,
    val stage: Int,
    val expiredTime: DateTime? = null,
    val hasOwn: Boolean = true
)