/*
 * Copyright 2023 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

///////////////////////////////
//////    PUBLISH TO SONATYPE / MAVEN CENTRAL
////// TESTING : (to local maven repo) <'publish and release' - 'publishToMavenLocal'>
////// RELEASE : (to sonatype/maven central), <'publish and release' - 'publishToSonatypeAndRelease'>
///////////////////////////////

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS_FULL   // always show the stacktrace!

plugins {
    id("com.dorkbox.GradleUtils") version "3.18"
    id("com.dorkbox.Licensing") version "2.28"
    id("com.dorkbox.VersionUpdate") version "2.8"
    id("com.dorkbox.GradlePublish") version "1.22.2"

    kotlin("jvm") version "1.9.0"
}

object Extras {
    // set for the project
    const val description = "Linux, MacOS, or Windows (notification/growl/toast/) popups for the desktop for Java 8+"
    const val group = "com.dorkbox"
    const val version = "4.5"

    // set as project.ext
    const val name = "Notify"
    const val id = "Notify"
    const val vendor = "Dorkbox LLC"
    const val vendorUrl = "https://dorkbox.com"
    const val url = "https://git.dorkbox.com/dorkbox/Notify"
}

///////////////////////////////
/////  assign 'Extras'
///////////////////////////////
GradleUtils.load("$projectDir/../../gradle.properties", Extras)
GradleUtils.defaults()
GradleUtils.compileConfiguration(JavaVersion.VERSION_1_8)
GradleUtils.jpms(JavaVersion.VERSION_1_9)

licensing {
    license(License.APACHE_2) {
        author(Extras.vendor)
        url(Extras.url)
        note(Extras.description)
    }
}

val exampleCompile by configurations.creating { extendsFrom(configurations.implementation.get()) }
sourceSets {
    main {
        resources {
            setSrcDirs(listOf("resources"))
            include("*.png")
        }
    }

    test {
        resources {
            setSrcDirs(listOf("test"))
            include("*.png")
        }
    }
}

///////////////////////////////
//////    Task defaults
///////////////////////////////
tasks.jar.get().apply {
    manifest {
        // https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html
        attributes["Name"] = Extras.name

        attributes["Specification-Title"] = Extras.name
        attributes["Specification-Version"] = Extras.version
        attributes["Specification-Vendor"] = Extras.vendor

        attributes["Implementation-Title"] = "${Extras.group}.${Extras.id}"
        attributes["Implementation-Version"] = GradleUtils.now()
        attributes["Implementation-Vendor"] = Extras.vendor
    }
}

dependencies {
    api("com.dorkbox:PropertyLoader:1.4")
    api("com.dorkbox:TweenEngine:9.2")
    api("com.dorkbox:SwingActiveRender:1.5")
    api("com.dorkbox:Updates:1.1")
    api("com.dorkbox:Utilities:1.48")
}

task<Jar>("jarExample") {
    archiveBaseName.set("Notify-Example")
    group = BasePlugin.BUILD_GROUP
    description = "Create an all-in-one example for testing, on a standard Java installation"

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    outputs.upToDateWhen { false }

    from(sourceSets["main"].output)
    from(sourceSets["test"].output)

    from(exampleCompile.map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.DSA", "META-INF/*.SF")
    }

    manifest {
        attributes["Main-Class"] = "dorkbox.notify.NotifyTest"
    }
}

publishToSonatype {
    groupId = Extras.group
    artifactId = Extras.id
    version = Extras.version

    name = Extras.name
    description = Extras.description
    url = Extras.url

    vendor = Extras.vendor
    vendorUrl = Extras.vendorUrl

    issueManagement {
        url = "${Extras.url}/issues"
        nickname = "Gitea Issues"
    }

    developer {
        id = "dorkbox"
        name = Extras.vendor
        email = "email@dorkbox.com"
    }
}
