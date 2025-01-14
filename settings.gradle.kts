pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://repo.papermc.io/repository/maven-public/")
	}
}

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("netty-core", "io.netty:netty-all:4.1.114.Final")
			library("authlib", "com.mojang:authlib:6.0.54")
			plugin("shadow", "io.github.goooler.shadow").version("8.1.8")
		}
	}
}

rootProject.name = "TabList"

include("api", "global",
	"v1_21_4",
	"v1_21",
	"v1_20_6", "v1_20_4", "v1_20_2", "v1_20_1",
	"v1_19_4", "v1_19_3", "v1_19_2", "v1_19_1",
	"v1_18_2",
	"v1_17_1",
	"v1_8_8",
	"folia", "bukkit", "bungee")
