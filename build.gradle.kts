import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinxVersion by extra("0.4.1")
val lineBotVersion by extra("7.8.0")
val cloudSqlVersion by extra("5.0.4")
val springCloudGcpVersion by extra("5.1.0")
val mapsApiVersion by extra("2.2.0")

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    kotlin("plugin.allopen") version "1.9.23"

    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.firebaseapp"
version = "0.5.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}


dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-reflect")
    implementation("org.jetbrains.kotlinx", "kotlinx-datetime", kotlinxVersion)

    implementation("org.springframework.boot", "spring-boot-starter-web")
    implementation("org.springframework.boot", "spring-boot-starter-thymeleaf")

    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("org.postgresql", "postgresql")
    implementation("com.google.cloud", "spring-cloud-gcp-starter-sql-postgresql")
    implementation("com.google.cloud", "spring-cloud-gcp-starter-storage")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin")

    implementation("com.google.maps", "google-maps-services", mapsApiVersion)

    implementation("com.linecorp.bot", "line-bot-spring-boot-web", lineBotVersion)
    implementation("com.linecorp.bot", "line-bot-spring-boot-handler", lineBotVersion)
    implementation("com.linecorp.bot", "line-bot-spring-boot-client", lineBotVersion)

    implementation("org.springframework.boot", "spring-boot-starter-security")
    implementation("org.springframework.boot", "spring-boot-starter-oauth2-resource-server")
    implementation("org.thymeleaf.extras", "thymeleaf-extras-springsecurity6")

    developmentOnly("org.springframework.boot", "spring-boot-devtools")
    testImplementation("org.springframework.boot", "spring-boot-starter-test")
    testImplementation("org.springframework.security", "spring-security-test")
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


/*
tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
        freeCompilerArgs.add("-Xexport-kdoc")
    }
}
 */