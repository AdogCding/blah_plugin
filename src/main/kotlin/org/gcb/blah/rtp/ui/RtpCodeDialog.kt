package org.gcb.blah.rtp.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.testFramework.LightVirtualFile
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
    private val highlighter: EditorHighlighter,
    private val fileType: FileType,
    private val fileSuffix: String,
    private val demoFileName: String = "DemoFile"
): DialogWrapper(project) {

    private lateinit var previewEditor: Editor

    init {
        title = "Rtp Code Generator"
        init()
    }

    override fun createCenterPanel(): JComponent {

        val psiFile = LightVirtualFile(
            "$demoFileName.$fileSuffix",
            fileType,
            generatedCode
        )

        val document = FileDocumentManager.getInstance().getDocument(psiFile)
            ?: throw RuntimeException("Document not found")

        previewEditor = EditorFactory.getInstance().createViewer(document, project)

        (previewEditor as EditorEx).highlighter = highlighter

        val settings = previewEditor.settings
        settings.isLineNumbersShown = true       // 显示行号
        settings.isFoldingOutlineShown = true    // 显示折叠箭头
        settings.isVirtualSpace = false          // 关闭虚拟空间
        settings.isLineMarkerAreaShown = false   // 隐藏左侧的断点区域
        settings.isUseSoftWraps = true        // 如果你想让太长的代码自动折行显示，取消这行注释

        val panel = JPanel(BorderLayout())
        panel.add(previewEditor.component, BorderLayout.CENTER)
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
        if (this::previewEditor.isInitialized) {
            EditorFactory.getInstance().releaseEditor(previewEditor)
        }
        super.dispose()
    }

}