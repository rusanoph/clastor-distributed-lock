plugins {
    java
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":adapter:curator"))
    implementation(project(":adapter:zookeeper"))

    implementation("org.springframework.boot:spring-boot-starter-web:4.0.0")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}

tasks.getByName<Jar>("jar") {
    enabled = false
}