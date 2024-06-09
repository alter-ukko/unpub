plugins {
    kotlin("jvm") version "1.9.23"
    application
}

group = "com.dimdarkevil"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:6.1.6")
    implementation("org.slf4j:slf4j-simple:2.0.10")
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.dimdarkevil.unpub.Unpub")
    applicationName = "unpub"
}
