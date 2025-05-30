plugins {
    `java-library`
    pmd
    jacoco
    alias(libs.plugins.lombok)
    alias(libs.plugins.dependencycheck)
    alias(libs.plugins.spring.dependency.management)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.axion.release)
}

repositories {
    if (project.hasProperty("jenkins-ci")) {
        maven {
            url = uri("${System.getProperty("nexus.url")}/repository/cognizone-group")
            credentials {
                username = System.getProperty("nexus.username")
                password = System.getProperty("nexus.password")
            }
            isAllowInsecureProtocol = true
        }
    }
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withJavadocJar()
    withSourcesJar()
}

scmVersion {
    tag.apply {
        prefix = "v"
        versionSeparator = ""
        branchPrefix = mapOf(
            "release/.*" to "release-v",
            "hotfix/.*" to "hotfix-v"
        )
    }
    nextVersion.apply {
        suffix = "SNAPSHOT"
        separator = "-"
    }
}

version = scmVersion.version
group = "zone.cogni.semanticz"

pmd {
    isIgnoreFailures = true
    isConsoleOutput = true
    toolVersion = "7.0.0"
    rulesMinimumPriority = 5
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
}

dependencies {
    // Core Jena dependencies
    implementation(libs.jena.arq)
    implementation(libs.jena.fuseki.main)
    implementation(libs.jena.tdb2)
    implementation("org.apache.jena:jena-core:${libs.versions.jena.get()}")
    implementation(libs.guava)
    
    // Spring framework (optional - only for Spring integration)
    compileOnly("org.springframework:spring-context:6.2.1")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.4.1")
    compileOnly(libs.jakarta.annotation.api)

    // Test dependencies
    testImplementation(libs.junit.jupiter)
    testImplementation("org.springframework:spring-context:6.2.1")
    testImplementation("org.springframework.boot:spring-boot-autoconfigure:3.4.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.1")
    testImplementation(libs.logback.classic)
    testImplementation("org.yaml:snakeyaml:2.0")
}

tasks.register("qualityCheck") {
    dependsOn(tasks.pmdMain)
    dependsOn(tasks.pmdTest)
    dependsOn(tasks.jacocoTestReport)
    dependsOn(tasks.dependencyCheckAnalyze)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

tasks.jar {
    from("${projectDir}") {
        include("LICENSE")
        into("META-INF")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group.toString()
            artifactId = "semanticz-jena-local-federation"
            version = scmVersion.version
            from(components["java"])

            pom {
                name.set("semanticz-jena-local-federation")
                description.set("Spring Boot component for Jena local federation functionality.")
                url.set("https://github.com/cognizone/semanticz-jena-local-federation")

                scm {
                    connection.set("scm:git@github.com:cognizone/semanticz-jena-local-federation.git")
                    developerConnection.set("scm:git@github.com:cognizone/semanticz-jena-local-federation.git")
                    url.set("https://github.com/cognizone/semanticz-jena-local-federation")
                }

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("cognizone")
                        name.set("Cognizone")
                        email.set("dev@cognizone.com")
                    }
                }
            }
        }
    }

    repositories {
        if (project.hasProperty("publishToCognizoneNexus")) {
            maven {
                credentials {
                    username = System.getProperty("nexus.username")
                    password = System.getProperty("nexus.password")
                }
                val releasesRepoUrl = "${System.getProperty("nexus.url")}/repository/cognizone-release"
                val snapshotsRepoUrl = "${System.getProperty("nexus.url")}/repository/cognizone-snapshot"
                url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                isAllowInsecureProtocol = true
            }
        }
        if (project.hasProperty("publishToMavenCentral")) {
            maven {
                credentials {
                    username = System.getProperty("ossrh.username")
                    password = System.getProperty("ossrh.password")
                }
                val stagingRepoUrl = "${System.getProperty("ossrh.url")}/service/local/staging/deploy/maven2"
                val snapshotsRepoUrl = "${System.getProperty("ossrh.url")}/content/repositories/snapshots"
                url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else stagingRepoUrl)
            }
        }
    }
}

signing {
    if (project.hasProperty("publishToMavenCentral") || project.hasProperty("publishToCognizoneNexus")) {
        sign(publishing.publications["mavenJava"])
    }
}

tasks.withType<Javadoc> {
    options {
        (this as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
    }
    isFailOnError = false
}
