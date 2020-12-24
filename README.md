# TabList [![GitHub release](https://img.shields.io/github/release/montlikadani/TabList.svg)](https://github.com/montlikadani/TabList/releases) [![Github All Releases](https://img.shields.io/github/downloads/montlikadani/TabList/total.svg)](https://github.com/montlikadani/TabList/releases) [![GitHub issues](https://img.shields.io/github/issues/montlikadani/TabList.svg)](https://github.com/montlikadani/TabList/issues)

[![bStats](https://img.shields.io/badge/bStats-1.8-brightgreen)](https://bstats.org/plugin/bukkit/TabList)

***

A fully configurable plugin that allows all online players that are one the server to get an animated tablist with header/footer, which is configurable in the config.

## Link
* [Spigot](https://www.spigotmc.org/resources/46229/)
* [Bukkit](https://dev.bukkit.org/projects/animated-tab-tablist)
* [MCMarket](https://www.mc-market.org/resources/6127/)
* [Sponge](https://ore.spongepowered.org/montlikadani/%5BAnimated-Tab%5D---TabList)

# TabList API
You can manually add the jar file to your build path or you can use jitpack if you use maven or gradle:
## Maven:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.montlikadani</groupId>
        <artifactId>TabList</artifactId>
        <version>LATEST</version> <!-- Change the LATEST to the current version of plugin -->
        <scope>provided</scope>
    </dependency>
</dependencies>
```
## Gradle:
```gradle
repositories {
    maven { 
        url 'https://jitpack.io' 
    }
}
dependencies {
    implementation 'com.github.montlikadani:TabList:LATEST' //Change the LATEST to the current version of plugin
}
```

For API methods: [TabListAPI](https://github.com/montlikadani/TabList/blob/master/src/main/java/hu/montlikadani/tablist/bukkit/API/TabListAPI.java)

### Bug report/feature request
If you find a bug about the plugin, please make an issue here: https://github.com/montlikadani/TabList/issues
