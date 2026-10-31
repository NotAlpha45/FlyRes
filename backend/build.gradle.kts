// Business and data layers. This module must never depend on JavaFX.
plugins {
    `java-library`
}

dependencies {
    implementation(libs.sqlite.jdbc)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}
