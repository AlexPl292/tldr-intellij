package com.github.alexpl292.tldrintellij.services

import com.github.alexpl292.tldrintellij.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
