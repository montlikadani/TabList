plugins {
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

group = "hu.montlikadani.tablist"

repositories {
    maven("https://repo.papermc.io/repository/maven-snapshots/")
}

dependencies {
    api(project(":api"))

    paperweight.foliaDevBundle("1.20.4-R0.1-SNAPSHOT") {
        isTransitive = false
    }
}
