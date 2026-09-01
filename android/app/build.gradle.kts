import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val apiBaseUrl: String = (project.findProperty("apiBaseUrl") as? String)?.trimEnd('/')
    ?: "https://webnovo.ir/project/webapp"

android {
    namespace = "ir.sabztime.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "ir.sabztime.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // اگر keystore.properties موجود نباشد (ساخت پیش‌نمایش در CI)،
            // با کلید debug امضا می‌شود تا APK قابل نصب بماند.
            val keystorePropsFile = rootProject.file("keystore.properties")
            if (keystorePropsFile.exists()) {
                val props = Properties()
                keystorePropsFile.inputStream().use { props.load(it) }
                signingConfig = signingConfigs.create("release") {
                    storeFile = rootProject.file(props.getProperty("storeFile"))
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                }
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all { it.jvmArgs("-Xmx2g") }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-util")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// ---------------------------------------------------------------------------
// CI diagnostics: mirror gradle stderr (kotlin compile errors) and junit
// failures into $GITHUB_STEP_SUMMARY so they are readable via the check-run
// API even when runner logs are not downloadable.
// ---------------------------------------------------------------------------
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

val ciSummaryPath: String? = System.getenv("GITHUB_STEP_SUMMARY")
val ciStderrFile = File(buildDir, "ci-stderr.log")

if (ciSummaryPath != null) {
    val tee = PrintWriter(FileWriter(ciStderrFile, true), true)
    logging.addStandardErrorListener(tee)
    gradle.buildFinished {
        val summary = File(ciSummaryPath)
        val failure = it.failure
        if (failure != null) {
            summary.appendText("\n## gradle failure\n```\n$failure\n```\n")
        }
        if (ciStderrFile.exists() && ciStderrFile.length() > 0) {
            val lines = ciStderrFile.readLines().filter {
                it.startsWith("e: ") || it.contains("error:") || it.contains("FAILED")
            }.distinct()
            if (lines.isNotEmpty()) {
                summary.appendText("\n## captured errors\n```\n${lines.take(80).joinToString("\n")}\n```\n")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    finalizedBy("ciExportTestResults")
}

tasks.register("ciExportTestResults") {
    doLast {
        val summary = ciSummaryPath?.let(::File) ?: return@doLast
        val dir = File(buildDir, "test-results/testDebugUnitTest")
        val files = dir.listFiles { f -> f.name.endsWith(".xml") } ?: return@doLast
        summary.appendText("\n## junit results\n")
        files.forEach { f ->
            val t = f.readText()
            Regex("<failure[^>]*message=\"([^\"]*)\"").findAll(t).forEach { m ->
                summary.appendText("- FAIL: ${m.groupValues[1].take(300)}\n")
            }
            Regex("<testcase[^>]*name=\"([^\"]+)\"[^>]*classname=\"([^\"]+)\"").findAll(t).forEach { m ->
                summary.appendText("- case: ${m.groupValues[2]}#${m.groupValues[1]}\n")
            }
        }
    }
}
