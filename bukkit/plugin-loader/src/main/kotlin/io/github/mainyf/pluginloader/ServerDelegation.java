package io.github.mainyf.pluginloader;

import java.io.File;

public class ServerDelegation {

    private final File pluginsFolder;

    public ServerDelegation(File pluginsFolder) {
        this.pluginsFolder = pluginsFolder;
    }

    public File getPluginsFolder() {
        return pluginsFolder;
    }

}
