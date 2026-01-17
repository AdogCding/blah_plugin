package org.gcb.blah

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.impl.JavaConstantExpressionEvaluator
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.xml.XmlTag

class MybatisSqlMarkProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (!element.isMybatisDmlTag()) {
            return
        }
        val sqlId = findSqlId(element as XmlTag)
        if (sqlId.isNullOrBlank()) {
            return
        }
        val project = element.project
        // 工具类的类名
        val toolClassName = PluginSettingState.getInstance(project).toolClassName
        if (toolClassName.isEmpty()) {
            return
        }
        val targets = findMethod(project, toolClassName, sqlId)
        if (targets.isEmpty()) {
            return
        }
        val iconBuilder = NavigationGutterIconBuilder.create(
            AllIcons.Gutter.ImplementedMethod
        )
            .setTargets(targets)
        result.add(iconBuilder.createLineMarkerInfo(element))
    }

    private fun PsiElement.isMybatisDmlTag(): Boolean {
        if (this !is XmlTag) {
            return false
        }
        val tagName = this.name
        if (tagName !in setOf("select", "insert", "delete", "update")) {
            return false
        }
        return true
    }


    private fun findSqlId(mybatisDmlTag: XmlTag): String? {
        val sqlId = mybatisDmlTag.getAttributeValue("id")
        val ns = mybatisDmlTag.parent?.run { if (this is XmlTag && this.name == "mapper") return this.getAttributeValue("namespace") }
        if (ns == null) {
            return null
        }
        return "${ns}.${sqlId}"
    }

    private fun findLiteralAndItsUsage(
        literal: PsiLiteralExpression,
        toolClassName: String,
        sqlId: String
    ): List<PsiMethodCallExpression> {
        TODO("Not yet implemented")
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
    fun findMethod(project: Project, toolClassName: String, sqlId: String): List<PsiElement> {
        val res = mutableListOf<PsiElement>()
        val scope = GlobalSearchScope.allScope(project)
        val psiSearchHelper = PsiSearchHelper.getInstance(project)
        psiSearchHelper.processElementsWithWord({ psiEl, _ ->
            if (psiEl.parent !is PsiLiteralExpression) {
                return@processElementsWithWord true
            }
            val literal = psiEl.parent as PsiLiteralExpression
            val trivialLiteralAndItsUsages = findLiteralAndItsUsage(literal, toolClassName, sqlId);
            if (trivialLiteralAndItsUsages.isNotEmpty()) {
                res.addAll(trivialLiteralAndItsUsages)
            }
            val binaryExpressionAndItsUsages = findBinaryExpressionAndItsUsageWhenLiteralRefSqlId(literal, toolClassName, sqlId)
            if (binaryExpressionAndItsUsages.isNotEmpty()) {
                res.addAll(binaryExpressionAndItsUsages)
            }
            true
        }, scope, sqlId, UsageSearchContext.IN_STRINGS, true)
        return res
    }

    private fun findBinaryExpressionAndItsUsageWhenLiteralRefSqlId(
        literal: PsiLiteralExpression,
        toolClassName: String,
        sqlId: String
    ): List<PsiMethodCallExpression> {
        // check if it is a binary expression
        val binaryExpression = PsiTreeUtil.getParentOfType(literal, PsiBinaryExpression::class.java)
        if (binaryExpression != null) {
            return emptyList()
        }
        val sqlIdField = PsiTreeUtil.getParentOfType(binaryExpression, PsiField::class.java)
        if (sqlIdField != null) {
            val query = ReferencesSearch.search(sqlIdField)
            val queryRes= query.findAll()
            queryRes.forEach { psiRef ->
                if (psiRef !is PsiReferenceExpression) {
                    return@forEach
                }

            }
        }
        val binaryExprEvalRes = JavaConstantExpressionEvaluator.computeConstantExpression(binaryExpression, false)
        if (binaryExprEvalRes != sqlId) {
            return emptyList()
        }
        return emptyList();
    }

}