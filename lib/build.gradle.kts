plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    // Apply the maven-publish plugin for publishing to a Maven repository.
    `maven-publish`
    signing
    jacoco
}

group = "jp.vemi"
version = "1.0.0-beta1"

repositories {
    mavenCentral()
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")

    // Mockito for mocking in tests.
    testImplementation("org.mockito:mockito-core:5.2.0")

    // Database dependencies for testing.
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation("mysql:mysql-connector-java:8.0.33")
    
    // TestContainers for integration testing
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:mysql:1.19.3")
    
    // AssertJ for fluent assertions
    testImplementation("org.assertj:assertj-core:3.24.2")

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api("org.mybatis:mybatis:3.5.13")
    api("org.mybatis:mybatis-spring:3.0.2")

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation("org.mybatis.generator:mybatis-generator-core:1.4.2")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.apache.commons:commons-dbcp2:2.13.0")

    // Runtime only dependencies are not added to the compile classpath of projects that depend on this project.
    runtimeOnly("mysql:mysql-connector-java:8.0.32")

    // Lombok for generating boilerplate code.
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

    // SLF4J for logging
    implementation("org.slf4j:slf4j-api:2.0.1")
    implementation("ch.qos.logback:logback-classic:1.4.5")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    reports {
        html.required.set(true)
        junitXml.required.set(false)
    }
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(false)
        csv.required.set(false)
        html.required.set(true)
    }
}