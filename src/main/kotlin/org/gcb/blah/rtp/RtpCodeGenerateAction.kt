package org.gcb.blah.rtp

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory

class RtpCodeGenerateAction: AnAction("Rtp Code Generate") {
    override fun actionPerformed(p0: AnActionEvent) {
        val project = p0.project!!
        val highlighter = EditorHighlighterFactory.getInstance()
            .createEditorHighlighter(project, JavaFileType.INSTANCE)
        RtpCodeDialog(project, "public class Good {}",
            highlighter,
            JavaFileType.INSTANCE,
            "java").show()
    }

}