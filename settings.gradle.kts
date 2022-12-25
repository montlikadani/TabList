pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://repo.papermc.io/repository/maven-public/")
	}
}

rootProject.name = "TabList"

include(
	"global",
	"api",
	"v1_19_R2",
	"v1_19_R1",
	"v1_18_R2",
	"v1_8_R3",
	"bukkit",
	"bungee",
	"tablist-sponge7",
	"tablist-sponge8")
project(":tablist-sponge7").projectDir = file("sponge7")
project(":tablist-sponge8").projectDir = file("sponge8")
