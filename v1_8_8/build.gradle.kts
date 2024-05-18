repositories {
	maven("https://libraries.minecraft.net/")
}

dependencies {
	api(project(":api"))

	compileOnly(libs.authlib)
	compileOnly(files("lib/spigot-1.8.8.jar"))
}
