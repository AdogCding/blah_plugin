package org.gcb.blah.mybatis.syntax.references

import com.intellij.database.dataSource.connection.statements.results
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
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
        val methods = MybatisSqlUsageUtils.findMethod(project, PluginSettingState.getInstance(project).toolClassName, myBatisDmlSql)
        val methodDeclarations = methods.map {
            MybatisMapKeyUtils.findMethodDeclaration(it)
        }
        val result = mutableListOf<PsiElementResolveResult>()
        methodDeclarations.forEach {
            if (it == null) {
                return@forEach
            }
            val expression = MybatisMapKeyUtils.findPutUsage(it, targetKey) ?: return@forEach
            result.add(PsiElementResolveResult(expression))
        }
        return result.toTypedArray()
    }
}