package org.gcb.blah.mybatis

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.psi.JavaCodeFragmentFactory
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.EditorTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class PluginSettingsConfigurable(private val project: Project) : Configurable {
    private val settings = PluginSettingState.getInstance(project)
    private lateinit var dbUtilToolEditElement: EditorTextField
    private lateinit var mybatisMapperAnnotation: EditorTextField
    private lateinit var panel: DialogPanel

    // 设置页面的显示名称
    override fun getDisplayName(): String = "MyBatis Tool Configuration"

    override fun createComponent(): JComponent {
        // 1. 创建一个 Java 代码片段 (Code Fragment)
        // 这个片段的上下文被设定为“类引用”，所以 IDEA 知道这里应该填类名
        val dbUtilCodeFragment = JavaCodeFragmentFactory.getInstance(project)
            .createReferenceCodeFragment(
                settings.toolClassName, // 初始内容
                null,
                true,
                true
            )
        val mybatisAnnotationCodeFragment = JavaCodeFragmentFactory.getInstance(project)
            .createReferenceCodeFragment(
                settings.mybatisAnnotationMapperName, // 初始内容
                null,
                true,
                true
            )

        val dbUtilDoc = PsiDocumentManager.getInstance(project)
            .getDocument(dbUtilCodeFragment)
            ?: throw IllegalStateException("无法为 CodeFragment 创建 Document")
        val mybatisAnnotationDoc = PsiDocumentManager.getInstance(project)
            .getDocument(mybatisAnnotationCodeFragment)
            ?: throw IllegalStateException("无法为 CodeFragment 创建 Document")
        // 2. 使用这个片段创建一个编辑器文本框
        // JavaFileType.INSTANCE 让它拥有 Java 的语法高亮
        dbUtilToolEditElement = EditorTextField(dbUtilDoc, project, JavaFileType.INSTANCE)
        mybatisMapperAnnotation = EditorTextField(mybatisAnnotationDoc, project, JavaFileType.INSTANCE)
        // 3. 构建 UI
        panel = panel {
            row("数据库工具类全限定名:") {
                // 将自定义组件 wrap 进 DSL
                cell(dbUtilToolEditElement)
                    .align(AlignX.FILL) // 让它填满水平空间
            }
            row("Mybatis注解:") {
                // 将自定义组件 wrap 进 DSL
                cell(mybatisMapperAnnotation)
                    .align(AlignX.FILL) // 让它填满水平空间
            }
            row {
                // 定义 Checkbox
                checkBox("寻找Mybatis的Mapper")
                    // 【关键】绑定数据：双向绑定到 state 的变量
                    .bindSelected(settings::isLooking4NativeMapper)
                    .comment("勾选后，将启用功能")
            }
        }
        return panel
    }

    override fun isModified(): Boolean {
        return dbUtilToolEditElement.text != settings.toolClassName
                || panel.isModified()
                || mybatisMapperAnnotation.text != settings.mybatisAnnotationMapperName
    }

    // 点击 "Apply" 或 "OK" 时调用
    override fun apply() {
        applyToolClassName()
        applyMybatisMapperAnnotation()
        panel.apply()
    }

    private fun applyEditElement(editorTextField: EditorTextField, assignPluginSettingState: (String) -> Unit) {
        val input = editorTextField.text.trim()
        if (input.isBlank()) {
            assignPluginSettingState("")
            return
        }
        // --- 校验逻辑 ---
        val javaClass = JavaPsiFacade.getInstance(project)
            .findClass(input, GlobalSearchScope.allScope(project))

        if (javaClass == null) {
            throw ConfigurationException("在当前项目中未找到类: $input")
        }
        assignPluginSettingState(input)
    }

    private fun applyMybatisMapperAnnotation() {
        applyEditElement(mybatisMapperAnnotation) {
            i -> settings.mybatisAnnotationMapperName = i
        }
    }

    private fun applyToolClassName() {
        applyEditElement(dbUtilToolEditElement) {
            i -> settings.toolClassName = i
        }
    }

    override fun reset() {
        dbUtilToolEditElement.text = settings.toolClassName
        mybatisMapperAnnotation.text = settings.mybatisAnnotationMapperName
    }
}