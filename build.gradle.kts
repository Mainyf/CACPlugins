import java.util.regex.Pattern

plugins {
    id("java")
    kotlin("jvm") version "1.6.10"
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
            implementation("io.github.mainyf:newmclib-craftbukkit:1.7.2")
            compileOnly(rootProject.files("./server/bukkit/plugins/ProtocolLib.jar"))

            when (project.name) {
                "item-skills-plus" -> {
                    embed("com.udojava:EvalEx:2.7")
                    compileOnly(rootProject.files("./server/bukkit/plugins/ItemsAdder_3.0.4b.jar"))
                    compileOnly(rootProject.files("./server/bukkit/plugins/PlaceholderAPI-2.11.1.jar"))
                }
                "my-islands" -> {
                    compileOnly(rootProject.files("./server/bukkit/plugins/ItemsAdder_3.0.4b.jar"))
                    compileOnly(rootProject.files("./server/bukkit/plugins/PlotSquared-Bukkit-6.6.2-Premium.jar"))
                    compileOnly(rootProject.files("./server/bukkit/plugins/CMI9.1.3.0.jar"))
                }
                "bungee-settings-bukkit" -> {
                    compileOnly(rootProject.files("./server/bukkit/plugins/CMI9.1.3.0.jar"))
                    compileOnly(rootProject.files("./server/bukkit/plugins/PlaceholderAPI-2.11.1.jar"))
                }
            }
        }
    }

    tasks.jar {
        from(embed.map(::zipTree))
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
            when (project.name) {
                "item-skills-plus" -> {
                    dd.addAll(listOf("ProtocolLib", "ItemsAdder", "PlaceholderAPI"))
                }
                "my-islands" -> {
                    dd.addAll(listOf("CMI", "PlotSquared", "ItemsAdder", "ProtocolLib"))
                }
                "world-settings" -> {
                    dd.addAll(listOf("ProtocolLib"))
                }
                "bungee-settings-bukkit" -> {
                    dd.addAll(listOf("CMI", "PlaceholderAPI"))
                }
            }
            dd.add("NewMCLib")
            depend = dd
        }

        tasks.register<DefaultTask>("initSources") {
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
            rootProject.file("./server/bukkit/plugins/").listFiles()?.find {
                it.name.startsWith(project.name) && it.name.endsWith(".jar")
            }?.delete()
            from(tasks.jar)
            into(rootProject.file("./server/bukkit/plugins/").absolutePath)
        }

        tasks.register<JavaExec>("runServer") {
            dependsOn(tasks.findByName("copyPlugin"))
            classpath = rootProject.files("./server/bukkit/paper-1.18.2-239.jar")
            mainClass.set("io.papermc.paperclip.Paperclip")
            jvmArgs = listOf("-Xmx4g", "-Dfile.encoding=UTF-8")
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