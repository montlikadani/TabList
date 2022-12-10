pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()

		maven("https://repo.papermc.io/repository/maven-public/")
		maven("https://repo.maven.apache.org/maven2/")
		maven("https://repo.purpurmc.org/snapshots")
		maven("https://jitpack.io")
		maven("https://repo.codemc.org/repository/maven-public")
		maven("https://oss.sonatype.org/content/repositories/snapshots")
	}
}

rootProject.name = "TabList"

include(":global")
include(":bukkit")
include(":bungee")
include(":tablist-sponge7")
include(":tablist-sponge8")
project(":tablist-sponge7").projectDir = file("sponge7")
project(":tablist-sponge8").projectDir = file("sponge8")
