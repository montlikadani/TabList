repositories {
	maven("https://repo-new.spongepowered.org/maven")
}

plugins {
	id("java-library")
}

dependencies {
	compileOnly("org.spongepowered:spongeapi:8.1.0-SNAPSHOT")
}

description = "tablist-sponge8"
version = "1.0.5"
