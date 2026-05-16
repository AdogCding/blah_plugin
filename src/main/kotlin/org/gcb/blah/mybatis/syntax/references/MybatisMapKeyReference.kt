package org.gcb.blah.mybatis.syntax.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import org.gcb.blah.mybatis.common.MybatisSqlUsageUtils
import org.gcb.blah.mybatis.jump.PluginSettingState
import org.gcb.blah.mybatis.syntax.common.MybatisMapKeyUtils

class MybatisMapKeyReference(element: PsiElement, textRange: TextRange, private val targetKey: String):
    PsiPolyVariantReferenceBase<PsiElement>(element, textRange) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (targetKey.isBlank()) {
            return emptyArray()
        }
        val project = element.project
        val dmlTag = PsiTreeUtil.findFirstParent(element) {
            MybatisSqlUsageUtils.isMybatisDmlTag(it)
        }
        if (dmlTag == null || !MybatisSqlUsageUtils.isMybatisDmlTag(dmlTag)) {
            return emptyArray()
        }
        val myBatisDmlSql = MybatisSqlUsageUtils.findSqlIdOfXmlRawSql(dmlTag as XmlTag) ?: return emptyArray()

        /**
         * 找到DBUtils.selectList(SQL_ID, map)表达式
         */
        val methodExpressions = MybatisSqlUsageUtils.findMethodExpression(project, PluginSettingState.getInstance(project).toolClassName, myBatisDmlSql)
        val methodDeclarations = methodExpressions.map {
            MybatisMapKeyUtils.findMethodMapParameterDeclaration(it)
        }
        val result = mutableListOf<PsiElementResolveResult>()
        methodDeclarations.forEach { psiMethods ->
            for (psiMethod in psiMethods) {
                val expression = MybatisMapKeyUtils.findPutUsage(psiMethod, targetKey) ?: continue
                result.add(PsiElementResolveResult(expression))
            }
        }
        return result.toTypedArray()
    }
}