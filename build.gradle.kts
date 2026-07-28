plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

// Oldest API the plugin supports. Compiling against it guarantees that no method
// introduced after 1.21.11 sneaks in.
val minimumPaperApi = "1.21.11-R0.1-SNAPSHOT"

// Newest API the plugin is verified against by the `checkLatestApi` task.
val latestPaperApi = "26.2.build.84-stable"

// Minecraft 1.21.11 runs on Java 21, so the jar must not require anything newer.
val targetJavaVersion = 21

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val latestApi: Configuration by configurations.creating

dependencies {
    compileOnly("io.papermc.paper:paper-api:$minimumPaperApi")
    latestApi("io.papermc.paper:paper-api:$latestPaperApi")
}

java {
    // A recent JDK is needed to *read* the newest paper-api jar; `release` below
    // keeps the emitted bytecode usable on Java 21 servers.
    toolchain.languageVersion = JavaLanguageVersion.of(26)
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release = targetJavaVersion
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:deprecation")
    }

    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    runServer {
        minecraftVersion(providers.gradleProperty("runVersion").getOrElse("26.2"))
        jvmArgs("-Xms2G", "-Xmx2G")
    }
}

// Recompiles the exact same sources against the newest Paper API. Anything that was
// removed or changed between the two versions fails here instead of at runtime.
val checkLatestApi by tasks.registering(JavaCompile::class) {
    group = "verification"
    description = "Compiles the sources against paper-api $latestPaperApi to verify forward compatibility."
    source(sourceSets.main.get().java)
    classpath = latestApi
    destinationDirectory = layout.buildDirectory.dir("classes/java/latestApi")
    options.release = targetJavaVersion
    options.encoding = "UTF-8"
}

tasks.check {
    dependsOn(checkLatestApi)
}
