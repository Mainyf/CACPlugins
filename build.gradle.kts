import java.util.regex.Pattern

plugins {
    id("java")
    kotlin("jvm") version "1.7.10"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

allprojects {
    repositories {
        mavenLocal()
        maven {
            url = uri("https://papermc.io/repo/repository/maven-public")
        }
        maven {
            url = uri("https://hub.spigotmc.org/nexus/content/repositories/public")
        }
        maven {
            url = uri("https://repo.codemc.org/repository/maven-releases")
        }
        mavenCentral()
        jcenter()
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
}

subprojects {

    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
        if (hasBukkit(this@subprojects)) {
            plugin("net.minecrell.plugin-yml.bukkit")
        }
    }

    version = "1.0"

    sourceSets.main.get().java.srcDirs("src/main/kotlin")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    kotlin {
        jvmToolchain {
            (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    val embed by configurations.creating

    configurations.implementation.get().extendsFrom(embed)

    afterEvaluate {
        configurations.implementation.get()
    }

    dependencies {
        if (hasBungeeCord(project)) {
            embed("org.jetbrains.kotlin:kotlin-stdlib")
            compileOnly(rootProject.files("libs/BungeeCord.jar"))
        }
        if (hasBukkit(project)) {
            implementation("io.papermc.paper:paper-api:1.19.2-R0.1-SNAPSHOT")
            compileOnly(rootProject.files("./libs/paper-server-1.19.2-R0.1-SNAPSHOT-reobf.jar"))
            implementation("org.jetbrains.kotlin:kotlin-stdlib")
            implementation("io.netty:netty-all:4.1.68.Final")
            implementation(rootProject.files("./bukkit-rebel-plugin.jar"))
//            compileOnly(rootProject.files("./libs/paper-server-1.18.2-R0.1-SNAPSHOT-reobf.jar"))
            compileOnly(rootProject.files("./libs/authlib-3.3.39.jar"))
            implementation("io.github.mainyf:newmclib-craftbukkit:1.7.3:")
            compileOnly(rootProject.files("./plugins/ProtocolLib.jar"))
            compileOnly(rootProject.files("./plugins/PlaceholderAPI-2.11.2.jar"))
            compileOnly(rootProject.files("./plugins/CommandAPI-8.5.1.jar"))
            compileOnly(rootProject.files("./libs/Vault.jar"))

            when (project.name) {
                "plugin-loader" -> {
                    implementation(rootProject.fileTree(mapOf(
                        "dir" to "./server/bukkit/libraries",
                        "include" to "**/*.jar"
                    )))
                    embed("net.bytebuddy:byte-buddy:1.11.22")
                }
                "item-skills-plus" -> {
                    embed("com.udojava:EvalEx:2.7")
                    compileOnly(rootProject.files("./plugins/ItemsAdder_3.2.3-r10.jar"))
                }
                "item-enchant-plus" -> {
                    embed("com.udojava:EvalEx:2.7")
                    compileOnly(rootProject.files("./plugins/ItemsAdder_3.2.3-r10.jar"))
                    compileOnly(rootProject.files("./plugins/MythicMobs-5.1.4.jar"))
                }
                "my-islands" -> {
//                    compileOnly("net.skinsrestorer:skinsrestorer-api:14.1.10")

                    compileOnly(rootProject.files("./plugins/ItemsAdder_3.2.3-r10.jar"))
                    compileOnly(rootProject.files("./plugins/PlotSquared-Bukkit-6.9.3-Premium.jar"))
//                    compileOnly(rootProject.files("./libs/PlotSquared-Bukkit-6.6.2-Premium.jar"))
                    compileOnly(rootProject.files("./plugins/CMI9.2.2.0.jar"))
                    compileOnly(rootProject.files("./libs/AuthMe-5.6.0-beta2.jar"))
                    compileOnly(rootProject.files("./libs/datafixerupper-4.1.27.jar"))
                    compileOnly(rootProject.files("./libs/SkinsRestorer.jar"))
                    compileOnly(rootProject.project(":bukkit:bungee-settings-bukkit"))
                    compileOnly(rootProject.project(":bukkit:social-system"))
                }
                "login-settings" -> {
                    implementation("io.github.dreamvoid:MiraiMC-Integration:1.7")
                    implementation(rootProject.files("./libs/MiraiMC-Bukkit.jar"))
                    implementation("net.mamoe:mirai-core-jvm:2.11.1")
                    compileOnly(rootProject.files("./libs/AuthMe-5.6.0-beta2.jar"))
                    compileOnly(rootProject.project(":bukkit:bungee-settings-bukkit"))
                }
                "bungee-settings-bukkit" -> {
                    compileOnly(rootProject.files("./plugins/CMI9.2.2.0.jar"))
                }
                "player-account" -> {
                    embed("com.aliyun:alibabacloud-dysmsapi20170525:1.0.1")
                }
                "player-settings" -> {
//                    embed("com.alibaba:easyexcel:3.0.5")
                    compileOnly(rootProject.files("./libs/AuthMe-5.6.0-beta2.jar"))
                    compileOnly(rootProject.project(":bukkit:bungee-settings-bukkit"))
                }
                "quest-extension" -> {
                    implementation(rootProject.files("./plugins/GCore.jar"))
                    implementation(rootProject.files("./plugins/QuestCreator.jar"))
                    compileOnly(rootProject.project(":bukkit:custom-economy"))
                }
                "command-settings" -> {
                    compileOnly(rootProject.files("./plugins/ItemsAdder_3.2.3-r10.jar"))
                    compileOnly(rootProject.project(":bukkit:bungee-settings-bukkit"))
                }
                "mcrmb-migration" -> {
//                    compileOnly(rootProject.files("./plugins/MCRMB-2.0b19-12fe19a.jar"))
                    compileOnly(rootProject.files("./plugins/PlayerPoints-3.2.4.jar"))
                }
                "mcrmb-tools" -> {
                    compileOnly(rootProject.files("./plugins/MCRMB-2.0b19-12fe19a.jar"))
                }
                "shop-manager" -> {
                    compileOnly(rootProject.files("./libs/QuickShop.jar"))
                }
                "social-system" -> {
                    compileOnly(rootProject.project(":bukkit:bungee-settings-bukkit"))
//                    compileOnly(rootProject.project(":bukkit:my-islands"))
                }
                "custom-economy" -> {
                    compileOnly(rootProject.files("./plugins/PlaceholderAPI-2.11.2.jar"))
                }
                "cs-dungeon" -> {
                    compileOnly(rootProject.files("./plugins/ItemsAdder_3.2.3-r10.jar"))
                    compileOnly(rootProject.files("./plugins/CustomStructures-1.8.0.jar"))
                    compileOnly(rootProject.files("./plugins/MythicMobs-5.1.4.jar"))
                    compileOnly(rootProject.project(":bukkit:world-settings"))
                }
            }
        }
    }

    tasks.jar {
        if (project.name == "player-account") {
//            from(provider {
//                val fatJarDir = project.projectDir.resolve("build/tmp/lib")
//                fatJarDir.listFiles()?.forEach { file ->
//                    file.delete()
//                }
//                embed.toList().forEach {
//                    val libFile = fatJarDir.resolve(it.name)
//                    it.copyTo(libFile, true)
//                }
//                fatJarDir
//            })
            from(embed)
        } else {
            from(embed.map(::zipTree))
        }
//        from(embed.map(::zipTree))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    if (hasBukkit(project)) {
        bukkit {
            val pluginName = lineToUpper(project.name)
            val packageName = lineToLower(project.name)
            name = pluginName
            main = "io.github.mainyf.${packageName}.${pluginName}"
            version = project.version.toString()
            apiVersion = "1.13"

            val dd = mutableListOf<String>()
            val sd = mutableListOf<String>()
            when (project.name) {
//                "item-skills-plus" -> {
//                    dd.addAll(listOf("ProtocolLib", "ItemsAdder", "PlaceholderAPI"))
//                }
                "item-enchant-plus" -> {
                    dd.addAll(listOf("ProtocolLib", "ItemsAdder", "PlaceholderAPI", "MythicMobs"))
                }
                "my-islands" -> {
                    dd.addAll(listOf("CMI", "PlotSquared", "ItemsAdder", "ProtocolLib", "BungeeSettingsBukkit"))
                    sd.addAll(listOf("AuthMe", "SocialSystem"))
                }
                "world-settings" -> {
                    dd.addAll(listOf("ProtocolLib"))
                }
                "bungee-settings-bukkit" -> {
                    dd.addAll(listOf("CMI", "PlaceholderAPI"))
                }
                "player-settings" -> {
                    sd.addAll(listOf("AuthMe", "BungeeSettingsBukkit"))
                }
                "quest-extension" -> {
                    dd.addAll(listOf("QuestCreator", "CustomEconomy"))
                }
                "command-settings" -> {
                    dd.addAll(listOf("ItemsAdder"))
                    sd.addAll(listOf("BungeeSettingsBukkit"))
                }
                "mcrmb-migration" -> {
                    dd.addAll(listOf("PlayerPoints"))
                }
                "mcrmb-tools" -> {
                    dd.addAll(listOf("Mcrmb"))
                }
                "login-settings" -> {
                    dd.addAll(listOf("AuthMe", "MiraiMC", "BungeeSettingsBukkit"))
                }
                "shop-manager" -> {
                    dd.addAll(listOf("QuickShop", "Vault"))
                }
                "social-system" -> {
                    dd.addAll(listOf("PlaceholderAPI", "BungeeSettingsBukkit"))
//                    sd.addAll(listOf("MyIslands"))
                }
                "custom-economy" -> {
                    dd.addAll(listOf("PlaceholderAPI"))
                }
                "cs-dungeon" -> {
                    dd.addAll(listOf("CustomStructures", "ItemsAdder", "MythicMobs", "WorldSettings"))
                }
                "tools-plugin" -> {
//                    dd.addAll(listOf("PlaceholderAPI"))
                }
//                "linkQQ" -> {
//                    dd.addAll(listOf("MiraiMC", "AuthMe"))
//                }
            }
            if (project.name != "plugin-loader") {
                dd.add("NewMCLib")
            }
            depend = dd
            softDepend = sd
        }

        tasks.register<DefaultTask>("initSources") {
            group = "bukkit"
            doLast {
                val pluginName = lineToUpper(project.name)
                val packageName = "io/github/mainyf/${lineToLower(project.name)}"
                val mainDir = project.projectDir.resolve("src/main/kotlin")
                if (!mainDir.exists()) {
                    val lastPacketDir = mainDir.resolve(packageName)
                    lastPacketDir.mkdirs()
                    val file = lastPacketDir.resolve("${pluginName}.kt")
                    file.writeText(
                        """
package ${packageName.replace("/", ".")}

import org.apache.logging.log4j.LogManager
import org.bukkit.plugin.java.JavaPlugin

class $pluginName : JavaPlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("$pluginName")

        lateinit var INSTANCE: $pluginName

    }

    override fun onEnable() {
        INSTANCE = this
        
    }
}
            """.trimIndent()
                    )
                }
            }
        }

        val folder = "bukkit"

        tasks.register<Copy>("copyPlugin") {
            group = "bukkit"
            from(tasks.jar)
            into(rootProject.file("./server/${folder}/plugins/").absolutePath)
        }

        tasks.register<Copy>("copyPlugin2") {
            group = "bukkit"
            from(tasks.jar)
            into(rootProject.file("./server/bukkit2/plugins/").absolutePath)
        }

        tasks.register<JavaExec>("runServer") {
            dependsOn(tasks.findByName("copyPlugin"))
            classpath = rootProject.files("./server/${folder}/paper-1.19.2-211.jar")
            mainClass.set("io.papermc.paperclip.Paperclip")
            val jvmArgsText =
                "-Xmx4g -Dfile.encoding=UTF-8 --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/jdk.internal.access=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED"
//            jvmArgs = listOf("-Xmx4g", "-Dfile.encoding=UTF-8")
            jvmArgs = jvmArgsText.split(" ").toMutableList().apply {
                add("-Drebel.plugins=${rootProject.file("./bukkit-rebel-plugin.jar").absolutePath}")
                add("-Drebel.bukkit=true")
            }
            args = listOf("nogui")
            workingDir = rootProject.file("./server/${folder}")
            standardOutput = System.out
            standardInput = System.`in`
            errorOutput = System.err
            group = "bukkit"
            description = "runs the bukkit server"
        }

        tasks.register<JavaExec>("runServer2") {
            dependsOn(tasks.findByName("copyPlugin2"))
            classpath = rootProject.files("./server/bukkit2/paper-1.19.2-211.jar")
            mainClass.set("io.papermc.paperclip.Paperclip")
            val jvmArgsText =
                "-Xmx4g -Dfile.encoding=UTF-8 --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/jdk.internal.access=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED"
//            jvmArgs = listOf("-Xmx4g", "-Dfile.encoding=UTF-8")
            jvmArgs = jvmArgsText.split(" ").toMutableList().apply {
                add("-Drebel.plugins=${rootProject.file("./bukkit-rebel-plugin.jar").absolutePath}")
                add("-Drebel.bukkit=true")
            }
            args = listOf("nogui")
            workingDir = rootProject.file("./server/bukkit2")
            standardOutput = System.out
            standardInput = System.`in`
            errorOutput = System.err
            group = "bukkit"
            description = "runs the bukkit server"
        }

        tasks.register<JavaExec>("runTestServer") {
            dependsOn(tasks.findByName("copyPlugin"))
            classpath = rootProject.files("./server/test/paper-1.19.2-211.jar")
            mainClass.set("io.papermc.paperclip.Paperclip")
            val jvmArgsText =
                "-Xmx4g -Dfile.encoding=UTF-8 --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/jdk.internal.access=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED"
//            jvmArgs = listOf("-Xmx4g", "-Dfile.encoding=UTF-8")
            jvmArgs = jvmArgsText.split(" ")
            args = listOf("nogui")
            workingDir = rootProject.file("./server/test")
            standardOutput = System.out
            standardInput = System.`in`
            errorOutput = System.err
            group = "bukkit"
            description = "runs the bukkit server"
        }
    }


    if (hasBungeeCord(project)) {
        tasks.register<Copy>("copyPlugin") {
            group = "bukkit"
            rootProject.file("./server/bungeecord/plugins/").listFiles()?.find {
                it.name.startsWith(project.name) && it.name.endsWith(".jar")
            }?.delete()
            from(tasks.jar)
            into(rootProject.file("./server/bungeecord/plugins/").absolutePath)
        }

        tasks.register<JavaExec>("runServer") {
            dependsOn(tasks.findByName("copyPlugin"))
            classpath = rootProject.files("./server/bungeecord/BungeeCord.jar")
            mainClass.set("net.md_5.bungee.Bootstrap")
            jvmArgs = listOf("-Xmx4g"/*, "-Dfile.encoding=UTF-8"*/)
            args = listOf("nogui")
            workingDir = rootProject.file("./server/bungeecord")
            standardOutput = System.out
            standardInput = System.`in`
            errorOutput = System.err
            group = "bukkit"
            description = "runs the bukkit server"
        }
    }

}


fun lineToUpper(param: String): String {
    val sb = StringBuilder(param)
    val mc = Pattern.compile("-").matcher(param)
    var i = 0
    sb.setCharAt(0, sb[0].toString().toUpperCase().toCharArray()[0])
    while (mc.find()) {
        val position = mc.end() - (i++)
        sb.replace(position - 1, position + 1, sb.substring(position, position + 1).toUpperCase())
    }
    return sb.toString()
}

fun lineToLower(param: String): String {
    val sb = StringBuilder(param)
    val mc = Pattern.compile("-").matcher(param)
    var i = 0
    while (mc.find()) {
        val position = mc.end() - (i++)
        sb.replace(position - 1, position + 1, sb.substring(position, position + 1).toLowerCase())
    }
    return sb.toString()
}

fun hasBukkit(project: Project): Boolean {
    return "bukkit" == project.projectDir.parentFile.name
}

fun hasBungeeCord(project: Project): Boolean {
    return "bungeecord" == project.projectDir.parentFile.name
}