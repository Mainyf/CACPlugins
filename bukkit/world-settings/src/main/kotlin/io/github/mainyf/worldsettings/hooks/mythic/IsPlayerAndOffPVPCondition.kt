package io.github.mainyf.worldsettings.hooks.mythic

import io.github.mainyf.worldsettings.getWorldSetting
import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import io.lumine.mythic.api.skills.conditions.ISkillMetaCondition
import io.lumine.mythic.core.skills.SkillCondition
import org.bukkit.entity.Player

class IsPlayerAndOffPVPCondition(line: String) : SkillCondition(line), IEntityCondition {

    override fun check(entity: AbstractEntity): Boolean {
        if (!entity.isPlayer) return false
        val settings = getWorldSetting(entity.bukkitEntity.location) ?: return false

        return !settings.pvp
    }

}