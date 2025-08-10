import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
    kotlin("jvm") version "2.0.20-Beta1"
    kotlin("kapt") version "2.0.20-Beta1"
    id("com.gradleup.shadow") version "8.3.2"
    id("eclipse")
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.8"
    id("xyz.jpenilla.run-velocity") version "2.3.1"
}

group = "club.asynclab"
version = "1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://maven.elytrium.net/repo/") }
    maven { url = uri("https://jitpack.io") }
}

val shade: Configuration by configurations.creating
val fullShade: Configuration by configurations.creating

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
//    shade("io.netty:netty-all:4.1.123.Final")
//    implementation("io.netty:netty-all:4.1.123.Final")
    shade("com.github.ben-manes.caffeine:caffeine:3.2.2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.2")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

tasks {
    runVelocity {
        // Configure the Velocity version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        velocityVersion("3.4.0-SNAPSHOT")
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

val templateSource = file("src/main/templates")
val templateDest = layout.buildDirectory.dir("generated/sources/templates")
val generateTemplates = tasks.register<Copy>("generateTemplates") {
    val props = mapOf("version" to project.version)
    inputs.properties(props)

    from(templateSource)
    into(templateDest)
    expand(props)
}

sourceSets.main.configure { java.srcDir(generateTemplates.map { it.outputs }) }

project.idea.project.settings.taskTriggers.afterSync(generateTemplates)
project.eclipse.synchronizationTasks(generateTemplates)

tasks.shadowJar {
    minimize {
        fullShade.dependencies.forEach { exclude(dependency(it)) }
    }

    mergeServiceFiles()
    configurations = listOf(shade, fullShade)
}

tasks.build { dependsOn("shadowJar") }