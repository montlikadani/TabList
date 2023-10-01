pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://repo.papermc.io/repository/maven-public/")
	}
}

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("netty-core", "io.netty:netty-all:4.1.98.Final")
			library("authlib", "com.mojang:authlib:3.3.39") // 3.3.39 was compiled with JDK 8, so we must use this
		}
	}
}

rootProject.name = "TabList"

include(
		"global",
		"api",
		"v1_20_R2",
		"v1_20_R1",
		"v1_19_R3",
		"v1_19_R2",
		"v1_19_R1",
		"v1_18_R2",
		"v1_17_R1",
		"v1_8_R3",
		"bukkit",
		"bungee")
