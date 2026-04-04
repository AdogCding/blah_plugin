package org.gcb.blah.mybatis.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import org.gcb.blah.mybatis.jump.MybatisCheckSeverity
import org.gcb.blah.mybatis.jump.PluginSettingState

class SelectStmtResultTypeAnnotator : Annotator {
    override fun annotate(p0: PsiElement, p1: AnnotationHolder) {
        val project = p0.project
        val mybatisCheckSeverity = PluginSettingState.getInstance(project).mybatisCheckSeverity
        if (mybatisCheckSeverity == MybatisCheckSeverity.IGNORE.code) {
            return
        }
        if (p0 !is XmlTag) {
            return
        }
        if (!p0.isSelectStmt()) {
            return
        }
        p1.newAnnotation(map2HighlightSeverity(mybatisCheckSeverity), "select columns not match result type")
            .range(p0.textRange)
            .tooltip("Error")
            .create()
    }
}

private fun map2HighlightSeverity(severity: String): HighlightSeverity = when (severity) {
    MybatisCheckSeverity.ERROR.code -> HighlightSeverity.ERROR
    MybatisCheckSeverity.WARNING.code -> HighlightSeverity.WARNING
    MybatisCheckSeverity.INFO.code -> HighlightSeverity.WEAK_WARNING
    else -> HighlightSeverity.INFORMATION
}

private fun XmlTag.isSelectStmt(): Boolean {
    return this.name == "select"
}
