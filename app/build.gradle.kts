plugins {
    kotlin("jvm") version "1.9.20"
    application
    id("com.google.protobuf") version "0.9.1"
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
    implementation("io.grpc:grpc-netty:1.58.0")
    implementation("io.grpc:grpc-protobuf:1.58.0")
    implementation("io.grpc:grpc-stub:1.58.0")
    implementation("io.grpc:grpc-services:1.58.0")
    implementation("com.google.protobuf:protobuf-java:3.24.0")

    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    
    // Ktor для веб-сервера
    implementation("io.ktor:ktor-server-core:2.3.5")
    implementation("io.ktor:ktor-server-netty:2.3.5")
    implementation("io.ktor:ktor-server-html-builder:2.3.5")
    
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

// Указываем где искать сгенерированные файлы
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
    mainClass.set("com.xirr.WebServerKt")
}