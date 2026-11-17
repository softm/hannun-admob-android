plugins {
    id("com.android.library") version "8.9.1"
    id("maven-publish")
}

group = "com.github.softm"
version = "1.0.1"

android {
    namespace = "softm.hannun.ads"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api("com.google.android.gms:play-services-ads:25.5.0")
    api("com.google.android.ump:user-messaging-platform:4.0.0")
    testImplementation("junit:junit:4.13.2")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = project.group.toString()
                artifactId = "hannun-admob-android"
                version = project.version.toString()
            }
        }
    }
}
