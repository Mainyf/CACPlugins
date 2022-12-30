package io.github.mainyf.toolsplugin.module

import io.github.mainyf.newmclib.exts.deserialize
import io.github.mainyf.newmclib.exts.toComp
import io.github.mainyf.toolsplugin.config.ConfigTP
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent

object ChunkLogger : Listener {

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        if (!ConfigTP.chunkLoggerDebug) return
        Bukkit.broadcast(
            "世界: ${event.chunk.world.name} 区块已加载"
                .deserialize()
                .append(
                    "x: ${event.chunk.x} y: ${event.chunk.z}"
                        .deserialize()
                        .hoverEvent(
                            HoverEvent.showText("点击复制".toComp())
                        )
                        .clickEvent(ClickEvent.copyToClipboard("${event.chunk.x} ${event.chunk.z}"))
                )
        )
    }

}