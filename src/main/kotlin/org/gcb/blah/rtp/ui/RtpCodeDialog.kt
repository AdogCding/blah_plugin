package org.gcb.blah.rtp.ui

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.WriteActionAutoCloseable
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.ui.EditorTextField
import com.intellij.ui.LanguageTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel

class RtpCodeDialog(
    val project: Project,
    private val generatedCode: String,
    private val fileType: FileType
) : DialogWrapper(project) {

    init {
        title = "Rtp Code Generator"
        init()
    }

    fun formatCode(document: Document) {
        PsiDocumentManager.getInstance(project).getPsiFile(document)?.let { psiFile ->
            WriteCommandAction.runWriteCommandAction(project) {
                CodeStyleManager.getInstance(project).reformat(psiFile)
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        val language = (fileType as LanguageFileType).language
        val editorPanel = LanguageTextField(language, project, generatedCode, false)

        formatCode(editorPanel.document)

        editorPanel.isViewer = true
        editorPanel.setOneLineMode(false)

        editorPanel.addSettingsProvider { editor ->
            editor.settings.isLineNumbersShown = true
            editor.settings.isFoldingOutlineShown = true
            editor.settings.isVirtualSpace = false
            editor.colorsScheme = EditorColorsManager.getInstance().globalScheme
        }
        val panel = JPanel(BorderLayout())
        panel.add(editorPanel, BorderLayout.CENTER)
        return panel
    }

    override fun getInitialSize(): Dimension {
        return JBUI.size(800, 600)
    }

    // 重写底部按钮：默认有 OK 和 Cancel，我们把它们改成 "复制到剪贴板" 和 "关闭"
    override fun createActions(): Array<Action> {
        val copyAction = object : DialogWrapperAction("复制到剪贴板") {
            override fun doAction(e: ActionEvent?) {
                // 将代码写入系统剪贴板
                CopyPasteManager.getInstance().setContents(StringSelection(generatedCode))
                // 可选：弹个小气泡提示复制成功
                Messages.showInfoMessage("代码已复制到剪贴板！", "成功")
            }
        }
        // 返回我们自定义的 Action 数组
        return arrayOf(copyAction, cancelAction)
    }

    override fun dispose() {
        super.dispose()
    }

}