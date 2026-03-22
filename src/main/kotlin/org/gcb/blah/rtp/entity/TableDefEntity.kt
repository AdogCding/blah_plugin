package org.gcb.blah.rtp.entity

import com.intellij.database.psi.DbTable
import com.intellij.database.util.DasUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys

data class TableDefEntity(
    val tableName: String,
    val columns: List<String>,
    val primaryKeys: List<String>,
)


object TableDefService {
    fun getTableDefFromAction(action: AnActionEvent): TableDefEntity {
        val keys = action.getData(LangDataKeys.PSI_ELEMENT_ARRAY)
        val table = keys?.first() as? DbTable ?: throw IllegalStateException("Table does not contain table")
        val tableName = table.name
        val cols = DasUtil.getColumns(table).map { it.name }.toList()
        val primaryKeys = DasUtil.getColumns(table).filter { DasUtil.isPrimary(it) }.map { it.name }.toList()
        return TableDefEntity(tableName, cols, primaryKeys)
    }
}