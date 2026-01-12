package org.gcb.blah

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class RefreshMybatisMarkerAction: AnAction("Refresh Mybatis Marker") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        // 核心：强制重启当前项目的代码分析守护进程
        DaemonCodeAnalyzer.getInstance(project).restart()

        // 这是一个提示，告诉你运行成功了 (调试用，发布时可以去掉)
        Messages.showInfoMessage(project, "MyBatis 插件分析已强制刷新", "刷新成功")
    }

}