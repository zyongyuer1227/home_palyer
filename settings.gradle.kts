pluginManagement {
    repositories {
        maven("https://mirrors.tencent.com/nexus/repository/maven-public")
        maven("https://repo.huaweicloud.com/repository/maven")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://mirrors.tencent.com/nexus/repository/maven-public")
        maven("https://repo.huaweicloud.com/repository/maven")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        google()
        mavenCentral()
    }
}

rootProject.name = "IPTVPlayer"
include(":app")
