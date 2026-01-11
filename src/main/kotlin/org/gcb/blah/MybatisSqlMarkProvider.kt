package org.gcb.blah

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag

class MybatisSqlMarkProvider: RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is XmlTag) {
            return
        }
        val tagName = element.name
        if (tagName !in setOf("select", "insert", "delete", "update")) {
            return
        }
        val sqlId = element.getAttributeValue("id") ?: return
        val project = element.project
        val toolClassName = PluginSettingState.getInstance(project).toolClassName
        if (toolClassName.isEmpty()) {
            return
        }
        val targets = findMethod(project, toolClassName, sqlId)
        if (targets.isEmpty()) {
            return
        }
        val iconBuilder = NavigationGutterIconBuilder.create(
            AllIcons.Gutter.ImplementedMethod)
            .setTargets(targets)
        result.add(iconBuilder.createLineMarkerInfo(element))
    }

    fun findMethod(project: Project, targetClassName:String, sqlId: String):List<PsiElement> {
        val res = mutableListOf<PsiElement>()
        val scope =  GlobalSearchScope.allScope(project)
        val psiSearchHelper = PsiSearchHelper.getInstance(project)
        psiSearchHelper.processElementsWithWord({
            psiEl, _ ->
            if (psiEl.parent !is PsiLiteralExpression) {
                return@processElementsWithWord true
            }
            val literal = psiEl.parent as PsiLiteralExpression
            if (literal.value != sqlId) {
                return@processElementsWithWord true
            }
            val expressionList = PsiTreeUtil.getParentOfType(literal, PsiExpressionList::class.java)
                ?: return@processElementsWithWord true
            val expressions = expressionList.expressions
            if (expressions.isEmpty() || expressions[0] != literal) {
                return@processElementsWithWord true
            }
            val methodCall = PsiTreeUtil.getParentOfType(expressionList, PsiMethodCallExpression::class.java)
                ?: return@processElementsWithWord true
            val qn = methodCall.resolveMethod()?.containingClass?.qualifiedName
            if (qn == targetClassName) {
                res.add(methodCall)
            }
            true
        }, scope, sqlId, UsageSearchContext.IN_STRINGS, true)
        return res
    }
}