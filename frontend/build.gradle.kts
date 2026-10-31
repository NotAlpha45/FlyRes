// Presentation layer: JavaFX boundaries on top of the backend facade.
plugins {
    application
    alias(libs.plugins.javafx)
    alias(libs.plugins.shadow)
}

javafx {
    version = libs.versions.javafx.get()
    modules("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation(project(":backend"))
    implementation(libs.atlantafx)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly(libs.junit.launcher)
}

application {
    mainClass = "com.flyres.ui.Launcher"
}

tasks.shadowJar {
    archiveFileName = "FlyRes.jar"
    // INCLUDE so every META-INF/services file reaches the merge transformer.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
}
