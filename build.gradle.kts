plugins {
    id("java")
    id ("com.github.johnrengelman.shadow") version "7.1.0"
    `maven-publish`
}

group = "me.heroostech.cityloader"
version = "v1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(libs.minestom)
    implementation(libs.citystom)
    implementation(libs.zstd)
    implementation(libs.commons)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

project.tasks.findByName("jar")?.enabled = false

publishing {
    publications {
        create<MavenPublication>("maven") {
            afterEvaluate {
                val shadowJar = tasks.findByName("shadowJar")
                if (shadowJar == null) from(components["java"])
                else artifact(shadowJar)
            }
            groupId = "me.heroostech.cityloader"
            artifactId = "CityLoader"
            version = "v1.0.0"
        }
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveClassifier.set("")
}