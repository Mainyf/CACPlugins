package io.github.mainyf.worldsettings.hooks.mythic

import io.github.mainyf.worldsettings.getWorldSetting
import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import io.lumine.mythic.core.skills.SkillCondition

class AllowPlayerCondition(line: String) : SkillCondition(line), IEntityCondition {

    override fun check(entity: AbstractEntity): Boolean {
        if (!entity.isPlayer) return true
        val settings = getWorldSetting(entity.bukkitEntity.location) ?: return true

        return settings.pvp
    }

}