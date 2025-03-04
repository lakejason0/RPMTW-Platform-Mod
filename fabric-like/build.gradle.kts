architectury {
    common(project.property("enabled_platforms").toString().split(","))
}

val common by configurations.registering
configurations {
    compileClasspath {
        extendsFrom(common.get())
    }

    runtimeClasspath {
        extendsFrom(common.get())
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://bai.jfrog.io/artifactory/maven") //ModMenu
        content {
            includeGroup("com.terraformersmc")
        }
    }
    maven {
        // Patchouli
        url = uri("https://maven.blamejared.com")
    }
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")
    modApi("net.fabricmc:fabric-language-kotlin:${project.property("fabric-kotlin_version")}")
    // Remove the next line if you don't want to depend on the API
    modApi("dev.architectury:architectury-fabric:${project.property("architectury_version")}")

//    modImplementation("vazkii.patchouli:Patchouli:${project.property("patchouli_version")}-FABRIC-SNAPSHOT")
    modImplementation("com.terraformersmc:modmenu:${project.property("modmenu_version")}")

    "compileClasspath"(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
}

val accessWidenerFile = project(":common").file("src/main/resources/rpmtw_platform_mod.accesswidener")

loom {
    accessWidenerPath.set(accessWidenerFile)
}