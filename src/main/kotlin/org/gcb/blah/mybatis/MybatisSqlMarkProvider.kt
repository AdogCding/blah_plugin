package org.gcb.blah.mybatis

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
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.impl.JavaConstantExpressionEvaluator
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.awt.RelativePoint
import org.gcb.blah.MyMessageBundle
import java.awt.event.MouseEvent
import kotlin.run

class MybatisSqlMarkProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (!element.isMybatisDmlTag()) {
            return
        }
        val sql = findSqlId(element as XmlTag) ?: return
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
                    .getNotificationGroup("MyBatisPlugin.Notification") // 对应 xml 里的 id
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
                var foundTargets = emptyList<PsiElement>()
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    ApplicationManager.getApplication().runReadAction {
                        if (indicator.isCanceled) {
                            return@runReadAction
                        }
                        foundTargets = findMethod(project, toolClassName, myBatisDmlSql)
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
                        (foundTargets.first() as? NavigatablePsiElement)?.navigate(true) ?: return
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

    private fun PsiElement.isMybatisDmlTag(): Boolean {
        if (this !is XmlTag) {
            return false
        }
        val tagName = this.name
        return tagName in setOf("select", "insert", "delete", "update")
    }


    private fun findSqlId(mybatisDmlTag: XmlTag): MyBatisDmlSql? {
        val sqlId = mybatisDmlTag.getAttributeValue("id")
        if (mybatisDmlTag.parent == null || mybatisDmlTag.parent !is XmlTag) {
            return null
        }
        val mapperTag = mybatisDmlTag.parent as XmlTag

        if (mapperTag.name != "mapper" || mapperTag.getAttributeValue("namespace").isNullOrBlank()) {
            return null
        }
        val ns = mapperTag.getAttributeValue("namespace")
        if (sqlId.isNullOrBlank() || ns.isNullOrBlank()) {
            return null
        }
        return MyBatisDmlSql(sqlId, ns)
    }

    private fun findLiteralAndItsUsage(
        literal: PsiLiteralExpression,
        toolClassName: String,
        sqlId: MyBatisDmlSql
    ): List<PsiMethodCallExpression> {
        if (literal.value != sqlId.toFullName()) {
            return emptyList()
        }
        val res = mutableListOf<PsiMethodCallExpression>()
        res.addAll(getDirectUsages(literal, toolClassName))
        res.addAll(getLocalVariableUsages(literal, toolClassName))
        res.addAll(getSqlFieldUsages(literal, toolClassName))
        return res
    }

    /**
     * 根据字符串找到sqlId的使用函数
     * 1. sqlId作为字符串的一部分出现在一个函数调用中
     * e.g. DBUtils.selectList(SQL_NS + "sqlId", ..)
     *
     * 2. sqlId经过拼接后作为完整的sqlId
     * e.g. private static final String SQL_ID = SQL_NS + "sqlId"
     *      DBUtils.selectList(SQL_ID, ...)
     *
     * 3. sqlId直接属于字符串的一部分
     *  e.g. private static final String SQL_ID = "sn+sqlId"
     *       DBUtils.selectList(SQL_ID, ...)
     */
    fun findMethod(project: Project, toolClassName: String, myBatisDmlSql: MyBatisDmlSql): List<PsiElement> {
        val res = mutableListOf<PsiElement>()
        val scope = GlobalSearchScope.allScope(project)
        val psiSearchHelper = PsiSearchHelper.getInstance(project)
        psiSearchHelper.processElementsWithWord({ psiEl, _ ->
            if (psiEl.parent !is PsiLiteralExpression) {
                return@processElementsWithWord true
            }
            val literal = psiEl.parent as PsiLiteralExpression
            val trivialLiteralAndItsUsages = findLiteralAndItsUsage(literal, toolClassName, myBatisDmlSql)
            if (trivialLiteralAndItsUsages.isNotEmpty()) {
                res.addAll(trivialLiteralAndItsUsages)
            }
            val binaryExpressionAndItsUsages =
                findBinaryExpressionAndItsUsageWhenLiteralRefSqlId(literal, toolClassName, myBatisDmlSql)
            if (binaryExpressionAndItsUsages.isNotEmpty()) {
                res.addAll(binaryExpressionAndItsUsages)
            }
            true
        }, scope, myBatisDmlSql.sqlId, UsageSearchContext.IN_STRINGS, true)
        return res
    }

    private fun findBinaryExpressionAndItsUsageWhenLiteralRefSqlId(
        literal: PsiLiteralExpression,
        toolClassName: String,
        sqlId: MyBatisDmlSql
    ): List<PsiMethodCallExpression> {
        val res = mutableListOf<PsiMethodCallExpression>()
        // check if it is a binary expression
        val binaryExpression =
            PsiTreeUtil.getParentOfType(literal, PsiBinaryExpression::class.java) ?: return emptyList()
        val binaryExprEvalRes = JavaConstantExpressionEvaluator.computeConstantExpression(binaryExpression, false)
        if (binaryExprEvalRes != sqlId.toFullName()) {
            return emptyList()
        }
        val sqlFieldUsages = getSqlFieldUsages(binaryExpression, toolClassName)
        val localVariableUsages = getLocalVariableUsages(binaryExpression, toolClassName)
        val directUsages = getDirectUsages(binaryExpression, toolClassName)
        res.addAll(sqlFieldUsages)
        res.addAll(localVariableUsages)
        res.addAll(directUsages)
        return res
    }

    private fun getDirectUsages(expression: PsiExpression, toolClassName: String): List<PsiMethodCallExpression> {
        val methodCall = PsiTreeUtil.getParentOfType(expression, PsiMethodCallExpression::class.java)
        if (methodCall?.resolveMethod()?.containingClass?.qualifiedName == toolClassName) {
            return listOf(methodCall)
        }
        return emptyList()
    }

    private fun getLocalVariableUsages(
        expression: PsiExpression,
        toolClassName: String
    ): List<PsiMethodCallExpression> {
        val declareStatement =
            PsiTreeUtil.getParentOfType(expression, PsiDeclarationStatement::class.java) ?: return emptyList()
        val localVariable =
            PsiTreeUtil.getChildOfType(declareStatement, PsiLocalVariable::class.java) ?: return emptyList()
        return getExpressionRefByToolClass(localVariable, toolClassName)
    }

    private fun getExpressionRefByToolClass(base: PsiElement, toolClassName: String): List<PsiMethodCallExpression> {
        val queryResult = ReferencesSearch.search(base)
        val res = mutableListOf<PsiMethodCallExpression>()
        queryResult.forEach { psiRef ->
            if (psiRef !is PsiReferenceExpression) {
                return@forEach
            }
            val methodCall = PsiTreeUtil.getParentOfType(psiRef, PsiMethodCallExpression::class.java)
            if (methodCall?.resolveMethod()?.containingClass?.qualifiedName == toolClassName) {
                res.add(methodCall)
            }
        }
        return res
    }

    private fun getSqlFieldUsages(expression: PsiExpression, toolClassName: String): List<PsiMethodCallExpression> {
        val sqlIdField = PsiTreeUtil.getParentOfType(expression, PsiField::class.java) ?: return emptyList()
        return getExpressionRefByToolClass(sqlIdField, toolClassName)
    }
}