package io.github.mainyf.pluginloader;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

public class PluginRepository implements Listener {

    private static File repositoryFolder;
    private static String serverId;

    public static void init() {
        PluginLoader.INSTANCE.saveDefaultConfig();
        PluginLoader.INSTANCE.reloadConfig();

        ConfigurationSection config = PluginLoader.INSTANCE.getConfig();

        String repositoryURL = config.getString("repositoryURL");
        repositoryFolder = new File(repositoryURL);
        serverId = config.getString("serverId");
    }

    public static void loadPlugin() {
        File unused = new File(repositoryFolder, "._");
        if (!unused.exists()) {
            unused.mkdirs();
        }
        File commonPlugins = new File(repositoryFolder, ".common");
        File serverPlugins = new File(repositoryFolder, serverId);
        List<File> pendingLoadPlugins = new ArrayList<>();
        Arrays.stream(commonPlugins.listFiles()).forEach(file -> {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                pendingLoadPlugins.add(file);
            }
        });
        Arrays.stream(serverPlugins.listFiles()).forEach(file -> {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                pendingLoadPlugins.add(file);
            }
        });

        SimplePluginManager pluginManager = (SimplePluginManager) Bukkit.getServer().getPluginManager();
        HashMap<Pattern, org.bukkit.plugin.PluginLoader> fileAssociations = getFieldValue(pluginManager, "fileAssociations");
        org.bukkit.plugin.PluginLoader pluginLoader = fileAssociations.values().stream().findFirst().get();


//        new ByteBuddy()
//                .subclass(Server.class)
//                .method(ElementMatchers.any())
//                .intercept(MethodDelegation.to(new ServerDelegation()))
        Plugin[] plugins = pluginManager.loadPlugins(unused, pendingLoadPlugins);
        System.out.println();
    }

    @EventHandler
    public static void onServerLoad(ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) {
            return;
        }
        loadPlugin();
    }

    private static <T> T getFieldValue(Object obj, String fieldName) {
        Field field;
        try {
            field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}