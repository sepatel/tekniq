import net.researchgate.release.GitAdapter
import net.researchgate.release.ReleaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun prop(key: String) = properties[key]?.toString()

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin", "kotlin-gradle-plugin", properties["kotlin_version"].toString())
    }
}

plugins {
    kotlin("jvm") version "1.9.20" apply false
    id("net.researchgate.release") version "2.8.1"
    `java-library`
    signing
    `maven-publish`
}

defaultTasks("clean", "build")

fun ReleaseExtension.git(configureFn: GitAdapter.GitConfig.() -> Unit) {
    (propertyMissing("git") as GitAdapter.GitConfig).configureFn()
}
release {
    failOnCommitNeeded = true
    failOnPublishNeeded = false
    failOnSnapshotDependencies = true
    failOnUnversionedFiles = true
    failOnUpdateNeeded = false
    revertOnFail = true

    git {
        requireBranch = "master"
        pushToRemote = "origin"
    }
}

tasks {
    val modules = listOf("tekniq-core", "tekniq-cache", "tekniq-jdbc", "tekniq-rest")
    "beforeReleaseBuild" {
        modules.forEach { dependsOn(":$it:test") }
    }
    "afterReleaseBuild" {
        dependsOn("publish")
        modules.forEach { dependsOn(":$it:publish") }
    }
}

allprojects {
    apply(plugin = "kotlin")
    apply<JavaLibraryPlugin>()
    apply<SigningPlugin>()
    apply<MavenPublishPlugin>()

    group = "io.tekniq"

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("reflect"))

        testImplementation(kotlin("test"))
        testImplementation("io.kotest:kotest-runner-junit5:${properties["kotest_version"]}")
        testImplementation("io.kotest:kotest-assertions-core:${properties["kotest_version"]}")
        testImplementation("io.kotest:kotest-property:${properties["kotest_version"]}")
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        testImplementation("ch.qos.logback", "logback-classic", prop("logback_version"))
    }


    tasks {
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_11.majorVersion
                apiVersion = "1.9"
                languageVersion = "1.9"
                javaParameters = true
                suppressWarnings = true
                freeCompilerArgs = listOf("-Xallow-result-return-type")
            }
        }

        withType<Sign>().configureEach {
            onlyIf { !version.toString().endsWith("-SNAPSHOT") }
        }

        withType<Test> {
            useJUnitPlatform()
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_11.majorVersion))
        }
        withJavadocJar()
        withSourcesJar()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                pom {
                    name.set(project.name)
                    packaging = "jar"
                    description.set("Kotlin idiomatic framework for improved JVM/Application development")
                    url.set("https://github.com/sepatel/tekniq")

                    scm {
                        connection.set("scm:git:git://github.com/sepatel/tekniq.git")
                        developerConnection.set("scm:git:git://github.com/sepatel/tekniq")
                        url.set("https://github.com/sepatel/tekniq")
                    }

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://raw.githubusercontent.com/sepatel/tekniq/master/LICENSE")
                        }
                    }

                    developers {
                        developer {
                            id.set("sepatel")
                            name.set("Sejal Patel")
                            email.set("sejal@tekniq.io")
                        }
                    }
                }
            }
        }
        repositories {
            maven {
                name = "sonatype"
                if (version.toString().endsWith("-SNAPSHOT")) {
                    setUrl("https://oss.sonatype.org/content/repositories/snapshots/")
                } else {
                    setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                }
                credentials {
                    username = project.findProperty("ossrhUsername").toString()
                    password = project.findProperty("ossrhPassword").toString()
                }
            }
        }
    }

    signing {
        useGpgCmd()
        isRequired = true
        sign(publishing.publications)
    }
}

project(":tekniq-cache") {
    dependencies {
        implementation(project(":tekniq-core"))
        implementation("com.github.ben-manes.caffeine", "caffeine", prop("caffeine_version"))
    }
}
project(":tekniq-jdbc") {
    dependencies {
        implementation(project(":tekniq-core"))
        testImplementation("org.hsqldb:hsqldb:2.3.4")
    }
}
project(":tekniq-rest") {
    dependencies {
        implementation("com.fasterxml.jackson.core", "jackson-core", prop("jackson_version"))
        implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", prop("jackson_version"))
        testImplementation("io.javalin:javalin:${properties["javalin_version"]}")
        testImplementation("ch.qos.logback:logback-classic:${properties["logback_version"]}")
    }
}
