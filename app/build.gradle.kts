plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":adapter:curator"))
    implementation(project(":adapter:zookeeper"))

    implementation(libs.spring.boot.web)

    testImplementation(libs.curator.test)
    testImplementation(libs.zookeeper)
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

tasks.processTestResources {
    from(rootProject.file("docs/openapi.yaml"))
}
