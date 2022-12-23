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

	compileOnly("com.mojang:authlib:3.3.39") // 3.3.39 was compiled with JDK 8 so we must use this
	compileOnly(files("lib/spigot-1.8.8.jar"))
}
