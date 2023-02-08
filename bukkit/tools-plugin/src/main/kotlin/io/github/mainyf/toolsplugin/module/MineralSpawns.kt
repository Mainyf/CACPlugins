package io.github.mainyf.toolsplugin.module

import io.github.mainyf.newmclib.exts.asWorld
import io.github.mainyf.toolsplugin.config.ConfigTP
import org.bukkit.Bukkit
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import java.util.*

object MineralSpawns : BlockPopulator() {

    private val populators = mutableSetOf<BlockPopulatorImpl>()

    fun init() {
        initGenerator()
    }

    fun initGenerator() {
        Bukkit.getWorlds().forEach { world ->
            world.populators.removeAll(this.populators)
        }
        populators.clear()
        ConfigTP.customBlockGenerators.values.forEach { generator ->
            populators.add(BlockPopulatorImpl(generator))
        }
        this.populators.forEach { populator ->
            populator.config.worlds.forEach {
                it.asWorld()!!.populators.add(this)
            }
        }
    }

    class BlockPopulatorImpl(val config: ConfigTP.CustomBlockGenerator) : BlockPopulator() {

    }

    override fun populate(
        worldInfo: WorldInfo,
        random: Random,
        chunkX: Int,
        chunkZ: Int,
        limitedRegion: LimitedRegion
    ) {

    }

}