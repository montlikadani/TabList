plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    disableAutoTargetJvm()
}

repositories {
    maven("https://repo.codemc.org/repository/nms/")
    maven("https://libraries.minecraft.net/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    mavenCentral()
}

dependencies {
    api(project(":api"))

    compileOnly("org.spigotmc:spigot:1.19.3-R0.1-SNAPSHOT")
    compileOnly("com.mojang:authlib:3.3.39") // 3.3.39 was compiled with JDK 8 so we must use this

    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT") {
        exclude("junit", "junit")
    }
}
