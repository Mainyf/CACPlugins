package io.github.mainyf.pluginloader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginLoader extends JavaPlugin {

    public static Logger LOGGER = LogManager.getLogger("PluginLoader");

    public static PluginLoader INSTANCE;

    @Override
    public void onEnable() {
        INSTANCE = this;

        PluginRepository.init();
        Bukkit.getServer().getPluginManager().registerEvents(new PluginRepository(), this);
    }

}