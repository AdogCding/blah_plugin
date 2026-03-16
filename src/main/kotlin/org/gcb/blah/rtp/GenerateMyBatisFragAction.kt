package org.gcb.blah.rtp

import com.intellij.database.psi.DbTable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys

class GenerateMyBatisFragAction: AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val keys = e.getData(LangDataKeys.PSI_ELEMENT_ARRAY)
        e.presentation.isEnabledAndVisible = keys?.any { it is DbTable } ?: false
    }
    override fun actionPerformed(p0: AnActionEvent) {
        
    }
}