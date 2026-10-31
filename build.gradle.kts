subprojects {
    group = "com.flyres"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }

    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.compilerArgs.add("-Xlint:all,-serial")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}

// Submission requirement: a zip of every .java file in the project.
tasks.register<Zip>("packageSource") {
    group = "distribution"
    description = "Zips all Java source files for submission."
    archiveFileName = "FlyRes-source.zip"
    destinationDirectory = layout.buildDirectory.dir("distributions")
    from(projectDir) {
        include("backend/src/**/*.java", "frontend/src/**/*.java")
    }
}
