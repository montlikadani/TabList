buildscript {
	repositories {
		gradlePluginPortal()
	}
	dependencies {
		classpath("gradle.plugin.com.github.jengelman.gradle.plugins:shadow:7.0.0")
	}
}

repositories {
	mavenCentral()
	maven("https://oss.sonatype.org/content/repositories/snapshots")
}

plugins {
	id("com.github.johnrengelman.shadow") version "7.0.0"
	id("java-library")
}

dependencies {
	implementation(project(":global"))
	compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
	compileOnly("net.md-5:bungeecord-chat:1.19-R0.1-SNAPSHOT")
}

description = "TabList-bungee"
version = "TabList-bungee-2.3.0"
