package org.gcb.blah.rtp

import com.intellij.database.psi.DbTable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys

class GenerateMyBatisFragAction: AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(LangDataKeys.PSI_ELEMENT) is DbTable
    }
    override fun actionPerformed(p0: AnActionEvent) {
        
    }
}