import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  java
  application
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.lonpe"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "5.0.3"
val junitJupiterVersion = "5.9.1"

val mainVerticleName = "org.lonpe.MainVerticle"
val launcherClassName = "io.vertx.launcher.application.VertxApplication"

application {
  mainClass.set(launcherClassName)
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-launcher-application")
  implementation("io.vertx:vertx-web-validation")
  implementation("io.vertx:vertx-auth-jwt")
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-pg-client")
  implementation("io.vertx:vertx-hazelcast")
  implementation("io.vertx:vertx-mail-client")
  implementation("io.vertx:vertx-rx-java3")
  implementation("com.ongres.scram:scram-client:2.1")

  implementation("io.vertx:vertx-config:5.0.3")

  implementation("commons-logging:commons-logging:1.1.1")
  implementation("org.slf4j:slf4j-api:2.0.16")
  implementation("ch.qos.logback:logback-classic:1.4.5")


  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}


application {
  mainClass.set("org.lonpe.Application")
   applicationDefaultJvmArgs = listOf(
           // "-XX:MaxRAMPercentage=75.0",
           // "-Dspring.profiles.active=development",
           // hazelcastArgs
       )    
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf(mainVerticleName)
}
