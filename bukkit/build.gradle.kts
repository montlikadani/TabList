plugins {
    alias(libs.plugins.shadow)
}

repositories {
    gradlePluginPortal()
    mavenCentral()

    maven("https://repo.codemc.org/repository/maven-public")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://libraries.minecraft.net/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.essentialsx.net/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

    maven {
		url = uri("https://jitpack.io")
		content {
			includeGroup("com.github.johnrengelman")
			includeGroup("com.github.xtomyserrax")
			includeGroup("com.github.MilkBowl")
			includeGroup("com.github.LeonMangler")
		}
	}

    maven("https://oss.sonatype.org/content/groups/public/") // Netty
}

val nmsProjects = setOf("1_8_8", "1_17_1", "1_18_2", "1_19_1", "1_19_2", "1_19_3", "1_19_4", "1_20_1", "1_20_2",
    "1_20_4", "1_20_6", "1_21", "1_21_4")

dependencies {
    implementation(project(":global"))
    api(project(":api"))
    api(project(":folia"))

    nmsProjects.forEach {
        api(project(":v$it"))
    }

    implementation("org.bstats:bstats-bukkit:3.1.0")

    compileOnly("com.github.xtomyserrax:StaffFacilities:5.0.8")
    compileOnly(libs.authlib)
    compileOnly("net.luckperms:api:5.4")

    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT") {
        exclude("com.mojang", "authlib")
    }

    compileOnly("net.essentialsx:EssentialsX:2.21.0-SNAPSHOT") {
        isTransitive = false
    }

    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit")
    }

    compileOnly("me.clip:placeholderapi:2.11.6") {
        isTransitive = false
    }

    compileOnly("com.github.LeonMangler:SuperVanish:6.2.19") {
        isTransitive = false
    }

    compileOnly(files("lib/CMI9.0.0.0API.jar", "lib/PermissionsEx-1.23.4.jar"))
    compileOnly(libs.netty.core)
}

group = "hu.montlikadani.tablist"
version = "5.7.7"

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    jar {
        archiveClassifier.set("noshade")
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("TabList-bukkit-v${project.version}.jar")
        includeEmptyDirs = false

        dependencies {
            nmsProjects.forEach {
                include(project(":v$it"))
            }

            include(project(":api"))
            include(project(":global"))
            include(dependency("org.bstats::"))
        }

        relocate("org.bstats", "${project.group}.lib.bstats")
        minimize()
    }

    build {
        dependsOn(shadowJar)
    }
}
