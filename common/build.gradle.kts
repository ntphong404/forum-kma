plugins {
    id("org.springframework.boot") version "3.5.7" apply false
    id("io.spring.dependency-management") version "1.1.7"
    `java-library`
    `maven-publish`
}

group = "com.forum.kma"
version = "1.0.0"
description = "common module for Forum-KMA"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // Reactive (Spring WebFlux)
    api("org.springframework.boot:spring-boot-starter-webflux:3.5.0")

    // Lombok
    api("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    // MapStruct
    api("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // Validation API (cho @NotNull, @Email, v.v.)
    api("jakarta.validation:jakarta.validation-api:3.0.2")

    // Reactor (dùng trong Reactive service)
    api("io.projectreactor:reactor-core:3.6.3")

    // Jackson (dùng để serialize các object chung như ApiResponse)
    api("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    // Log (dành cho các module khác)
    api("org.slf4j:slf4j-api:2.0.12")

    // Nimbus JOSE + JWT
    implementation ("com.nimbusds:nimbus-jose-jwt:9.31")

    // Optional, nếu dùng Spring ConfigurationProperties
    implementation ("org.springframework.boot:spring-boot-configuration-processor:3.5.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}
