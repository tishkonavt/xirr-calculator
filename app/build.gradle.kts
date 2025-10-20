plugins {
    kotlin("jvm") version "1.9.20"
    application
    id("com.google.protobuf") version "0.9.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.xirr"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    
    // XChart для графиков
    implementation("org.knowm.xchart:xchart:3.8.5")
    
    // gRPC
    implementation(platform("io.grpc:grpc-bom:1.58.0"))
    implementation("io.grpc:grpc-netty")
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
    implementation("io.grpc:grpc-services")
    implementation("com.google.protobuf:protobuf-java:3.24.0")
    
    // Аннотации
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    
    // Логирование
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // Тесты
    testImplementation(kotlin("test"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.0"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.58.0"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDirs(
                "build/generated/source/proto/main/grpc",
                "build/generated/source/proto/main/java"
            )
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.xirr.MainKt")  // ← Изменили на Main
}

// Shadow JAR для удобного распространения
tasks.shadowJar {
    archiveBaseName.set("xirr-calculator")
    archiveClassifier.set("")
    
    mergeServiceFiles()
    
    manifest {
        attributes["Main-Class"] = "com.xirr.MainKt"
    }
}