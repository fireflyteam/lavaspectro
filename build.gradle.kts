import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "2.1.0"
    id("dev.arbjerg.lavalink.gradle-plugin") version "1.1.2"
    `maven-publish`
}

group = "team.firefly.lavalink.lavaspectro"
val baseVersion = "1.0.0"

val githubHeadRef: String? = System.getenv("GITHUB_HEAD_REF") ?: System.getenv("GITHUB_REF_NAME")

val isSnapshot = project.hasProperty("snapshot") || 
    githubHeadRef?.startsWith("refs/heads/") == true

// Git commit hash (fixed exec)
val commitHash = try {
    val stdout = ByteArrayOutputStream()
    project.exec {
        commandLine("git", "rev-parse", "--short=7", "HEAD")
        standardOutput = stdout
    }
    stdout.toString().trim()
} catch (e: Exception) {
    "unknown"
}

// Versions (fixed assignment)
val mavenSnapshotVersion = if (isSnapshot) "$baseVersion-SNAPSHOT" else baseVersion
val commitVersion = if (isSnapshot) "$baseVersion+${commitHash}" else baseVersion

version = mavenSnapshotVersion

repositories {
    mavenCentral()
    maven { url = uri("https://maven.lavalink.dev/releases") }
    maven { url = uri("https://maven.lavalink.dev/snapshots") }
    maven { url = uri("https://jitpack.io") }
    
    if (project.hasProperty("MAVEN_REPOSITORY")) {
        maven {
            url = uri(project.property("MAVEN_REPOSITORY").toString())
            credentials {
                username = project.findProperty("MAVEN_USERNAME") as? String ?: ""
                password = project.findProperty("MAVEN_TOKEN") as? String ?: ""
            }
        }
    }
}

dependencies {
    compileOnly("dev.arbjerg.lavalink:plugin-api:4.1.2")
    implementation("org.apache.commons:commons-math3:3.6.1")
    compileOnly("org.jetbrains.kotlin:kotlin-reflect")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

kotlin {
    jvmToolchain(21)
}

lavalinkPlugin {
    name.set("lavaspectro")
    apiVersion.set("4")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    publications {
        // SNAPSHOT (auto-newest)
        create<MavenPublication>("snapshot") {
            version = mavenSnapshotVersion  // Direct assignment
            from(components["java"])
        }
        
        // Commit-specific
        create<MavenPublication>("commit") {
            version = commitVersion  // Direct assignment
            from(components["java"])
            artifact(tasks.jar.get()) {
                classifier = "commit-${commitHash}"
            }
        }
    }
    
    repositories {
        if (System.getenv("MAVEN_REPOSITORY") != null || project.hasProperty("MAVEN_REPOSITORY")) {
            create<MavenArtifactRepository>("reposilite") {
                val repoUrl = System.getenv("MAVEN_REPOSITORY")?.takeIf { it.isNotBlank() } 
                    ?: project.property("MAVEN_REPOSITORY").toString()
                val finalUrl = if (isSnapshot) "$repoUrl/snapshots" else "$repoUrl/releases"
                url = uri(finalUrl)
                credentials {
                    username = System.getenv("MAVEN_USERNAME") ?: (project.findProperty("MAVEN_USERNAME") as? String ?: "")
                    password = System.getenv("MAVEN_TOKEN") ?: (project.findProperty("MAVEN_TOKEN") as? String ?: "")
                }
            }
        }
    }
}

tasks.register("printVersion") {
    doLast { println(version) }
}
