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
    maven("https://oss.sonatype.org/content/groups/public/") // Netty
}

dependencies {
    api(project(":api"))

    compileOnly("org.spigotmc:spigot:1.17.1-R0.1-SNAPSHOT") {
        exclude("org.apache.logging", "log4j")
    }
    compileOnly(libs.authlib)

    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT") {
        exclude("junit", "junit")
        exclude("org.yaml", "snakeyaml")
        exclude("com.google.code.gson", "gson")
    }

    implementation(libs.netty.core)
}
