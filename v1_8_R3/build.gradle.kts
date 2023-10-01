plugins {
	id("java-library")
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
	disableAutoTargetJvm()
}

repositories {
	maven("https://libraries.minecraft.net/")
}

dependencies {
	api(project(":api"))

	compileOnly(libs.authlib)
	compileOnly(files("lib/spigot-1.8.8.jar"))
}
