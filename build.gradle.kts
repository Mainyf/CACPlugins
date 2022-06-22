import java.util.regex.Pattern

plugins {
    id("java")
    kotlin("jvm") version "1.7.0"
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
            implementation("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
            implementation("org.jetbrains.kotlin:kotlin-stdlib")
            implementation("io.netty:netty-all:4.1.68.Final")
            compileOnly(rootProject.files("./libs/paper-server-1.18.2-R0.1-SNAPSHOT-reobf.jar"))
            compileOnly(rootProject.files("./libs/authlib-3.3.39.jar"))
            implementation("io.github.mainyf:newmclib-craftbukkit:1.7.2")
            compileOnly(rootProject.files("./server/bukkit/plugins/ProtocolLib.jar"))

            when (project.name) {
                "item-skills-plus" -> {
                    embed("com.udojava:EvalEx:2.7")
                    compileOnly(rootProject.files("./server/bukkit/plugins/ItemsAdder_3.2.0b-beta4.jar"))
                    compileOnly(rootProject.files("./server/bukkit/plugins/PlaceholderAPI-2.11.1.jar"))
                }
                "my-islands" -> {
//                    compileOnly("net.skinsrestorer:skinsrestorer-api:14.1.10")

                    compileOnly(rootProject.files("./server/bukkit/plugins/ItemsAdder_3.2.0b-beta4.jar"))
                    compileOnly(rootProject.files("./server/bukkit/plugins/PlotSquared-Bukkit-6.9.0-Premium.jar"))
//                    compileOnly(rootProject.files("./libs/PlotSquared-Bukkit-6.6.2-Premium.jar"))
                    compileOnly(rootProject.files("./server/bukkit/plugins/CMI9.1.3.0.jar"))
                    compileOnly(rootProject.files("./libs/AuthMe-5.6.0-beta2.jar"))
                    compileOnly(rootProject.files("./libs/datafixerupper-4.1.27.jar"))
                    compileOnly(rootProject.files("./libs/SkinsRestorer.jar"))
                }
                "bungee-settings-bukkit" -> {
                    compileOnly(rootProject.files("./server/bukkit/plugins/CMI9.1.3.0.jar"))
                    compileOnly(rootProject.files("./server/bukkit/plugins/PlaceholderAPI-2.11.1.jar"))
                }
                "player-account" -> {
                    embed("com.aliyun:alibabacloud-dysmsapi20170525:1.0.1")
                }
                "player-settings" -> {
                    embed("com.alibaba:easyexcel:3.0.5")
                    compileOnly(rootProject.files("./libs/AuthMe-5.6.0-beta2.jar"))
                }
                "command-settings" -> {
                    compileOnly(rootProject.files("./server/bukkit/plugins/ItemsAdder_3.2.0b-beta4.jar"))
                }
                "mcrmb-migration" -> {
                    compileOnly(rootProject.files("./server/bukkit/plugins/MCRMB-2.0b19-12fe19a.jar"))
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
                "item-skills-plus" -> {
                    dd.addAll(listOf("ProtocolLib", "ItemsAdder", "PlaceholderAPI"))
                }
                "my-islands" -> {
                    dd.addAll(listOf("CMI", "PlotSquared", "ItemsAdder", "ProtocolLib"))
                    sd.addAll(listOf("AuthMe"))
                }
                "world-settings" -> {
                    dd.addAll(listOf("ProtocolLib"))
                }
                "bungee-settings-bukkit" -> {
                    dd.addAll(listOf("CMI", "PlaceholderAPI"))
                }
                "player-settings" -> {
                    sd.addAll(listOf("AuthMe"))
                }
                "command-settings" -> {
                    dd.addAll(listOf("ItemsAdder"))
                }
                "mcrmb-migration" -> {
                    dd.addAll(listOf("Mcrmb"))
                }
            }
            dd.add("NewMCLib")
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

import org.bukkit.plugin.java.JavaPlugin

class $pluginName : JavaPlugin() {

    override fun onEnable() {
        
    }
}
            """.trimIndent()
                    )
                }
            }
        }

        tasks.register<Copy>("copyPlugin") {
            group = "bukkit"
//            rootProject.file("./server/bukkit/plugins/").listFiles()?.find {
//                it.name.startsWith(project.name) && it.name.endsWith(".jar")
//            }?.delete()
            from(tasks.jar)
            into(rootProject.file("./server/bukkit/plugins/").absolutePath)
        }

        tasks.register<JavaExec>("runServer") {
            dependsOn(tasks.findByName("copyPlugin"))
            classpath = rootProject.files("./server/bukkit/paper-1.18.2-239.jar")
            mainClass.set("io.papermc.paperclip.Paperclip")
            val jvmArgsText = "-Xmx4g -Dfile.encoding=UTF-8 --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/jdk.internal.access=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED"
//            jvmArgs = listOf("-Xmx4g", "-Dfile.encoding=UTF-8")
            jvmArgs = jvmArgsText.split(" ")
            args = listOf("nogui")
            workingDir = rootProject.file("./server/bukkit")
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
            jvmArgs = listOf("-Xmx4g", "-Dfile.encoding=UTF-8")
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