package org.gcb.blah.rtp.actions

import com.intellij.database.psi.DbTable
import com.intellij.database.util.DasUtil
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import org.gcb.blah.rtp.ui.RtpCodeDialog
import org.gcb.blah.rtp.entity.TableDefEntity
import org.gcb.blah.rtp.templates.RtpEntityCodeTemplate

class RtpEntityCodeGenerateAction: AnAction("Rtp Entity Code Generate") {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val keys = e.getData(LangDataKeys.PSI_ELEMENT_ARRAY)
        e.presentation.isEnabledAndVisible = keys?.any { it is DbTable } ?: false
    }

    override fun actionPerformed(p0: AnActionEvent) {
        val project = p0.project!!
        val keys = p0.getData(LangDataKeys.PSI_ELEMENT_ARRAY)
        val table = keys?.first() as? DbTable ?: return
        val tableName = table.name
        val cols = DasUtil.getColumns(table).map { it.name }.toList()
        val highlighter = EditorHighlighterFactory.getInstance()
            .createEditorHighlighter(project, JavaFileType.INSTANCE)
        RtpCodeDialog(
            project, RtpEntityCodeTemplate.getEntityCode(TableDefEntity(tableName, cols)),
            highlighter,
            JavaFileType.INSTANCE,
            "java",
            tableName
        ).show()
    }

}