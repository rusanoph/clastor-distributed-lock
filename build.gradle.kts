allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    pluginManager.withPlugin("java") {

        group = "io.sagittarius.simplelock"
        version = findProperty("version")?.toString() ?: "1.0-SNAPSHOT"

        dependencies {
            // Lombok
            "compileOnly"("org.projectlombok:lombok:1.18.34")
            "annotationProcessor"("org.projectlombok:lombok:1.18.34")
            "testCompileOnly"("org.projectlombok:lombok:1.18.34")
            "testAnnotationProcessor"("org.projectlombok:lombok:1.18.34")

            // JUnit
            "testImplementation"("org.junit.jupiter:junit-jupiter:5.11.3")
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

        tasks.named<Jar>("jar") {
            manifest {
                attributes(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version
                )
            }
        }

        tasks.withType<Jar>().configureEach {
            val examplePrefixes = setOf(":examples:", ":example:")
            val isExample = examplePrefixes.any(project.path::startsWith)

            val licenseFile = if (isExample) {
                "$rootDir/example/LICENSE"
            } else {
                "$rootDir/LICENSE"
            }

            from(licenseFile) {
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

//configure(subprojects.filter { it.path != ":util" }) {
//    pluginManager.withPlugin("java") {
//        dependencies {
//            implementation(project(":util"))
//        }
//    }
//}