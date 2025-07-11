plugins {
    `java-library`
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/java"))
        }
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
    test {
        java {
            setSrcDirs(listOf("src/test/java"))
        }
        resources {
            setSrcDirs(listOf("src/test/resources"))
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":lib"))
    api("org.springframework.boot:spring-boot-autoconfigure:3.2.2")
    api("org.springframework:spring-context:6.1.3")
    api("org.springframework:spring-beans:6.1.3")
    api("org.mybatis:mybatis-spring:3.0.3")
    api("org.mybatis:mybatis-spring:3.0.2")
    
    implementation("org.springframework:spring-tx:5.3.29")
    implementation("org.springframework:spring-jdbc:5.3.29")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("com.h2database:h2:2.2.224")
}