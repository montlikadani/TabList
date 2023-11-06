plugins {
	id("com.github.johnrengelman.shadow") version "8.1.1"
	id("java-library")
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
	disableAutoTargetJvm()
}

repositories {
	gradlePluginPortal()
	mavenCentral()
	maven("https://oss.sonatype.org/content/repositories/snapshots")
	maven("https://jitpack.io")
}

dependencies {
	implementation(project(":global"))

	compileOnly("net.md-5:bungeecord-api:1.20-R0.2-SNAPSHOT") {
		exclude("com.mojang", "brigadier")
	}

	compileOnly("net.md-5:bungeecord-chat:1.20-R0.2-SNAPSHOT") {
		exclude("com.mojang", "brigadier")
	}

	compileOnly("com.github.LeonMangler:PremiumVanishAPI:2.9.0-4") {
		isTransitive = true
	}
}

version = "2.3.4"

// All of these is required to include :global project class files
tasks {
	withType<JavaCompile> {
		options.encoding = "UTF-8"
	}
	jar {
		archiveClassifier.set("noshade")
	}
	shadowJar {
		archiveClassifier.set("")
		archiveFileName.set("TabList-bungee-v${project.version}.jar")
	}
	build {
		dependsOn(shadowJar)
	}
}
