package org.gcb.blah.stats.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.gcb.blah.stats.ui.TableOfMethodStatsPanelUtils


class FindAllTablesOfMethodAction: AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        val data = arrayOf(
            arrayOf("1", "enable.feature.x", "true", "开启新功能 X"),
            arrayOf("2", "max.connections", "100", "最大连接数上限"),
            arrayOf("3", "timeout.ms", "5000", "请求超时时间(毫秒)")
        )
        val columnNames = arrayOf("ID", "Key", "Value", "Description")
        TableOfMethodStatsPanelUtils.createPanel(data, columnNames)
    }

}