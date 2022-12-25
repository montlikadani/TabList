plugins {
	id("java-library")
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
	disableAutoTargetJvm()
}

repositories {
	maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
	compileOnly("org.purpurmc.purpur:purpur-api:1.19.3-R0.1-SNAPSHOT") {
		exclude("com.google.guava", "guava")
		exclude("com.google.code.gson", "gson")
		exclude("junit", "junit")
	}
}

version = "1.0.0"
