package org.gcb.blah.stats.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel

object TableOfMethodStatsPanelUtils {
    fun createPanel(data: Array<Array<String>>, columns: Array<String>): JPanel {
        val model = object : DefaultTableModel(data, columns) {
            override fun isCellEditable(row: Int, column: Int): Boolean = false
        }
        val table = JBTable(model).apply {
            this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            this.setShowGrid(true)
            this.autoResizeMode = JBTable.AUTO_RESIZE_ALL_COLUMNS
            columnModel.getColumn(0).apply {
                this.preferredWidth = 50
                this.maxWidth = 80
            }
        }
        return panel {
            row {
                // cell 包装 JBScrollPane，让表格跟随窗口拉伸扩展
                cell(JBScrollPane(table))
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.Align.FILL)
            }.resizableRow()
        }
    }
}