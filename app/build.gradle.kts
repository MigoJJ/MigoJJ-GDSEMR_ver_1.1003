plugins {
    application
    alias(libs.plugins.javafx)
}

val javafxVersion = project.property("javafxVersion") as String
val sqliteVersion = project.property("sqliteVersion") as String
val slf4jVersion = project.property("slf4jVersion") as String
val appMaxHeap = providers.gradleProperty("appMaxHeap").getOrElse("1024m")

dependencies {
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    runtimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.2") // Added for JSON parsing

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testfx:testfx-core:4.0.18")
    testImplementation("org.testfx:testfx-junit5:4.0.18")
    testImplementation("org.hamcrest:hamcrest:3.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing")
}

application {
    mainClass.set("com.emr.gds.IttiaApp")
    applicationDefaultJvmArgs = listOf(
        "-Xms64m", "-Xmx$appMaxHeap",
        "--enable-native-access=javafx.graphics", "--enable-native-access=ALL-UNNAMED"
    )
}
