package org.gcb.blah

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiLocalVariable
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
        val sqlId = findSqlId(element as XmlTag) ?: return
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
        return listOf()
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
    fun findMethod(project: Project, toolClassName: String, sqlId: MyBatisDmlSql): List<PsiElement> {
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
        }, scope, sqlId.sqlId, UsageSearchContext.IN_STRINGS, true)
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
        if (binaryExprEvalRes != sqlId.toSqlString()) {
            return emptyList()
        }
        val sqlFieldUsages = getSqlFieldUsages(binaryExpression, toolClassName)
        val localVariableUsages = getLocalVariableUsages(binaryExpression, toolClassName);
        val directUsages = getDirectUsages(binaryExpression, toolClassName);
        res.addAll(sqlFieldUsages)
        res.addAll(localVariableUsages)
        res.addAll(directUsages);
        return res;
    }

    private fun getDirectUsages(binaryExpression: PsiBinaryExpression, toolClassName: String): List<PsiMethodCallExpression> {
        val methodCall = PsiTreeUtil.getParentOfType(binaryExpression, PsiMethodCallExpression::class.java)
        if (methodCall?.resolveMethod()?.containingClass?.qualifiedName == toolClassName) {
            return listOf(methodCall)
        }
        return emptyList()
    }

    private fun getLocalVariableUsages(binaryExpression: PsiBinaryExpression, toolClassName: String): List<PsiMethodCallExpression> {
        val declareStatement = PsiTreeUtil.getParentOfType(binaryExpression, PsiDeclarationStatement::class.java) ?: return emptyList()
        val localVariable = PsiTreeUtil.getChildOfType(declareStatement, PsiLocalVariable::class.java) ?: return emptyList()
        return getBinaryExpressionRefByToolClass(localVariable, toolClassName)
    }

    private fun getBinaryExpressionRefByToolClass(base: PsiElement, toolClassName: String): List<PsiMethodCallExpression> {
        val queryResult = ReferencesSearch.search(base)
        val res = mutableListOf<PsiMethodCallExpression>();
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

    private fun getSqlFieldUsages(binaryExpression: PsiBinaryExpression, toolClassName: String): List<PsiMethodCallExpression> {
        val sqlIdField = PsiTreeUtil.getParentOfType(binaryExpression, PsiField::class.java) ?: return emptyList()
        return getBinaryExpressionRefByToolClass(sqlIdField, toolClassName)
    }
}