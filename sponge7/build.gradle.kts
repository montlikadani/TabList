repositories {
	maven("https://repo.spongepowered.org/repository/maven-public/")
}

plugins {
	id("java-library")
}

dependencies {
	compileOnly("org.spongepowered:spongeapi:7.5.0-SNAPSHOT")
}

description = "tablist-sponge7"
version = "1.0.5"
