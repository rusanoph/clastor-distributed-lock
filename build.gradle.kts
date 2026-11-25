allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    pluginManager.withPlugin("java") {
        apply(plugin = "checkstyle")
        dependencyLocking {
            lockAllConfigurations()
        }
        extensions.configure<CheckstyleExtension> {
            toolVersion = "10.21.4"
            configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        }

        group = "io.sagittarius.clastor.distributedlock"
        version = rootProject.version

        dependencies {
            // Lombok
            "compileOnly"(libs.lombok)
            "annotationProcessor"(libs.lombok)

            // JUnit
            "testImplementation"(platform(libs.junit.bom))
            "testImplementation"(libs.junit.jupiter)
            "testRuntimeOnly"(libs.junit.platform.launcher)
        }

        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(
                    JavaLanguageVersion.of(
                        findProperty("javaVersion")?.toString() ?: "21"
                    )
                )
            }
            withJavadocJar()
            withSourcesJar()
        }

        tasks.withType<Jar>().configureEach {
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
            manifest {
                attributes(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version
                )
            }
            from(rootProject.file("LICENSE")) {
                into("META-INF")
                rename { "LICENSE.txt" }
            }
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        apply(plugin = "maven-publish")

        this@subprojects.extensions.configure<PublishingExtension>("publishing") {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(this@subprojects.components["java"])
                    artifactId = project.name
                }
            }
        }
    }
}
