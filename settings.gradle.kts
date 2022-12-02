@file:Suppress("UnstableApiUsage")

rootProject.name = "CityLoader"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("minestom", "com.github.Minestom:Minestom:-SNAPSHOT")
            library("citystom", "me.heroostech.citystom:CityStom:v1.0.0")
            library("lombok", "org.projectlombok:lombok:1.18.24")
            library("zstd", "com.github.luben:zstd-jni:1.5.2-3")
            library("commons", "commons-io:commons-io:2.11.0")
        }
    }
}