import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val lineBotVersion by extra("8.6.0")
val springCloudGcpVersion by extra("5.1.0")
val mapsApiVersion by extra("2.2.0")

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    kotlin("plugin.allopen") version "1.9.23"

    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "com.firebaseapp"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin")
    implementation("com.linecorp.bot", "line-bot-spring-boot-handler", lineBotVersion)
    implementation("org.springframework.boot", "spring-boot-starter-web")
    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("com.google.cloud", "spring-cloud-gcp-starter-sql-postgresql")
    implementation("com.google.cloud", "spring-cloud-gcp-starter-storage")
    implementation("com.google.maps", "google-maps-services", mapsApiVersion)

    implementation("org.springframework.boot", "spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot", "spring-boot-starter-security")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
}
dependencyManagement {
    imports {
        mavenBom("com.google.cloud:spring-cloud-gcp-dependencies:$springCloudGcpVersion")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        freeCompilerArgs += "-Xjvm-default=all"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
