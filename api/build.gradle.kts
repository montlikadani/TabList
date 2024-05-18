repositories {
	maven("https://libraries.minecraft.net/")
	maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
	compileOnly(libs.authlib) {
		isTransitive = false
	}

	compileOnly("org.spigotmc:spigot-api:1.20.6-R0.1-SNAPSHOT") {
		exclude("com.google.guava", "guava")
		exclude("com.google.code.gson", "gson")
		exclude("junit", "junit")
		exclude("net.md-5")
		exclude("org.yaml")
	}
}
