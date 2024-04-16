pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://repo.papermc.io/repository/maven-public/")
	}
}

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("netty-core", "io.netty:netty-all:4.1.109.Final")
			library("authlib", "com.mojang:authlib:3.3.39") // 3.3.39 the only last version that was compiled with JDK 8
		}
	}
}

rootProject.name = "TabList"

include("global", "api",
	"v1_20_4", "v1_20_2", "v1_20_1",
	"v1_19_4", "v1_19_3", "v1_19_2", "v1_19_1",
	"v1_18_2",
	"v1_17_1",
	"v1_8_8",
	"bukkit", "bungee")
