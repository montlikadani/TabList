plugins {
	id("com.github.johnrengelman.shadow") version "7.0.0"
	id("io.papermc.paperweight.userdev") version "1.3.11"
	id("java-library")
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
	disableAutoTargetJvm()
}

repositories {
	maven("https://repo.purpurmc.org/snapshots")
	maven("https://jitpack.io")
	maven("https://repo.codemc.org/repository/maven-public")
	maven("https://oss.sonatype.org/content/repositories/snapshots")
	maven("https://libraries.minecraft.net/")
	maven("https://repo.dmulloy2.net/nexus/repository/public/")
	maven("https://repo.essentialsx.net/snapshots/")
	maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
	implementation(project(":global"))
	implementation("com.github.xtomyserrax:StaffFacilities:5.0.6.0")
	implementation("org.bstats:bstats-bukkit:3.0.0")
	paperweightDevelopmentBundle("io.papermc.paper:dev-bundle:1.19.3-R0.1-SNAPSHOT")
	compileOnly("com.comphenix.protocol:ProtocolLib:4.8.0")
	compileOnly("com.mojang:authlib:3.3.39") // 3.3.39 was compiled with JDK 8 so we must use this
	compileOnly("org.purpurmc.purpur:purpur-api:1.19.3-R0.1-SNAPSHOT")

	compileOnly("net.essentialsx:EssentialsX:2.20.0-SNAPSHOT") {
		exclude("org.spigotmc", "spigot-api")
	}

	compileOnly("me.clip:placeholderapi:2.11.2")

	compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
		exclude("org.bukkit", "bukkit")
	}

	compileOnly("com.github.LeonMangler:SuperVanish:6.2.6-4")
	compileOnly(files("lib/CMI9.0.0.0API.jar", "lib/PermissionsEx-1.23.4.jar"))
}

version = "5.6.6"

tasks {
	withType<JavaCompile> {
		options.encoding = "UTF-8"
	}
	jar {
		archiveClassifier.set("noshade")
	}
	shadowJar {
		archiveClassifier.set("")
		//archiveFileName.set("TabList-bukkit-v${project.version}.jar") // idk why this doesn't includes deps, in bungee it includes
		relocate("org.bstats", "hu.montlikadani.tablist.bstats")
		exclude("META-INF/**")
	}
	build {
		dependsOn(shadowJar, reobfJar)
	}
}
