repositories {
    maven("https://repo.codemc.org/repository/nms/")
    maven("https://libraries.minecraft.net/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/") // Netty
}

dependencies {
    api(project(":api"))

    compileOnly("org.spigotmc:spigot:1.21.4-R0.1-SNAPSHOT") {
        exclude("org.yaml", "snakeyaml")
    }

    compileOnly(libs.authlib)

    compileOnly("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT") {
        exclude("junit", "junit")
        exclude("org.yaml", "snakeyaml")
        exclude("com.mojang", "authlib")
    }

    implementation(libs.netty.core)
}
