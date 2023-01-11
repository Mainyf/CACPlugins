package io.github.mainyf.questextension.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class ManualCompleteQuestEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    private final String name;

    public ManualCompleteQuestEvent(@NotNull final Player player, final String name) {
        super(player);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
