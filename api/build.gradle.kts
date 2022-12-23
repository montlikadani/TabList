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
	maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
	compileOnly("com.mojang:authlib:3.3.39") // 3.3.39 was compiled with JDK 8 so we must use this
	compileOnly("org.purpurmc.purpur:purpur-api:1.19.3-R0.1-SNAPSHOT")
}
