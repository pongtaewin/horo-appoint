import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.allopen") version "1.9.22"

    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.firebaseapp"
version = "0.2.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.jetbrains.kotlin","kotlin-reflect")
    implementation("org.jetbrains.kotlinx","kotlinx-datetime","0.4.1")

    implementation("org.springframework.boot","spring-boot-starter-web")
    implementation("org.springframework.boot","spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot","spring-boot-starter-data-jpa")
    developmentOnly("org.springframework.boot","spring-boot-devtools")
    testImplementation("org.springframework.boot","spring-boot-starter-test")

    implementation("org.postgresql","postgresql")
    implementation("com.google.cloud","spring-cloud-gcp-starter-sql-postgresql", "5.0.4")
    implementation("com.fasterxml.jackson.module","jackson-module-kotlin")

    implementation("com.linecorp.bot","line-bot-spring-boot-web", "7.8.0")
    implementation("com.linecorp.bot","line-bot-spring-boot-handler", "7.8.0")
    implementation("com.linecorp.bot","line-bot-spring-boot-client", "7.8.0")

}

allOpen {
    annotation("jakarta.persistence.Entity")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

