plugins {
    id("org.springframework.boot") version "3.5.13" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.hibernate.orm") version "6.6.45.Final" apply false
    id("org.graalvm.buildtools.native") version "0.10.6" apply false
}

group = "dn"
version = "0.0.1-SNAPSHOT"

extra["springCloudVersion"] = "2025.0.1"

subprojects {
    group = "dn"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
