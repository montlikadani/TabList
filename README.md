# TabList [![GitHub release](https://img.shields.io/github/release/montlikadani/TabList.svg)](https://github.com/montlikadani/TabList/releases) [![Github All Releases](https://img.shields.io/github/downloads/montlikadani/TabList/total.svg)](https://github.com/montlikadani/TabList/releases) [![GitHub issues](https://img.shields.io/github/issues/montlikadani/TabList.svg)](https://github.com/montlikadani/TabList/issues)

[![bStats](https://img.shields.io/badge/bStats-3.0.0-brightgreen)](https://bstats.org/plugin/bukkit/TabList/1479)

***

A plugin for Minecraft servers that allows to get an animated tablist with header/footer for players to replace the vanilla empty one.

- More detailed documentation can be found on [wiki](https://github.com/montlikadani/TabList/wiki) page.
- Download the latest build from [actions](https://github.com/montlikadani/TabList/actions) page. You will need github account to download.
- For general questions and support, join our discord server: https://discord.gg/XsfT5cJ

### API
To get access to TabList API you need [jitpack](https://jitpack.io/#montlikadani/TabList)

**Maven**
```xml
<repositories>
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>

<dependency>
	<groupId>com.github.montlikadani</groupId>
	<artifactId>TabList</artifactId>
	<version>master-SNAPSHOT</version>
</dependency>
```
**Gradle** (Groovy)
```groovy
repositories {
    maven {
        url = uri('https://jitpack.io')
    }
}

dependencies {
    compileOnly 'com.github.montlikadani:TabList:master-SNAPSHOT'
}
```
_Use `master-SNAPSHOT` as versioning to get the newest commit changes_

## Link
* [Spigot](https://www.spigotmc.org/resources/46229/)
* [Bukkit](https://dev.bukkit.org/projects/animated-tab-tablist)
* [Sponge](https://ore.spongepowered.org/montlikadani/%5BAnimated-Tab%5D---TabList)

### Bug report/feature request
If you find a bug or you want a feature to be added, please make an issue here: https://github.com/montlikadani/TabList/issues/new/choose
