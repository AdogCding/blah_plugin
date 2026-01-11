package org.gcb.blah

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaCodeFragmentFactory
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.EditorTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class PluginSettingsConfigurable(private val project: Project) : Configurable {
    private val settings = PluginSettingState.getInstance(project)
    private lateinit var myEditorTextField: EditorTextField

    // 设置页面的显示名称
    override fun getDisplayName(): String = "MyBatis Tool Configuration"

    override fun createComponent(): JComponent {
        // 1. 创建一个 Java 代码片段 (Code Fragment)
        // 这个片段的上下文被设定为“类引用”，所以 IDEA 知道这里应该填类名
        val codeFragment = JavaCodeFragmentFactory.getInstance(project)
            .createReferenceCodeFragment(
                settings.toolClassName, // 初始内容
                null,
                true,
                true
            )

        val document = PsiDocumentManager.getInstance(project)
            .getDocument(codeFragment)
            ?: throw IllegalStateException("无法为 CodeFragment 创建 Document")
        // 2. 使用这个片段创建一个编辑器文本框
        // JavaFileType.INSTANCE 让它拥有 Java 的语法高亮
        myEditorTextField = EditorTextField(document, project, JavaFileType.INSTANCE)

        // 3. 构建 UI
        return panel {
            row("工具类全限定名:") {
                // 将自定义组件 wrap 进 DSL
                cell(myEditorTextField)
                    .align(AlignX.FILL) // 让它填满水平空间
            }
            row {
                text("插件将查询该类内所有方法的usage")
            }
        }
    }

    override fun isModified(): Boolean {
        return myEditorTextField.text != settings.toolClassName
    }

    // 点击 "Apply" 或 "OK" 时调用
    override fun apply() {
        val inputClassName = myEditorTextField.text.trim()
        if (inputClassName.isBlank()) {
            settings.toolClassName = ""
            return
        }
        // --- 校验逻辑 ---
        val javaClass = JavaPsiFacade.getInstance(project)
            .findClass(inputClassName, GlobalSearchScope.allScope(project))

        if (javaClass == null) {
            throw ConfigurationException("在当前项目中未找到类: $inputClassName")
        }
        val oldSetting = settings.toolClassName
        settings.toolClassName = inputClassName
        if (oldSetting != settings.toolClassName) {
            DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }

    override fun reset() {
        myEditorTextField.text = settings.toolClassName
    }
}