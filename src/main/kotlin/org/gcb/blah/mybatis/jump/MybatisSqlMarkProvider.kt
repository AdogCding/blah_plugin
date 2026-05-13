package org.gcb.blah.mybatis.jump

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.PsiTargetNavigator
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiKeyword
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.awt.RelativePoint
import org.gcb.blah.MyMessageBundle
import org.gcb.blah.mybatis.common.MybatisSqlUsageUtils
import java.awt.event.MouseEvent

class MybatisSqlMarkProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (!MybatisSqlUsageUtils.isMybatisDmlTag(element)) {
            return
        }
        val sql = MybatisSqlUsageUtils.findSqlIdOfXmlRawSql(element as XmlTag) ?: return
        val marker = createMyBatisXmlLineMarkerFor(element, sql)
        result.add(marker)
    }

    private fun createMyBatisXmlLineMarkerFor(
        element: XmlTag,
        myBatisDmlSql: MyBatisDmlSql
    ): RelatedItemLineMarkerInfo<PsiElement> {
        return RelatedItemLineMarkerInfo(
            element,
            element.textRange,
            AllIcons.Gutter.ImplementedMethod,
            { "点击查找 Java 调用处" },
            MyBatisHelperNavigationHandler(myBatisDmlSql),
            GutterIconRenderer.Alignment.RIGHT,
            { emptyList() }
        )
    }

    private inner class MyBatisHelperNavigationHandler(val myBatisDmlSql: MyBatisDmlSql) :
        GutterIconNavigationHandler<PsiElement> {
        override fun navigate(p0: MouseEvent?, element: PsiElement?) {
            if (element == null) {
                return
            }
            val project = element.project
            val toolClassName = PluginSettingState.getInstance(project).toolClassName
            if (toolClassName.isBlank()) {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("MyBatisPlugin.Notification")
                    .createNotification(
                        MyMessageBundle.message("mybatis-helper.notification.title"),
                        "请先配置 MyBatis 工具类路径",
                        NotificationType.WARNING
                    )
                    .addAction(
                        NotificationAction.createSimple("去设置") {
                            ShowSettingsUtil.getInstance()
                                .showSettingsDialog(project, PluginSettingsConfigurable::class.java)
                        }
                    )
                    .notify(project)
                return
            }
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "正在搜索引用", true) {
                var foundTargets = mutableListOf<PsiElement>()
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    ApplicationManager.getApplication().runReadAction {
                        if (indicator.isCanceled) {
                            return@runReadAction
                        }
                        val rtpUsages = MybatisSqlUsageUtils.findMethod(project, toolClassName, myBatisDmlSql)
                        foundTargets.addAll(rtpUsages)
                        if (PluginSettingState.getInstance(project).isLooking4NativeMapper) {
                            val nativeUsages = findNativeMethod(project, myBatisDmlSql)
                            foundTargets.addAll(nativeUsages)
                        }

                    }
                }


                override fun onSuccess() {
                    if (project.isDisposed) {
                        return
                    }
                    if (foundTargets.isEmpty()) {
                        return
                    }
                    if (foundTargets.size == 1) {
                        (foundTargets.first() as? NavigatablePsiElement)?.navigate(true)
                        return
                    }
                    PsiTargetNavigator(foundTargets)
                        .createPopup(project, "选择跳转目标") { element ->
                            (element as NavigatablePsiElement).navigate(true)
                            true
                        }.show(RelativePoint(p0!!))
                }
            })
        }

    }

    fun findNativeMethod(
        project: Project,
        myBatisDmlSql: MyBatisDmlSql
    ): List<PsiElement> {
        val res = mutableListOf<PsiElement>()
        val scope = GlobalSearchScope.projectScope(project)
        val psiSearchHelper = PsiSearchHelper.getInstance(project)
        psiSearchHelper.processElementsWithWord({ psiEl, _ ->
            if (psiEl !is PsiIdentifier) {
                return@processElementsWithWord true
            }
            res.addAll(findAnnotationRef(project, psiEl, myBatisDmlSql))
            true
        }, scope, myBatisDmlSql.sqlId, UsageSearchContext.IN_CODE, true)
        return res
    }






    private fun findAnnotationRef(project: Project, literal: PsiIdentifier, myBatisDmlSql: MyBatisDmlSql): List<PsiElement> {
        // 首先检查一下是不是Mybatis的方法
        val targetMethod = PsiTreeUtil.getParentOfType(literal, PsiMethod::class.java) ?: return emptyList()
        val clazz = PsiTreeUtil.getParentOfType(targetMethod, PsiClass::class.java) ?: return emptyList()
        val anno = PluginSettingState.getInstance(project).mybatisAnnotationMapperName
        // class一定要是interface
        val keywordOfClazz = PsiTreeUtil.getChildOfType(clazz, PsiKeyword::class.java) ?: return emptyList()
        if (keywordOfClazz.text.trim() != "interface") {
            return emptyList()
        }
        if (!clazz.hasAnnotation(anno)) {
            return emptyList()
        }
        val fullName = "${clazz.qualifiedName}.${targetMethod.name}"
        if (fullName != myBatisDmlSql.toFullName()) {
            return mutableListOf()
        }
        return mutableListOf(targetMethod)
    }


}