plugins {
    java
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.forum.kma"
version = "0.0.1-SNAPSHOT"
description = "api-gateway"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

extra["springCloudVersion"] = "2025.0.0"

dependencies {
    implementation("com.forum.kma:common:1.0.0")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")

    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
