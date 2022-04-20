rootProject.name = "CACPlugins"

val rootDir = rootProject.projectDir
rootDir.resolve("bukkit").listFiles()?.forEach {
    if (it.isDirectory) {
        include("bukkit:${it.name}")
    }
}

rootDir.resolve("bungeecord").listFiles()?.forEach {
    if (it.isDirectory) {
        include("bungeecord:${it.name}")
    }
}