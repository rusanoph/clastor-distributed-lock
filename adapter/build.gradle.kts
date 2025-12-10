subprojects {
    pluginManager.withPlugin("java") {
        dependencies {
            "implementation"(project(":domain"))
        }
    }
}