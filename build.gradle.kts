plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "org.gcb"
version = "1.0.2-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea("2024.3.6")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here:


        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.database")
    }
    implementation("ognl:ognl:3.3.4")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243.26574"
        }

        changeNotes = """
        [SQL解析] 新增针对静态 SQL 文本的参数提取机制，支持 #{param} 变量的高亮与声明跳转。
        [动态SQL] 新增对 <if> 标签中 test 属性的 OGNL 表达式解析与引用绑定。
        [动态SQL] 新增对 <foreach> 标签中 collection 属性的解析支持，精准定位集合参数。
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    buildSearchableOptions {
        enabled = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
