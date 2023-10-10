# TabList [![GitHub release](https://img.shields.io/github/release/montlikadani/TabList.svg)](https://github.com/montlikadani/TabList/releases) [![Github All Releases](https://img.shields.io/github/downloads/montlikadani/TabList/total.svg)](https://github.com/montlikadani/TabList/releases) [![GitHub issues](https://img.shields.io/github/issues/montlikadani/TabList.svg)](https://github.com/montlikadani/TabList/issues)

[![bStats](https://img.shields.io/badge/bStats-3.0.2-brightgreen)](https://bstats.org/plugin/bukkit/TabList/1479)

***

A plugin for Minecraft servers that allows to get an animated tablist with header/footer for players to replace the vanilla empty one.

- More detailed documentation can be found on [wiki](https://github.com/montlikadani/TabList/wiki) page.
- Download the latest build from [actions](https://github.com/montlikadani/TabList/actions/workflows/gradle.yml) page. You will need github account to download.

### API
<details><summary>Click to view</summary>
To get access to TabList API you need <a href="https://jitpack.io/#montlikadani/TabList" target="_blank">jitpack</a>
<br/>
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
**Gradle** (Kotlin DSL)
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.montlikadani:TabList:master-SNAPSHOT")
}
```
_Use `master-SNAPSHOT` as versioning to get the newest commit changes_

**Alternative solution**
```kotlin
dependencies {
    compileOnly(files("lib/TabList-bukkit-version.jar"))
}
```
</details>

## Link
* [Spigot](https://www.spigotmc.org/resources/46229/)
* [Bukkit](https://dev.bukkit.org/projects/animated-tab-tablist)
* [Sponge](https://ore.spongepowered.org/montlikadani/%5BAnimated-Tab%5D---TabList)

### Bug report/feature request
If you find a bug or you want a feature to be added, please make an issue here: https://github.com/montlikadani/TabList/issues/new/choose
